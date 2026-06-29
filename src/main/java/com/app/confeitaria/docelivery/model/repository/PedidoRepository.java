package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Pedido;
import com.app.confeitaria.docelivery.model.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Busca direta para o painel do Confeiteiro
    List<Pedido> findByConfeiteiroIdOrderByDataHoraPedidoDesc(Long confeiteiroId);

    // Busca pedidos pendentes ou em preparação para o Confeiteiro (Fila de Trabalho)
    // JOIN FETCH carrega Pedido.itens e o produto de cada item na mesma query,
    // evitando LazyInitializationException ao converter para DTO fora da sessão.
    @Query("SELECT DISTINCT p FROM Pedido p "
         + "LEFT JOIN FETCH p.itens i "
         + "LEFT JOIN FETCH i.produto "
         + "WHERE p.confeiteiro.id = :confeiteiroId "
         + "AND p.status IN :status "
         + "ORDER BY p.dataHoraPedido ASC")
    List<Pedido> findByConfeiteiroIdAndStatusInOrderByDataHoraPedidoAsc(
            @Param("confeiteiroId") Long confeiteiroId,
            @Param("status") List<StatusPedido> status);

    // Busca pedidos de um cliente específico
    List<Pedido> findByClienteIdOrderByDataHoraPedidoDesc(Long clienteId);

    // Busca para a agenda futura (Filtra por agendados e ordena pela data de entrega)
    List<Pedido> findByConfeiteiroIdAndAgendadoTrueOrderByDataEntregaAgendadaAsc(Long confeiteiroId);

    // Busca por número do pedido (Caso precise de uma busca rápida no topo do site)
    Pedido findByNumeroPedido(String numeroPedido);

    // =========================================================================
    // PASSO 2 - ADICIONE ESTE MÉTODO ABAIXO:
    // =========================================================================
    // Ele busca TODOS os pedidos globais que são do tipo AGENDADO e que a data limite já chegou
    List<Pedido> findByStatusAndDataEntregaAgendadaBefore(StatusPedido status, LocalDateTime dataLimite);

    // Busca todos os pedidos com um status especifico (usado pelo endpoint do Entregador)
    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens i LEFT JOIN FETCH i.produto WHERE p.status = :status ORDER BY p.dataHoraPedido DESC")
    List<Pedido> findByStatusWithItens(@Param("status") StatusPedido status);

    // Essa linha precisa estar aqui para o Service funcionar!
    // JOIN FETCH garante que Pedido.itens seja carregado na mesma query,
    // evitando LazyInitializationException ao mapear para DTO fora da sessão.
    @Query("SELECT DISTINCT p FROM Pedido p LEFT JOIN FETCH p.itens i LEFT JOIN FETCH i.produto WHERE p.cliente.id = :clienteId ORDER BY p.dataHoraPedido DESC")
    List<Pedido> findByClienteId(@Param("clienteId") Long clienteId);
}