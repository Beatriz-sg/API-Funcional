package com.app.confeitaria.docelivery.dto;

import java.math.BigDecimal;

public class FinanceiroResumoDTO {
    private BigDecimal faturamentoBruto;
    private BigDecimal custosOperacionais;
    private BigDecimal lucroLiquido;
    private Long totalPedidos;
    private BigDecimal ticketMedio;

    public FinanceiroResumoDTO(BigDecimal faturamentoBruto, BigDecimal custosOperacionais, Long totalPedidos) {
        this.faturamentoBruto = faturamentoBruto;
        this.custosOperacionais = custosOperacionais;
        this.lucroLiquido = faturamentoBruto.subtract(custosOperacionais);
        this.totalPedidos = totalPedidos;
        this.ticketMedio = totalPedidos > 0
                ? faturamentoBruto.divide(BigDecimal.valueOf(totalPedidos), 2, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    // Getters e Setters
    public BigDecimal getFaturamentoBruto() { return faturamentoBruto; }
    public BigDecimal getCustosOperacionais() { return custosOperacionais; }
    public BigDecimal getLucroLiquido() { return lucroLiquido; }
    public Long getTotalPedidos() { return totalPedidos; }
    public BigDecimal getTicketMedio() { return ticketMedio; }
}