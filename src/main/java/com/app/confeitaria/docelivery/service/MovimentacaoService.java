package com.app.confeitaria.docelivery.service;


import com.app.confeitaria.docelivery.dto.FinanceiroResumoDTO;
import com.app.confeitaria.docelivery.model.entity.MovimentacaoFinanceira;
import com.app.confeitaria.docelivery.model.enums.CategoriaMovimentacao;
import com.app.confeitaria.docelivery.model.enums.TipoMovimentacao;
import com.app.confeitaria.docelivery.model.repository.MovimentacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovimentacaoService {

    @Autowired
    private MovimentacaoRepository movimentacaoRepository;

    // 1. REGISTRAR DESPESA MANUAL (Saídas lançadas pelo confeiteiro)
    @Transactional
    public MovimentacaoFinanceira registrarDespesa(MovimentacaoFinanceira despesa) {
        despesa.setTipo(TipoMovimentacao.SAIDA);
        despesa.setDataLancamento(LocalDateTime.now());

        // Garante que a categoria não vá como PEDIDO numa saída manual
        if (despesa.getCategoria() == CategoriaMovimentacao.PEDIDO) {
            despesa.setCategoria(CategoriaMovimentacao.OUTROS);
        }

        return movimentacaoRepository.save(despesa);
    }

    // 2. BUSCAR HISTÓRICO COMPLETO (Para a tabela de Transações Recentes)
    public List<MovimentacaoFinanceira> buscarFluxoCaixa(Long confeiteiroId) {
        return movimentacaoRepository.findByConfeiteiroIdOrderByDataLancamentoDesc(confeiteiroId);
    }

    // 3. GERAR RESUMO CONSOLIDADO (Para os cards de Rendimento, Ticket Médio, etc.)
    public FinanceiroResumoDTO obterResumoFinanceiro(Long confeiteiroId, int meses) {
        LocalDateTime dataInicio = LocalDateTime.now()
                .minusMonths(meses)
                .with(TemporalAdjusters.firstDayOfMonth())
                .with(LocalTime.MIN);

        // Busca somatórios direto do banco usando as queries do Repository
        BigDecimal faturamentoBruto = movimentacaoRepository.somarPorTipoEPeriodo(confeiteiroId, TipoMovimentacao.ENTRADA, dataInicio);
        BigDecimal custosOperacionais = movimentacaoRepository.somarPorTipoEPeriodo(confeiteiroId, TipoMovimentacao.SAIDA, dataInicio);

        // Conta quantos pedidos foram concluídos no período para calcular o ticket médio
        // Nota: Se preferires fazer um COUNT direto no seu PedidoRepository mais tarde, podes ajustar aqui.
        long totalPedidos = movimentacaoRepository.findByConfeiteiroIdOrderByDataLancamentoDesc(confeiteiroId)
                .stream()
                .filter(m -> m.getTipo() == TipoMovimentacao.ENTRADA && m.getDataLancamento().isAfter(dataInicio))
                .count();

        return new FinanceiroResumoDTO(faturamentoBruto, custosOperacionais, totalPedidos);
    }

    // 4. MAPEAR VENDAS DA SEMANA ATUAL (Para o gráfico semanal do React)
    public Map<String, BigDecimal> obterVendasSemanaAtual(Long confeiteiroId) {
        // Encontra a última segunda-feira para iniciar a contagem da semana
        LocalDate hoje = LocalDate.now();
        LocalDateTime inicioSemana = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay();

        List<MovimentacaoFinanceira> movimentacoes = movimentacaoRepository.findByConfeiteiroIdOrderByDataLancamentoDesc(confeiteiroId);

        // Inicializa o mapa com os dias na ordem correta e valores zerados
        Map<String, BigDecimal> vendasDias = new LinkedHashMap<>();
        vendasDias.put("Seg", BigDecimal.ZERO);
        vendasDias.put("Ter", BigDecimal.ZERO);
        vendasDias.put("Qua", BigDecimal.ZERO);
        vendasDias.put("Qui", BigDecimal.ZERO);
        vendasDias.put("Sex", BigDecimal.ZERO);
        vendasDias.put("Sab", BigDecimal.ZERO);
        vendasDias.put("Dom", BigDecimal.ZERO);

        // Mapeia e acumula os valores de ENTRADA de cada dia
        for (MovimentacaoFinanceira mov : movimentacoes) {
            if (mov.getTipo() == TipoMovimentacao.ENTRADA && mov.getDataLancamento().isAfter(inicioSemana)) {
                String diaAbreviado = traduzirDiaSemana(mov.getDataLancamento().getDayOfWeek());
                if (vendasDias.containsKey(diaAbreviado)) {
                    vendasDias.put(diaAbreviado, vendasDias.get(diaAbreviado).add(mov.getValor()));
                }
            }
        }

        return vendasDias;
    }

    // Método auxiliar para bater exatamente com as chaves que o Front-End espera ('Seg', 'Ter', etc.)
    private String traduzirDiaSemana(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "Seg";
            case TUESDAY -> "Ter";
            case WEDNESDAY -> "Qua";
            case THURSDAY -> "Qui";
            case FRIDAY -> "Sex";
            case SATURDAY -> "Sab";
            case SUNDAY -> "Dom";
        };
    }
}