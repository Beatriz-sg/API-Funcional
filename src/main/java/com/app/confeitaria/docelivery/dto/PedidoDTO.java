package com.app.confeitaria.docelivery.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDTO(
        Long id,
        String numeroPedido,
        String nomeCliente,
        String telefoneCliente,
        String enderecoEntrega,
        String formaPagamento,
        String tipoEntrega,
        String status,
        BigDecimal valorPedido,
        LocalDateTime dataCriacao,
        List<ItemPedidoDTO> itens
) {}
