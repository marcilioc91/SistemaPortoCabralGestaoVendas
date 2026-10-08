package com.sistemaportocabral.backend.repository;

import com.sistemaportocabral.backend.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    boolean existsByCategoriaId(Long categoriaId);
}
