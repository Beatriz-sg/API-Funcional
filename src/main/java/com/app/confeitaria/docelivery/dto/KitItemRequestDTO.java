package com.app.confeitaria.docelivery.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KitItemRequestDTO {

    @NotNull(message = "O ID do produto é obrigatório para compor o kit.")
    private Long produtoId;

    @NotNull(message = "A quantidade do produto é obrigatória.")
    @Min(value = 1, message = "A quantidade mínima de um item no kit deve ser 1.")
    private Integer quantidade;
}