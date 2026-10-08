package com.sistemaportocabral.backend.repository;

import com.sistemaportocabral.backend.entity.Pessoa;
import com.sistemaportocabral.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuarioLogin(String usuarioLogin);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByPessoa(Pessoa pessoa);
}
