package com.app.confeitaria.docelivery.model.entity;

import com.app.confeitaria.docelivery.model.enums.TipoPagamento;
import com.app.confeitaria.docelivery.model.enums.TipoUsuario;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(nullable = false)
    private LocalDateTime dataHoraPagamento;
    @Column(nullable = false)
    private double valorPagamento;
    @Column(name = "tipo_pagamento")
    private TipoPagamento tipoPagamento;

    @ManyToOne(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", referencedColumnName = "id",nullable = false)
    private Pedido pedido;
}
