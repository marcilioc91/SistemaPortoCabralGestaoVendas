# Sistema Porto Cabral — Cantina

Sistema web e desktop para gerenciamento de vendas de cantina, com controle de clientes, produtos, usuários, auditoria e relatórios.

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Frontend | Angular 21.2 (Standalone Components) |
| UI Components | Angular Material 21.2 + Bootstrap 5.3.8 |
| Backend | Java 25 + Spring Boot 4.0.4 |
| ORM | Spring Data JPA + Hibernate |
| Banco de dados | Firebird 5 embarcado (Jaybird 6) + Flyway |
| Build | Maven (backend) · Angular CLI (frontend) |
| Desktop | Electron 41.5 + NSIS (Windows installer) |
| Utilitários | Lombok · ngx-mask · BCryptPasswordEncoder |

---

## Arquitetura

```
Angular (Frontend)
        ↓
REST API (HTTP / JSON)
        ↓
Spring Boot (Backend)
        ↓
JPA / Hibernate
        ↓
Firebird 5 (embarcado, arquivo .fdb)
```

---

## Estrutura do Projeto

```
sistema-porto-cabral/
├── backend/               # Spring Boot (Java)
├── frontend/              # Angular + Electron
├── Annotations/
│   └── novaEstrutura.sql  # Scripts do banco de dados
└── build-release.bat      # Build do app desktop
```

### Backend

```
src/main/java/com/sistemaportocabral/backend/
├── config/
│   └── SecurityConfig.java           # BCrypt + CORS
├── controller/
│   ├── AuditoriaController.java
│   ├── ClienteController.java
│   ├── ProdutoController.java
│   ├── SpaController.java
│   ├── UsuarioController.java
│   └── VendaController.java
├── dto/
│   ├── AtualizarPerfilDTO.java
│   ├── CadastroRequestDTO.java
│   ├── LoginRequestDTO.java
│   ├── PagamentoRequestDTO.java
│   ├── RelatorioInventarioItemDTO.java
│   ├── ResetSenhaDTO.java
│   ├── VendaProdutoRequestDTO.java
│   └── VendaRequestDTO.java
├── entity/
│   ├── AuditoriaLog.java
│   ├── Cliente.java
│   ├── PerfilUsuario.java            # Enum: ADMIN, OPERADOR
│   ├── Pessoa.java
│   ├── Produto.java
│   ├── Usuario.java
│   ├── Venda.java
│   └── VendaItem.java
├── repository/
│   ├── AuditoriaLogRepository.java
│   ├── ClienteRepository.java
│   ├── PessoaRepository.java
│   ├── ProdutoRepository.java
│   ├── UsuarioRepository.java
│   └── VendaRepository.java
├── service/
│   ├── AuditoriaService.java
│   ├── ClienteService.java
│   ├── ProdutoService.java
│   ├── UsuarioService.java
│   └── VendaService.java
├── util/
│   └── ValidacaoUtil.java            # Validação de CPF e email
└── SistemaPortoCabralApplication.java
```

### Frontend

```
src/app/
├── guards/
│   ├── auth.guard.ts                 # loginGuard
│   └── login.guard.ts                # adminGuard
├── models/
│   ├── auditoria.ts
│   ├── cliente.ts
│   ├── produto.ts
│   ├── usuario.ts
│   └── venda.ts
├── pages/
│   ├── auditoria/
│   ├── cadastro-modal/
│   ├── clientes/
│   ├── gerenciar-usuarios/
│   ├── historico-vendas/
│   ├── home/
│   ├── login/
│   ├── produto-form/
│   ├── produtos/
│   ├── relatorio-inventario/
│   └── vendas/
├── services/
│   ├── auth.service.ts
│   ├── auditoria.service.ts
│   ├── cliente.service.ts
│   ├── produto.service.ts
│   └── venda.service.ts
├── footer/
└── utils/
    └── utils.ts
```

---

## Endpoints da API

### Autenticação (`/auth`)

| Método | Rota | Descrição | Perfil |
|---|---|---|---|
| POST | `/auth/login` | Autenticar usuário | Todos |
| POST | `/auth/cadastro` | Cadastrar novo usuário | Todos |
| POST | `/auth/recuperar-senha/solicitar` | Enviar código de recuperação para o e-mail cadastrado (`identificador`: login ou e-mail) | Todos |
| POST | `/auth/recuperar-senha/validar` | Conferir o código sem consumi-lo (`identificador`, `codigo`) | Todos |
| POST | `/auth/recuperar-senha/redefinir` | Redefinir senha com o código (`identificador`, `codigo`, `novaSenha`) | Todos |
| GET | `/auth/usuarios` | Listar usuários | ADMIN |
| PATCH | `/auth/usuarios/{id}/reset-senha` | Redefinir senha | ADMIN |
| PATCH | `/auth/usuarios/{id}/perfil` | Alterar perfil | ADMIN |

