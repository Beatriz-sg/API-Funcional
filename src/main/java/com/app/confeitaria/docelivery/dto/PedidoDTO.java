package com.app.confeitaria.docelivery.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PedidoDTO(
        Long id,
        String nomeCliente,
        String telefoneCliente,
        String enderecoEntrega,
        String status, // Ex: "NOVO", "PREPARANDO", "SAIU_PARA_ENTREGA"
        BigDecimal total,
        LocalDateTime dataCriacao,
        List<ItemPedidoDTO> itens
) {}