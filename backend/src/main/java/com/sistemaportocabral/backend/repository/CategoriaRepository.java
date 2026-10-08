package com.sistemaportocabral.backend.repository;

import com.sistemaportocabral.backend.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
    boolean existsByNomeIgnoreCase(String nome);
}
