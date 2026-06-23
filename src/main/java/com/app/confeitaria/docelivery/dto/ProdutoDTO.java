package com.app.confeitaria.docelivery.dto;

import java.math.BigDecimal;

public record ProdutoDTO(
        String nome,
        String descricao,
        Double preco,
        Integer estoque,
        Long categoriaId, // ID para buscar a Categoria no banco
        Boolean disponivel
) {
    // Usar record no Java 17+ simplifica o código (gera getters, construtores e equals automaticamente)
}