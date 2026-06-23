package com.app.confeitaria.docelivery.model.entity;

import com.app.confeitaria.docelivery.model.enums.StatusPedido;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

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
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "confeiteiro_id")
    private Confeiteiro confeiteiro;

    @ManyToOne
    @JoinColumn(name = "entregador_id")
    private Entregador entregador;
}