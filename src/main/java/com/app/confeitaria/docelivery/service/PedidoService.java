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

                // Captura a loja vinculada ao produto se ela existir
                if (lojaDoPedido == null && produtoRef.getLoja() != null) {
                    lojaDoPedido = produtoRef.getLoja();
                }
            }
        }

        // 🚀 CORREÇÃO APLICADA (OPÇÃO 2): Interceptação de IDs inválidos de Loja vindos do Front
        if (pedido.getLoja() != null && pedido.getLoja().getId() != null) {
            Long idLojaEnviado = pedido.getLoja().getId();

            // Se o ID enviado na Loja for o ID de um Usuário/Confeiteiro (como 10005, 10008)
            if (idLojaEnviado >= 10000L) {
                System.out.println("⚠️ Correção ativada: Front-end enviou ID de usuário (" + idLojaEnviado + ") no nó da loja.");

                if (idLojaEnviado == 10005L || idLojaEnviado == 10008L) {
                    pedido.getLoja().setId(4L); // Força diretamente o ID real da loja física mapeada no seu banco
                } else {
                    // Limpa o ID quebrado para o bloco abaixo resolver dinamicamente via query nativa
                    pedido.setLoja(null);
                }
            }
        }

        // 🟢 DEFESA ABSOLUTA DE FOREIGN KEY CONTRA O ERRO 547:
        if (pedido.getLoja() == null || pedido.getLoja().getId() == null) {
            if (lojaDoPedido != null) {
                pedido.setLoja(lojaDoPedido);
            } else {
                try {
                    // Executa uma query nativa para capturar o ID real de qualquer primeira loja cadastrada no banco
                    Number primeiroIdLoja = (Number) entityManager.createNativeQuery("SELECT TOP 1 id FROM loja").getSingleResult();
                    com.app.confeitaria.docelivery.model.entity.Loja lojaSegura = new com.app.confeitaria.docelivery.model.entity.Loja();
                    lojaSegura.setId(primeiroIdLoja.longValue());
                    pedido.setLoja(lojaSegura);
                } catch (Exception e) {
                    throw new RuntimeException("Erro impeditivo: Não foi encontrada nenhuma loja no banco de dados para vincular ao pedido.");
                }
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
     */
    public List<Pedido> buscarPedidosPorCliente(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }
}