### Clientes (`/clientes`)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/clientes` | Listar todos |
| POST | `/clientes` | Criar cliente |
| PUT | `/clientes/{id}` | Atualizar cliente |
| DELETE | `/clientes/{id}` | Excluir cliente |

### Produtos (`/produtos`)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/produtos` | Listar todos (ordenado por nome) |
| POST | `/produtos` | Criar produto |
| PUT | `/produtos/{id}` | Atualizar produto |
| DELETE | `/produtos/{id}` | Excluir produto |

### Vendas (`/vendas`)

| Método | Rota | Descrição | Perfil |
|---|---|---|---|
| POST | `/vendas` | Realizar nova venda | Todos |
| GET | `/vendas` | Listar todas | Todos |
| GET | `/vendas/cliente/{clienteId}` | Listar por cliente | Todos |
| PATCH | `/vendas/{id}/pagamento` | Atualizar pagamento | Todos |
| PUT | `/vendas/{id}/itens` | Editar itens da venda | Todos |
| GET | `/vendas/relatorio/inventario` | Relatório de inventário | ADMIN |

### Auditoria (`/auditoria`)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/auditoria` | Listar logs (filtro opcional por `usuarioId`) |

---

## Banco de Dados

O banco é um **Firebird 5 embarcado**: roda dentro do próprio backend, sem instalação de servidor.
A biblioteca nativa do Firebird vem como dependência Maven (`jaybird-firebird-embedded-win32-x86-64`), então o backend só roda em **Windows x64**.

- **Pasta de dados** (banco `PORTOCABRAL.FDB` + `config.properties`), definida em `PastaDados.java`:
  - **Desenvolvimento:** `backend/dados/`, dentro do projeto (ignorada pelo Git).
  - **Sistema instalado:** `C:\PortoCabral\dados`, fora da pasta do programa, então atualizações e desinstalação não apagam o banco. O instalador cria a pasta e dá permissão de escrita ao grupo Usuários (`frontend/installer/installer.nsh`).
  - Outro local: `--app.dados.dir=C:/outra/pasta` ou variável de ambiente `APP_DADOS_DIR` (só o arquivo do banco: `--app.db.path` / `APP_DB_PATH`).
  - O banco é criado automaticamente na primeira execução (UTF8, collation `UNICODE_CI`), e o caminho em uso aparece no log ao iniciar (`Pasta de dados: ... | Banco de dados: ...`).
- **Estrutura:** versionada com Flyway em `backend/src/main/resources/db/migration`. Toda alteração de tabela deve virar um novo script `V<n>__descricao.sql`; nunca altere um script já aplicado.
- **Acesso exclusivo:** no modo embarcado, só o processo do backend abre o arquivo. Para inspecionar o banco com uma ferramenta externa (ex.: DBeaver, FlameRobin), pare o backend antes.
- **Usuário padrão:** toda instalação já vem com o administrador **`admin` / senha `admin`** (script `V3__usuario_admin_padrao.sql`). No primeiro login o sistema **obriga a troca da senha** antes de liberar o acesso (coluna `USUARIO.TROCAR_SENHA`, endpoint `POST /auth/trocar-senha`). Novos cadastros feitos pela tela de login entram como `OPERADOR`.
- **Backup:** pare o sistema e copie o arquivo `PORTOCABRAL.FDB` da pasta de dados.

### Migração de um banco antigo (SQL Server, versões 1.x)

Copia clientes, produtos, categorias, usuários (com as mesmas senhas), vendas e auditoria para um banco Firebird novo, preservando os IDs.

1. **Na máquina antiga**, gere o backup (SSMS ou `sqlcmd`):
   ```sql
   BACKUP DATABASE PortoCabral TO DISK = 'C:\Temp\PortoCabral.bak' WITH COPY_ONLY, INIT
   ```
2. **Na máquina de desenvolvimento**, restaure com outro nome (veja os nomes lógicos com `RESTORE FILELISTONLY FROM DISK = '...'`):
   ```sql
   RESTORE DATABASE PortoCabral_Origem FROM DISK = 'C:\Temp\PortoCabral.bak'
   WITH MOVE '<nome_logico_dados>' TO 'C:\Temp\PortoCabral_Origem.mdf',
        MOVE '<nome_logico_log>'   TO 'C:\Temp\PortoCabral_Origem_log.ldf'
   ```
