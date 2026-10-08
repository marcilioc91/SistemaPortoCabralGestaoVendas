package com.sistemaportocabral.backend.service;

import com.sistemaportocabral.backend.dto.PagamentoRequestDTO;
import com.sistemaportocabral.backend.dto.RelatorioInventarioItemDTO;
import com.sistemaportocabral.backend.dto.VendaRequestDTO;
import com.sistemaportocabral.backend.dto.VendaProdutoRequestDTO;
import com.sistemaportocabral.backend.entity.Cliente;
import com.sistemaportocabral.backend.entity.Produto;
import com.sistemaportocabral.backend.entity.Usuario;
import com.sistemaportocabral.backend.entity.Venda;
import com.sistemaportocabral.backend.entity.VendaItem;
import com.sistemaportocabral.backend.repository.ProdutoRepository;
import com.sistemaportocabral.backend.repository.VendaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

@Service
public class VendaService {

    @Autowired
    private VendaRepository vendaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AuditoriaService auditoriaService;

    @Transactional
    public Venda realizarVenda(VendaRequestDTO dto) {
        Venda venda = new Venda();
        venda.setCliente(entityManager.getReference(Cliente.class, dto.getClienteId()));
        venda.setUsuario(entityManager.getReference(Usuario.class, dto.getUsuarioId()));
        venda.setFormaPagamento(dto.getFormaPagamento());
        venda.setValorPago(dto.getValorPago());

        venda = vendaRepository.save(venda);

        StringBuilder itensDesc = new StringBuilder();
        for (VendaProdutoRequestDTO item : dto.getProdutos()) {
            Produto produto = produtoRepository.findById(item.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + item.getProdutoId()));

            VendaItem vp = new VendaItem();
            vp.setVenda(venda);
            vp.setProduto(produto);
            vp.setQuantidade(item.getQuantidade());
            vp.setPrecoUnitario(produto.getPreco());

            entityManager.persist(vp);

            produto.setEstoque(produto.getEstoque() - item.getQuantidade());
            produtoRepository.save(produto);

            if (itensDesc.length() > 0) itensDesc.append(", ");
            itensDesc.append(produto.getNome()).append(" x").append(item.getQuantidade());
        }

        auditoriaService.registrar(
                dto.getUsuarioId(),
                dto.getUsuarioNome(),
                "VENDA",
                "Venda #" + venda.getId() + " realizada. Itens: " + itensDesc
        );

        return venda;
    }

    public List<Venda> listarTodas() {
        return vendaRepository.findAllComItens();
    }

    public List<Venda> listarPorCliente(Long clienteId) {
        return vendaRepository.findByClienteIdComItens(clienteId);
    }

    @Transactional
    public Venda atualizarPagamento(Long id, PagamentoRequestDTO dto) {
        Venda venda = vendaRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Venda não encontrada: " + id));
        venda.setFormaPagamento(dto.getFormaPagamento());
        venda.setValorPago(dto.getValorPago());
        return vendaRepository.save(venda);
    }

    public List<RelatorioInventarioItemDTO> gerarRelatorioInventario() {
        List<Venda> vendas = vendaRepository.findAllComItens();
        Map<Long, RelatorioInventarioItemDTO> mapa = new LinkedHashMap<>();

        for (Venda v : vendas) {
            if (v.getItens() == null) continue;
            for (VendaItem vi : v.getItens()) {
                Long produtoId = vi.getProduto().getId();
                RelatorioInventarioItemDTO dto = mapa.computeIfAbsent(produtoId,
                        k -> new RelatorioInventarioItemDTO(vi.getProduto().getNome(),
                                vi.getProduto().getCategoria() != null ? vi.getProduto().getCategoria().getNome() : null));

                BigDecimal receita = vi.getPrecoUnitario()
                        .multiply(BigDecimal.valueOf(vi.getQuantidade()));
                BigDecimal custoPorUnidade = vi.getProduto().getPreco_custo() != null
                        ? vi.getProduto().getPreco_custo()
                        : BigDecimal.ZERO;
                BigDecimal custo = custoPorUnidade.multiply(BigDecimal.valueOf(vi.getQuantidade()));

                dto.setQuantidadeVendida(dto.getQuantidadeVendida() + vi.getQuantidade());
                dto.setTotalReceita(dto.getTotalReceita().add(receita));
                dto.setTotalCusto(dto.getTotalCusto().add(custo));
            }
        }

        List<RelatorioInventarioItemDTO> resultado = new ArrayList<>(mapa.values());
        resultado.forEach(dto -> dto.setLucro(dto.getTotalReceita().subtract(dto.getTotalCusto())));
        resultado.sort(Comparator
                .comparing(RelatorioInventarioItemDTO::getNomeCategoria, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(RelatorioInventarioItemDTO::getNomeProduto, String.CASE_INSENSITIVE_ORDER));

        return resultado;
    }

    @Transactional
    public Venda atualizarItens(Long id, VendaRequestDTO dto) {
        Venda venda = vendaRepository.findById(id.intValue())
                .orElseThrow(() -> new RuntimeException("Venda não encontrada: " + id));

        for (VendaItem item : venda.getItens()) {
            Produto p = item.getProduto();
            p.setEstoque(p.getEstoque() + item.getQuantidade());
            produtoRepository.save(p);

            entityManager.remove(item);
        }

        venda.getItens().clear();
        vendaRepository.save(venda);

        for (VendaProdutoRequestDTO itemDto : dto.getProdutos()) {
            Produto produto = produtoRepository.findById(itemDto.getProdutoId())
                    .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

            VendaItem novoItem = new VendaItem();
            novoItem.setVenda(venda);
            novoItem.setProduto(produto);
            novoItem.setQuantidade(itemDto.getQuantidade());
            novoItem.setPrecoUnitario(produto.getPreco());

            entityManager.persist(novoItem);

            produto.setEstoque(produto.getEstoque() - itemDto.getQuantidade());
            produtoRepository.save(produto);
        }

        return vendaRepository.save(venda);
    }
}
