package com.sistemaportocabral.backend.service;

import com.sistemaportocabral.backend.dto.CadastroRequestDTO;
import com.sistemaportocabral.backend.entity.Cliente;
import com.sistemaportocabral.backend.entity.Pessoa;
import com.sistemaportocabral.backend.entity.PerfilUsuario;
import com.sistemaportocabral.backend.entity.Usuario;
import java.util.List;
import com.sistemaportocabral.backend.repository.ClienteRepository;
import com.sistemaportocabral.backend.repository.PessoaRepository;
import com.sistemaportocabral.backend.repository.UsuarioRepository;
import com.sistemaportocabral.backend.util.ValidacaoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    @Autowired
    private UsuarioRepository repository;

    @Autowired
    private PessoaRepository pessoaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public void resetSenha(Long id, String novaSenha) {
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("A nova senha não pode ser vazia.");
        }
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        repository.save(usuario);
    }

    /** Troca de senha pelo próprio usuário (exige a senha atual); usada na troca obrigatória do primeiro acesso */
    public Usuario trocarSenha(String login, String senhaAtual, String novaSenha) {
        Usuario usuario = autenticar(login, senhaAtual);
        if (usuario == null) {
            throw new IllegalArgumentException("Senha atual incorreta.");
        }
        if (novaSenha == null || novaSenha.isBlank()) {
            throw new IllegalArgumentException("A nova senha não pode ser vazia.");
        }
        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setTrocarSenha(false);
        Usuario salvo = repository.save(usuario);

        String nome = usuario.getPessoa() != null ? usuario.getPessoa().getNome() : usuario.getUsuarioLogin();
        auditoriaService.registrar(usuario.getId(), nome, "TROCA_SENHA",
                "Usuário '" + usuario.getUsuarioLogin() + "' trocou a própria senha.");
        return salvo;
    }

    public Usuario atualizarPerfil(Long id, PerfilUsuario perfil) {
        Usuario usuario = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        usuario.setPerfil(perfil);
        return repository.save(usuario);
    }

    public Usuario autenticar(String login, String senha) {
        return repository.findByUsuarioLogin(login)
                .filter(u -> passwordEncoder.matches(senha, u.getSenha()))
                .orElse(null);
    }

    @Transactional
    public Usuario cadastrar(CadastroRequestDTO dto) {
        String cpf = dto.getCpf();
        String cpfLimpo = null;
        if (cpf != null && !cpf.isBlank()) {
            cpfLimpo = cpf.replaceAll("[^0-9]", "");
            if (!ValidacaoUtil.cpfValido(cpfLimpo)) {
                throw new IllegalArgumentException("CPF inválido.");
            }
            if (pessoaRepository.existsByCpf(cpfLimpo)) {
                throw new IllegalArgumentException("CPF já cadastrado.");
            }
        }

        if (!ValidacaoUtil.emailValido(dto.getEmail())) {
            throw new IllegalArgumentException("E-mail inválido.");
        }

        Pessoa pessoa = new Pessoa();
        pessoa.setNome(dto.getNome());
        pessoa.setCpf(cpfLimpo);
        pessoa.setTelefone(dto.getTelefone());
        pessoa = pessoaRepository.save(pessoa);

        Usuario usuario = new Usuario();
        usuario.setPessoa(pessoa);
        usuario.setUsuarioLogin(dto.getUsuario());
        usuario.setEmail(dto.getEmail());
        usuario.setSenha(passwordEncoder.encode(dto.getSenha()));
        usuario.setPerfil(dto.getPerfil() != null ? dto.getPerfil() : PerfilUsuario.OPERADOR);
        usuario = repository.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setPessoa(pessoa);
        cliente.setObs(dto.getObs());
        clienteRepository.save(cliente);

        return usuario;
    }
}
