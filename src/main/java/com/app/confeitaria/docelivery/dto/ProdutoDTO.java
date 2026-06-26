package com.app.confeitaria.docelivery.dto;

import java.math.BigDecimal;

public record ProdutoDTO(
        String nome,
        String descricao,
        Double preco,
        Integer estoque,
        Long categoriaId,
        Boolean disponivel,
        Boolean emOferta,
        BigDecimal precoPromocional
) {
    // emOferta e precoPromocional são opcionais — null é aceito para produtos sem promoção
}