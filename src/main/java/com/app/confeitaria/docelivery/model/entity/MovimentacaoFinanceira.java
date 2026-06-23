package com.app.confeitaria.docelivery.model.entity;

import com.app.confeitaria.docelivery.model.enums.CategoriaMovimentacao;
import com.app.confeitaria.docelivery.model.enums.TipoMovimentacao;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_financeiras")
public class MovimentacaoFinanceira {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String descricao;

    @Column(nullable = false)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentacao tipo; // ENTRADA ou SAIDA

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaMovimentacao categoria; // PEDIDO, INSUMOS, EMBALAGEM, MARKETING, OUTROS

    @Column(nullable = false)
    private LocalDateTime dataLancamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confeiteiro_id", nullable = false)
    private Confeiteiro confeiteiro;

    // Construtores, Getters e Setters
    public MovimentacaoFinanceira() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public CategoriaMovimentacao getCategoria() { return categoria; }
    public void setCategoria(CategoriaMovimentacao categoria) { this.categoria = categoria; }
    public LocalDateTime getDataLancamento() { return dataLancamento; }
    public void setDataLancamento(LocalDateTime dataLancamento) { this.dataLancamento = dataLancamento; }
    public Confeiteiro getConfeiteiro() { return confeiteiro; }
    public void setConfeiteiro(Confeiteiro confeiteiro) { this.confeiteiro = confeiteiro; }
}