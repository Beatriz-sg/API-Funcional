package com.app.confeitaria.docelivery.dto;

import java.math.BigDecimal;

public record ItemPedidoDTO(
        Long produtoId,
        String nomeProduto,
        Integer quantidade,
        BigDecimal precoUnitario
) {}