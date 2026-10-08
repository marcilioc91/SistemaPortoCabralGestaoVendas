package com.sistemaportocabral.backend.service;

import com.sistemaportocabral.backend.entity.Produto;
import com.sistemaportocabral.backend.repository.CategoriaRepository;
import com.sistemaportocabral.backend.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Sort;

import java.util.List;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository repository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Produto> listar() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
    }

    public Produto salvar(Produto produto, Long usuarioId, String usuarioNome) {
        boolean isNovo = produto.getId() == null;
        if (produto.getCategoria() != null && produto.getCategoria().getId() != null) {
            produto.setCategoria(categoriaRepository.findById(produto.getCategoria().getId()).orElse(null));
        } else {
            produto.setCategoria(null);
        }
        Produto salvo = repository.save(produto);

        String tipo = isNovo ? "INCLUSAO_PRODUTO" : "ALTERACAO_PRODUTO";
        String acao = isNovo ? "incluído" : "alterado";
        auditoriaService.registrar(usuarioId, usuarioNome, tipo,
                "Produto '" + salvo.getNome() + "' (ID " + salvo.getId() + ") " + acao + ".");

        return salvo;
    }

    public void excluir(Long id, Long usuarioId, String usuarioNome) {
        repository.findById(id).ifPresent(p ->
                auditoriaService.registrar(usuarioId, usuarioNome, "EXCLUSAO_PRODUTO",
                        "Produto '" + p.getNome() + "' (ID " + id + ") excluído.")
        );
        repository.deleteById(id);
    }
}
