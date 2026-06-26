package com.app.confeitaria.docelivery.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class KitRequestDTO {
    private String nome;
    private String descricao;
    private Double preco;
    private Integer estoque;
    private Long confeiteiroId;
    private Long categoriaId;
    private Boolean emOferta;
    private BigDecimal precoPromocional;

    // 🟢 Mudamos de List<Long> para List<KitItemRequestDTO>
    // Agora o React consegue enviar o ID do produto E a quantidade dele juntamente!
    private List<KitItemRequestDTO> itens;
}