3. Compile o backend (`mvnw.cmd package -DskipTests`) e rode **`migrar-mssql.bat PortoCabral_Origem`**. O banco é gerado em `migracao\PORTOCABRAL.FDB` (pasta ignorada pelo Git).
4. Copie o `PORTOCABRAL.FDB` para `C:\PortoCabral\dados\` na máquina de destino, **com o sistema fechado**.

Regras da ferramenta (`MigracaoMssql.java`): o destino precisa estar vazio; tudo roda em uma transação (se falhar, nada é gravado); as contagens são conferidas tabela a tabela; bancos sem `CATEGORIA` (versão 1.0.1) também são aceitos; CPF vazio vira `NULL`. Se nenhum usuário ADMIN existir na origem, é criado o `admin`/`admin` com troca obrigatória.

### Configuração local (fora do Git)

Credenciais e ajustes por máquina ficam em `config.properties`, na pasta de dados (mesma pasta do banco), carregado automaticamente se existir.
Modelo com instruções: [`backend/config.exemplo.properties`](backend/config.exemplo.properties). Outro caminho: variável de ambiente `APP_CONFIG_PATH`.

### Recuperação de senha por e-mail

Na tela de login, **"Esqueci minha senha"** → informar usuário ou e-mail → o sistema envia um **código de 6 dígitos** para o e-mail cadastrado → informar o código (validado antes de seguir) → informar a nova senha.

- Código válido por **15 minutos**, de uso único, guardado como hash BCrypt (tabela `RECUPERACAO_SENHA`).
- Máximo de **5 tentativas** por código, somando a validação e a redefinição; novo envio só após 60 segundos; pedir um novo código invalida o anterior.
- Usuário/e-mail não cadastrado ou usuário sem e-mail: a tela mostra o erro e não avança.
- Pedido e redefinição ficam registrados na auditoria.
- Envio via **Gmail** (`smtp.gmail.com:587`, STARTTLS) com *senha de app*: preencher `spring.mail.username` e `spring.mail.password` no `config.properties`. Sem essas credenciais, a tela informa que o envio não está configurado.
- Requer internet na máquina no momento do envio.

### Diagrama simplificado

```
PESSOA ──────┬── USUARIO
             └── CLIENTE ── VENDA ── VENDA_ITEM ── PRODUTO ── CATEGORIA
                                 └── AUDITORIA_LOG
```

### Tabelas

| Tabela | Campos principais |
|---|---|
| `PESSOA` | id, nome, cpf (unique), telefone |
| `USUARIO` | id, pessoa_id, usuario_login, email, senha (BCrypt), perfil, data_criacao |
| `CLIENTE` | id, pessoa_id, obs |
| `CATEGORIA` | id, nome |
| `PRODUTO` | id, nome, preco, preco_custo, estoque, categoria_id |
| `VENDA` | id, cliente_id, usuario_id, forma_pagamento, valor_pago, data_venda |
| `VENDA_ITEM` | id, venda_id, produto_id, quantidade, preco_unitario |
| `RECUPERACAO_SENHA` | id, usuario_id, codigo_hash, criado_em, expira_em, tentativas, usado |
| `AUDITORIA_LOG` | id, usuario_id, usuario_nome, tipo_operacao, descricao, data_hora |

---

## Perfis de Usuário

| Perfil | Acesso |
|---|---|
| `OPERADOR` | Clientes, produtos, vendas, histórico, auditoria |
| `ADMIN` | Tudo acima + relatório de inventário + gerenciar usuários |

---

## Fluxo de Venda

1. Selecionar cliente (autocomplete)
2. Adicionar produtos ao carrinho
3. Confirmar forma de pagamento e valor pago
4. Backend cria a venda, baixa o estoque e registra a auditoria

### Formas de pagamento suportadas

`DINHEIRO` · `PIX` · `CARTAO_CREDITO` · `CARTAO_DEBITO` · `VOUCHER` · `PENDENTE`

---

## Funcionalidades

- **Autenticação**: login com BCrypt, sessão em sessionStorage, guards de rota
- **Cadastro**: cria automaticamente Pessoa + Usuário + Cliente
- **Clientes**: CRUD, visualização de histórico de compras, exclusão em cascata
- **Produtos**: CRUD, controle de estoque com validação anti-negativo
- **Vendas**: carrinho dinâmico, cálculo de total, confirmação de pagamento
- **Histórico de vendas**: agrupado por cliente, filtros por mês/ano/status, edição de itens e pagamentos
- **Relatório de inventário**: receita, custo e lucro por produto (ADMIN)
- **Auditoria**: log de todas as operações com filtros por usuário e data
- **Gerenciar usuários**: reset de senha e alteração de perfil (ADMIN)
- **App desktop**: instalador NSIS para Windows via Electron

---

## Executar localmente

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

Porta padrão: `8080`

### Frontend

```bash
cd frontend
npm install
npm start
```

Porta padrão: `4200`

### App desktop (Electron)

```bash
cd frontend
npm run electron
```

### Build de distribuição

```bat
build-release.bat
```
