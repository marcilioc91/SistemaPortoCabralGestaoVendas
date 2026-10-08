package com.sistemaportocabral.backend.repository;

import com.sistemaportocabral.backend.entity.RecuperacaoSenha;
import com.sistemaportocabral.backend.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecuperacaoSenhaRepository extends JpaRepository<RecuperacaoSenha, Long> {
    List<RecuperacaoSenha> findByUsuarioAndUsadoFalse(Usuario usuario);
    Optional<RecuperacaoSenha> findFirstByUsuarioAndUsadoFalseOrderByCriadoEmDesc(Usuario usuario);
}
