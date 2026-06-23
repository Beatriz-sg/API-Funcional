package com.app.confeitaria.docelivery.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "kit_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KitItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // O produto pai (o Kit em si)
    @ManyToOne(fetch = FetchType.LAZY) // Evita carregar dados desnecessários
    @JoinColumn(name = "kit_id", nullable = false)
    @JsonBackReference
    private Produto kit;

    // O produto filho (o doce/bolo já existente que entra no kit)
    // 🟢 ADICIONADO: foreignKey explícita com NO_ACTION garante que o banco não faça a deleção em cascata física do produto comum
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
            name = "produto_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_KIT_ITEM_PRODUTO", value = ConstraintMode.CONSTRAINT)
    )
    private Produto produto;

    // Quantidade desse produto específico dentro do kit (ex: 10 brigadeiros)
    @Column(nullable = false)
    private Integer quantidade;
}