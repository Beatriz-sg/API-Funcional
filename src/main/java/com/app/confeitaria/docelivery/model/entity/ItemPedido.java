package com.app.confeitaria.docelivery.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "itemPedido")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    // 🟢 CORRIGIDO: Alterado para Integer (aceita null se o Jackson precisar)
    @Column(nullable = false)
    private Integer quantidade;

    // 🟢 CORRIGIDO: Alterado para Double
    @Column(nullable = false)
    private Double precoUnitario; // Preço do doce no dia da compra

    // 🟢 CORRIGIDO: Alterado para Double (evita o erro 500 caso o React não envie o total calculado)
    @Column(nullable = false)
    private Double precoTotal;    // Calculado: quantidade * precoUnitario

    // RELACIONAMENTOS

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonIgnore
    private Pedido pedido; // O item precisa saber a qual pedido pertence

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto; // O item precisa saber qual doce foi vendido


}
