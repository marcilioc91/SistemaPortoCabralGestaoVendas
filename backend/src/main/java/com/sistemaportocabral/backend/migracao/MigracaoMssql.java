package com.sistemaportocabral.backend.migracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/**
 * Ferramenta de migração: copia os dados de um banco SQL Server da versão antiga (1.x) para o
 * banco Firebird atual, preservando IDs, senhas (BCrypt) e histórico.
 *
 * Só é ativada quando --app.migracao.origem-url é informado; nesse caso o backend executa a
 * migração e encerra (não sobe o sistema). Ver migrar-mssql.bat na raiz do projeto.
 *
 * Regras:
 * - o banco de destino precisa estar vazio (apenas com o admin padrão criado pelo Flyway);
 * - copia as colunas existentes nos dois lados (bancos antigos sem CATEGORIA também funcionam);
 * - tudo em uma transação: se algo falhar, o destino fica como estava.
 */
@Component
@ConditionalOnProperty(name = "app.migracao.origem-url")
public class MigracaoMssql implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MigracaoMssql.class);

    /** Ordem respeita as chaves estrangeiras */
    private static final List<String> TABELAS = List.of(
            "PESSOA", "CATEGORIA", "PRODUTO", "USUARIO", "CLIENTE", "VENDA", "VENDA_ITEM", "AUDITORIA_LOG");

    private static final int TAMANHO_LOTE = 500;

    @Autowired
    private DataSource destino;

    @Autowired
    private ConfigurableApplicationContext contexto;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${app.migracao.origem-url}")
    private String origemUrl;

    @Value("${app.migracao.origem-usuario:sa}")
    private String origemUsuario;

    @Value("${app.migracao.origem-senha:}")
    private String origemSenha;

    @Override
    public void run(ApplicationArguments args) {
        int codigoSaida = 0;
        try {
            migrar();
        } catch (Exception e) {
            log.error("MIGRACAO FALHOU: {}", e.getMessage(), e);
            codigoSaida = 1;
        }
        int codigo = codigoSaida;
        System.exit(SpringApplication.exit(contexto, () -> codigo));
    }

    private void migrar() throws SQLException {
        log.info("Iniciando migracao SQL Server -> Firebird. Origem: {}", origemUrl);
        Map<String, String> resumo = new LinkedHashMap<>();

        try (Connection origem = DriverManager.getConnection(origemUrl, origemUsuario, origemSenha);
             Connection fb = destino.getConnection()) {

            verificarDestinoVazio(fb);

            fb.setAutoCommit(false);
            try {
                removerDadosIniciais(fb);
                for (String tabela : TABELAS) {
                    resumo.put(tabela, copiarTabela(origem, fb, tabela));
                }
                fb.commit();
            } catch (SQLException | RuntimeException e) {
                fb.rollback();
                throw e;
            }

            // Fora da transação: DDL no Firebird
            fb.setAutoCommit(true);
            reiniciarIdentidades(fb);
            garantirAdministrador(fb);
        }

        log.info("MIGRACAO CONCLUIDA COM SUCESSO");
        resumo.forEach((tabela, r) -> log.info("  {} {}", String.format("%-14s", tabela), r));
    }

    /** Aceita apenas um banco recém-criado: no máximo o admin padrão, ainda com a senha de fábrica */
    private void verificarDestinoVazio(Connection fb) throws SQLException {
        for (String tabela : List.of("CATEGORIA", "PRODUTO", "CLIENTE", "VENDA", "VENDA_ITEM")) {
            if (contar(fb, "SELECT COUNT(*) FROM " + tabela) > 0) {
                throw new IllegalStateException("O banco de destino ja possui dados (tabela " + tabela
                        + "). Use uma pasta de dados vazia (--app.dados.dir) para a migracao.");
            }
        }
        long usuariosNaoPadrao = contar(fb,
                "SELECT COUNT(*) FROM USUARIO WHERE NOT (USUARIO_LOGIN = 'admin' AND TROCAR_SENHA = TRUE)");
        if (usuariosNaoPadrao > 0) {
            throw new IllegalStateException("O banco de destino ja possui usuarios cadastrados. "
                    + "Use uma pasta de dados vazia (--app.dados.dir) para a migracao.");
        }
    }

    /** Remove o admin padrão do Flyway: os usuários (e IDs) passam a ser os do banco antigo */
    private void removerDadosIniciais(Connection fb) throws SQLException {
        try (Statement st = fb.createStatement()) {
            st.executeUpdate("DELETE FROM RECUPERACAO_SENHA");
            st.executeUpdate("DELETE FROM AUDITORIA_LOG");
            st.executeUpdate("DELETE FROM USUARIO");
            st.executeUpdate("DELETE FROM PESSOA");
        }
    }

    private String copiarTabela(Connection origem, Connection fb, String tabela) throws SQLException {
        List<String> colunasOrigem = colunasOrigem(origem, tabela);
        if (colunasOrigem.isEmpty()) {
            log.warn("Tabela {} nao existe na origem (versao antiga): ignorada.", tabela);
            return "ausente na origem";
        }
        Map<String, Integer> tamanhosDestino = colunasDestino(fb, tabela);

        List<String> colunas = new ArrayList<>();
        for (String c : colunasOrigem) {
            if (tamanhosDestino.containsKey(c.toUpperCase())) colunas.add(c);
            else log.warn("Coluna {}.{} nao existe no banco novo: ignorada.", tabela, c);
        }

        String select = "SELECT " + String.join(", ", colunas.stream().map(c -> "[" + c + "]").toList())
                + " FROM [" + tabela + "] ORDER BY [ID]";
        String insert = "INSERT INTO " + tabela + " ("
                + String.join(", ", colunas.stream().map(String::toUpperCase).toList()) + ") VALUES ("
                + String.join(", ", Collections.nCopies(colunas.size(), "?")) + ")";

        int copiadas = 0;
        try (Statement st = origem.createStatement();
             ResultSet rs = st.executeQuery(select);
             PreparedStatement ps = fb.prepareStatement(insert)) {
            while (rs.next()) {
                Object id = rs.getObject("ID");
                for (int i = 0; i < colunas.size(); i++) {
                    String coluna = colunas.get(i).toUpperCase();
                    Object valor = ajustarValor(tabela, coluna, rs.getObject(i + 1));
                    Integer tamanho = tamanhosDestino.get(coluna);
                    if (valor instanceof String s && tamanho != null && tamanho > 0 && s.length() > tamanho) {
                        throw new IllegalStateException(String.format(
                                "%s ID %s: coluna %s tem %d caracteres (maximo %d no banco novo).",
                                tabela, id, coluna, s.length(), tamanho));
                    }
                    ps.setObject(i + 1, valor);
                }
                ps.addBatch();
                if (++copiadas % TAMANHO_LOTE == 0) ps.executeBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new SQLException("Erro ao copiar a tabela " + tabela + ": " + e.getMessage(), e);
        }

        long naOrigem = contar(origem, "SELECT COUNT(*) FROM [" + tabela + "]");
        long noDestino = contar(fb, "SELECT COUNT(*) FROM " + tabela);
        if (naOrigem != noDestino) {
            throw new IllegalStateException(String.format(
                    "Contagem divergente em %s: origem %d, destino %d.", tabela, naOrigem, noDestino));
        }
        return copiadas + " registro(s)";
    }

    private Object ajustarValor(String tabela, String coluna, Object valor) {
        // CPF vazio vira NULL: no banco novo o CPF é UNIQUE (vários NULL são permitidos, vários '' não)
        if (valor instanceof String s && tabela.equals("PESSOA") && coluna.equals("CPF") && s.isBlank()) {
            return null;
        }
        return valor;
    }

    private List<String> colunasOrigem(Connection origem, String tabela) throws SQLException {
        List<String> colunas = new ArrayList<>();
        try (PreparedStatement ps = origem.prepareStatement(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'dbo' AND TABLE_NAME = ? "
                        + "ORDER BY ORDINAL_POSITION")) {
            ps.setString(1, tabela);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) colunas.add(rs.getString(1));
            }
        }
        return colunas;
    }

    /** Nome da coluna (maiúsculo) -> tamanho máximo para texto (0 para os demais tipos) */
    private Map<String, Integer> colunasDestino(Connection fb, String tabela) throws SQLException {
        Map<String, Integer> colunas = new HashMap<>();
        try (ResultSet rs = fb.getMetaData().getColumns(null, null, tabela, null)) {
            while (rs.next()) {
                int tipo = rs.getInt("DATA_TYPE");
                boolean texto = tipo == Types.VARCHAR || tipo == Types.CHAR;
                colunas.put(rs.getString("COLUMN_NAME").toUpperCase(), texto ? rs.getInt("COLUMN_SIZE") : 0);
            }
        }
        if (colunas.isEmpty()) throw new IllegalStateException("Tabela " + tabela + " nao encontrada no banco novo.");
        return colunas;
    }

    /** Os IDs vieram explícitos: o próximo ID gerado precisa continuar depois do maior migrado */
    private void reiniciarIdentidades(Connection fb) throws SQLException {
        List<String> todas = new ArrayList<>(TABELAS);
        todas.add("RECUPERACAO_SENHA");
        try (Statement st = fb.createStatement()) {
            for (String tabela : todas) {
                long maior = contar(fb, "SELECT COALESCE(MAX(ID), 0) FROM " + tabela);
                st.execute("ALTER TABLE " + tabela + " ALTER COLUMN ID RESTART WITH " + (maior + 1));
            }
        }
    }

    /** Se o banco antigo não tinha nenhum ADMIN, recria o admin padrão (admin/admin, troca obrigatória) */
    private void garantirAdministrador(Connection fb) throws SQLException {
        if (contar(fb, "SELECT COUNT(*) FROM USUARIO WHERE PERFIL = 'ADMIN'") > 0) return;
        if (contar(fb, "SELECT COUNT(*) FROM USUARIO WHERE USUARIO_LOGIN = 'admin'") > 0) {
            log.warn("Nenhum ADMIN migrado e o login 'admin' ja existe: defina um ADMIN manualmente.");
            return;
        }
        try (Statement st = fb.createStatement()) {
            st.executeUpdate("INSERT INTO PESSOA (NOME) VALUES ('ADMINISTRADOR')");
        }
        try (PreparedStatement ps = fb.prepareStatement(
                "INSERT INTO USUARIO (PESSOA_ID, USUARIO_LOGIN, EMAIL, SENHA, PERFIL, DATA_CRIACAO, TROCAR_SENHA) "
                        + "SELECT MAX(ID), 'admin', 'admin@portocabral.local', ?, 'ADMIN', CURRENT_TIMESTAMP, TRUE "
                        + "FROM PESSOA WHERE NOME = 'ADMINISTRADOR'")) {
            ps.setString(1, passwordEncoder.encode("admin"));
            ps.executeUpdate();
        }
        log.warn("Nenhum usuario ADMIN no banco antigo: criado o admin padrao (admin/admin, troca obrigatoria).");
    }

    private long contar(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
