package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Pedido;
import com.app.confeitaria.docelivery.model.enums.StatusPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Busca direta para o painel do Confeiteiro
    List<Pedido> findByConfeiteiroIdOrderByDataHoraPedidoDesc(Long confeiteiroId);

    // Busca pedidos pendentes ou em preparação para o Confeiteiro (Fila de Trabalho)
    List<Pedido> findByConfeiteiroIdAndStatusInOrderByDataHoraPedidoAsc(Long confeiteiroId, List<StatusPedido> status);

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

    // Essa linha precisa estar aqui para o Service funcionar!
    List<Pedido> findByClienteId(Long clienteId);
}