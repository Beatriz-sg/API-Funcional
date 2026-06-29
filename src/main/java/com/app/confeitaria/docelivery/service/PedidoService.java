package com.app.confeitaria.docelivery.service;

import com.app.confeitaria.docelivery.dto.ItemPedidoDTO;
import com.app.confeitaria.docelivery.dto.PedidoDTO;
import com.app.confeitaria.docelivery.model.entity.ItemPedido;
import com.app.confeitaria.docelivery.model.entity.Pedido;
import com.app.confeitaria.docelivery.model.entity.MovimentacaoFinanceira; // Import novo
import com.app.confeitaria.docelivery.model.enums.StatusPedido;
import com.app.confeitaria.docelivery.model.enums.TipoMovimentacao; // Import novo
import com.app.confeitaria.docelivery.model.enums.CategoriaMovimentacao; // Import novo
import com.app.confeitaria.docelivery.model.repository.PedidoRepository;
import com.app.confeitaria.docelivery.model.repository.ProdutoRepository;
import com.app.confeitaria.docelivery.model.repository.MovimentacaoRepository; // Import novo
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager; // Import adicionado para segurança da FK
import jakarta.persistence.PersistenceContext; // Import adicionado para segurança da FK
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MovimentacaoRepository movimentacaoRepository; // Injetado sem quebrar o resto

    @PersistenceContext
    private EntityManager entityManager; // 🟢 Injetado para resolver dinamicamente o ID de segurança no banco

    // Primeiro método: Atualiza usando o ENUM seguro
    @Transactional // Adicionado para garantir a atomicidade do pedido + financeiro
    public PedidoDTO atualizarStatus(Long pedidoId, StatusPedido novoStatus) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado"));

        pedido.setStatus(novoStatus);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // GATILHO: Se o status virar CONCLUIDO ou ENTREGUE, gera movimentação
        if (novoStatus == StatusPedido.CONCLUIDO || novoStatus == StatusPedido.ENTREGUE) {
            gerarEntradaFinanceiraAutomatica(pedidoSalvo);
        }

        PedidoDTO pedidoDTO = converterParaDTO(pedidoSalvo);

        // Perfeito: Já enviava o DTO limpo
        messagingTemplate.convertAndSend("/topico/pedidos", pedidoDTO);

        return pedidoDTO;
    }

    public PedidoDTO converterParaDTO(Pedido pedido) {
        if (pedido == null) return null;

        String nomeDoCliente    = (pedido.getCliente() != null) ? pedido.getCliente().getNome() : "Cliente não informado";
        String telefoneDoCliente = (pedido.getCliente() != null) ? pedido.getCliente().getTelefone() : "";
        java.math.BigDecimal total = java.math.BigDecimal.valueOf(
                pedido.getValorPedido() != null ? pedido.getValorPedido() : 0.0);
        String statusStr = (pedido.getStatus() != null) ? pedido.getStatus().name() : "NOVO";

        List<ItemPedidoDTO> itensDTO = (pedido.getItens() != null)
                ? pedido.getItens().stream().map(item -> new ItemPedidoDTO(
                        item.getProduto() != null ? item.getProduto().getId() : null,
                        item.getProduto() != null ? item.getProduto().getNome() : "",
                        item.getQuantidade(),
                        java.math.BigDecimal.valueOf(item.getPrecoUnitario() != null ? item.getPrecoUnitario() : 0.0)
                  )).collect(Collectors.toList())
                : java.util.Collections.emptyList();

        return new PedidoDTO(
                pedido.getId(),
                pedido.getNumeroPedido(),
                nomeDoCliente,
                telefoneDoCliente,
                pedido.getEnderecoEntrega() != null ? pedido.getEnderecoEntrega() : "",
                pedido.getFormaPagamento() != null ? pedido.getFormaPagamento() : "",
                statusStr,
                total,
                pedido.getDataHoraPedido(),
                itensDTO
        );
    }

    @Transactional
    public Pedido realizarPedido(Pedido pedido) {
        String codigoUnico = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        pedido.setNumeroPedido("DOCE-" + codigoUnico);
        pedido.setDataHoraPedido(LocalDateTime.now());

        if (pedido.getAgendado() != null && pedido.getAgendado()) {
            pedido.setStatus(StatusPedido.AGENDADO); // Simplificado para usar direto o Enum
            if (pedido.getDataEntregaAgendada() != null &&
                    pedido.getDataEntregaAgendada().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("A data do agendamento não pode ser no passado.");
            }
        } else {
            pedido.setStatus(StatusPedido.NOVO); // Simplificado para usar direto o Enum
        }

        pedido.setCodStatus(true);
        double valorTotalGeral = 0;

        // Variável auxiliar para descobrir a loja através dos produtos enviados
        com.app.confeitaria.docelivery.model.entity.Loja lojaDoPedido = null;
        com.app.confeitaria.docelivery.model.entity.Usuario confeiteiroDoPedido = null;

        if (pedido.getItens() != null && !pedido.getItens().isEmpty()) {
            for (ItemPedido item : pedido.getItens()) {
                item.setPedido(pedido);

                var produtoRef = produtoRepository.findById(item.getProduto().getId())
                        .orElseThrow(() -> new RuntimeException("Produto não encontrado: " + item.getProduto().getId()));

                item.setPrecoUnitario(produtoRef.getPreco());
                double subtotal = item.getQuantidade() * item.getPrecoUnitario();
                item.setPrecoTotal(subtotal);
                valorTotalGeral += subtotal;

                if (produtoRef.getEstoque() < item.getQuantidade()) {
                    throw new RuntimeException("Estoque insuficiente para o produto: " + produtoRef.getNome());
                }

                produtoRef.setEstoque(produtoRef.getEstoque() - item.getQuantidade());

                // Captura o confeiteiro e a loja vinculados ao produto (única fonte confiável)
                if (confeiteiroDoPedido == null && produtoRef.getConfeiteiro() != null) {
                    confeiteiroDoPedido = produtoRef.getConfeiteiro();
                }
                if (lojaDoPedido == null && produtoRef.getLoja() != null) {
                    lojaDoPedido = produtoRef.getLoja();
                }
            }
        }

        // ── RESOLUÇÃO DO CONFEITEIRO ────────────────────────────────────────────────
        // O confeiteiro nunca é enviado pelo front no fluxo normal de compra.
        // Deriva-o de forma confiável a partir do produto, que sempre tem confeiteiro_id preenchido.
        if (pedido.getConfeiteiro() == null && confeiteiroDoPedido != null) {
            // confeiteiroDoPedido é um Usuario; precisamos de um Confeiteiro para o campo tipado
            com.app.confeitaria.docelivery.model.entity.Confeiteiro c =
                    new com.app.confeitaria.docelivery.model.entity.Confeiteiro();
            c.setId(confeiteiroDoPedido.getId());
            pedido.setConfeiteiro(c);
        }

        // ── RESOLUÇÃO DA LOJA ──────────────────────────────────────────────────────
        // O front envia loja.id com o userId do confeiteiro (ex: 10005) em vez do loja.id real.
        // A fonte de verdade é o produto — que tem loja_id correto — ou o confeiteiro (que tem loja).
        // Se o produto não tiver loja vinculada, busca via confeiteiro.
        if (lojaDoPedido == null && confeiteiroDoPedido != null) {
            // Tenta carregar o confeiteiro completo (com loja) para obter o loja_id real
            try {
                Number idLoja = (Number) entityManager
                        .createNativeQuery("SELECT loja_id FROM usuario WHERE id = :id")
                        .setParameter("id", confeiteiroDoPedido.getId())
                        .getSingleResult();
                if (idLoja != null) {
                    com.app.confeitaria.docelivery.model.entity.Loja lojaDoConfeiteiro =
                            new com.app.confeitaria.docelivery.model.entity.Loja();
                    lojaDoConfeiteiro.setId(idLoja.longValue());
                    lojaDoPedido = lojaDoConfeiteiro;
                }
            } catch (Exception ignored) { /* sem loja vinculada ao confeiteiro */ }
        }

        if (lojaDoPedido != null) {
            pedido.setLoja(lojaDoPedido);
        } else {
            // Último recurso: qualquer loja existente no banco
            try {
                Number primeiroIdLoja = (Number) entityManager
                        .createNativeQuery("SELECT TOP 1 id FROM loja")
                        .getSingleResult();
                com.app.confeitaria.docelivery.model.entity.Loja lojaSegura =
                        new com.app.confeitaria.docelivery.model.entity.Loja();
                lojaSegura.setId(primeiroIdLoja.longValue());
                pedido.setLoja(lojaSegura);
            } catch (Exception e) {
                throw new RuntimeException(
                        "Erro impeditivo: nenhuma loja encontrada no banco para vincular ao pedido.");
            }
        }

        pedido.setValorPedido(valorTotalGeral);
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        // --- AJUSTADO: CONVERTE PARA DTO ANTES DE ENVIAR PARA O WEBSOCKET ---
        if (pedidoSalvo.getConfeiteiro() != null) {
            PedidoDTO pedidoDTO = converterParaDTO(pedidoSalvo);
            String destino = "/topico/confeiteiro/" + pedidoSalvo.getConfeiteiro().getId() + "/pedidos";
            messagingTemplate.convertAndSend(destino, pedidoDTO); // Enviando o DTO seguro
        }

        return pedidoSalvo;
    }

    @Transactional(readOnly = true)
    public List<Pedido> buscarFilaConfeiteiro(Long confeiteiroId, List<String> status) {
        List<StatusPedido> statusEnums = status.stream()
                .map(s -> StatusPedido.valueOf(s.toUpperCase()))
                .collect(Collectors.toList());

        return pedidoRepository.findByConfeiteiroIdAndStatusInOrderByDataHoraPedidoAsc(confeiteiroId, statusEnums);
    }

    public Pedido buscarPorId(Long id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado."));
    }

    @Transactional
    public Pedido atualizarStatusViaString(Long id, String novoStatus) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido não encontrado com o ID: " + id));

        String statusMaiusculo = novoStatus.toUpperCase();
        pedido.setStatus(StatusPedido.valueOf(statusMaiusculo));

        if (statusMaiusculo.equals("ENTREGUE") || statusMaiusculo.equals("CANCELADO")) {
            pedido.setCodStatus(false);

            if (statusMaiusculo.equals("CANCELADO") && pedido.getItens() != null) {
                for (ItemPedido item : pedido.getItens()) {
                    var produto = item.getProduto();
                    produto.setEstoque(produto.getEstoque() + item.getQuantidade());
                }
            }
        }

        Pedido pedidoAtualizado = pedidoRepository.save(pedido);

        // GATILHO: Se a string recebida for do fechamento da venda, gera movimentação
        if (statusMaiusculo.equals("CONCLUIDO") || statusMaiusculo.equals("ENTREGUE")) {
            gerarEntradaFinanceiraAutomatica(pedidoAtualizado);
        }

        // --- AJUSTADO: CONVERTE PARA DTO ANTES DE ENVIAR PARA O WEBSOCKET ---
        if (pedidoAtualizado.getConfeiteiro() != null) {
            PedidoDTO pedidoDTO = converterParaDTO(pedidoAtualizado);
            String destino = "/topico/confeiteiro/" + pedidoAtualizado.getConfeiteiro().getId() + "/pedidos";
            messagingTemplate.convertAndSend(destino, pedidoDTO); // Enviando o DTO seguro
        }

        return pedidoAtualizado;
    }

    // MÉTODO PRIVADO ISOLADO: Executa o salvamento financeiro sem misturar na lógica existente
    private void gerarEntradaFinanceiraAutomatica(Pedido pedido) {
        try {
            MovimentacaoFinanceira entrada = new MovimentacaoFinanceira();
            entrada.setDescricao("Venda - Pedido #" + (pedido.getNumeroPedido() != null ? pedido.getNumeroPedido() : pedido.getId()));
            entrada.setValor(java.math.BigDecimal.valueOf(pedido.getValorPedido()));
            entrada.setTipo(TipoMovimentacao.ENTRADA);
            entrada.setCategoria(CategoriaMovimentacao.PEDIDO);
            entrada.setDataLancamento(LocalDateTime.now());
            entrada.setConfeiteiro(pedido.getConfeiteiro());

            movimentacaoRepository.save(entrada);
        } catch (Exception e) {
            System.err.println("Aviso: Falha ao gerar movimentação financeira automática: " + e.getMessage());
        }
    }

    /**
     * CLIENTE: Busca todos os pedidos efetuados por um cliente específico para o histórico.
     * @Transactional(readOnly = true) mantém a sessão Hibernate aberta durante o mapeamento,
     * evitando LazyInitializationException ao acessar Pedido.itens.
     */
    @Transactional(readOnly = true)
    public List<Pedido> buscarPedidosPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    /**
     * CONFEITEIRO: Histórico completo — todos os status, ordenado por data desc.
     */
    @Transactional(readOnly = true)
    public List<Pedido> buscarTodosPedidosConfeiteiro(Long confeiteiroId) {
        return pedidoRepository.findByConfeiteiroIdOrderByDataHoraPedidoDesc(confeiteiroId);
    }
}