package com.app.confeitaria.docelivery.model.entity;

import com.app.confeitaria.docelivery.model.enums.StatusPedido;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import com.app.confeitaria.docelivery.model.enums.TipoEntrega;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10, unique = true)
    private String numeroPedido;

    // 🟢 CORRIGIDO: Mudou de 'double' para 'Double' (Aceita null e evita erro 500)
    private Double valorPedido;

    private LocalDateTime dataHoraPedido;

    // 🟢 CORRIGIDO: Mudou de 'boolean' para 'Boolean' (Mais seguro para desserialização de JSON)
    private Boolean codStatus;
    private Boolean agendado;

    private LocalDateTime dataEntregaAgendada;

    @Column(name = "endereco_entrega", length = 500)
    private String enderecoEntrega;

    @Column(name = "forma_pagamento", length = 50)
    private String formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entrega", nullable = false)
    private TipoEntrega tipoEntrega;

    @Column(length = 500)
    private String observacao;

    @Column(length = 50)
    private String cupom;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusPedido status;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens;

    @ManyToOne
    @JoinColumn(name = "loja_id")
    private Loja loja; // Este nome 'loja' deve ser usado no Repository

    @ManyToOne
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({"senha", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled"})
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "confeiteiro_id")
    @JsonIgnoreProperties({"senha", "password", "authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired", "enabled", "loja"})
    private Confeiteiro confeiteiro;

    @ManyToOne
    @JoinColumn(name = "entregador_id")
    private Entregador entregador;
}