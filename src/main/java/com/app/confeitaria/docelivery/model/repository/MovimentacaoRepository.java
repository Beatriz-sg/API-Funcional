package com.app.confeitaria.docelivery.model.repository;



import com.app.confeitaria.docelivery.model.entity.MovimentacaoFinanceira;
import com.app.confeitaria.docelivery.model.enums.TipoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentacaoRepository extends JpaRepository<MovimentacaoFinanceira, Long> {

    // Busca todo o histórico de fluxo de caixa ordenado pelo mais recente
    List<MovimentacaoFinanceira> findByConfeiteiroIdOrderByDataLancamentoDesc(Long confeiteiroId);

    // Soma os valores de um determinado tipo (ENTRADA/SAIDA) num período específico
    @Query("SELECT COALESCE(SUM(m.valor), 0) FROM MovimentacaoFinanceira m " +
            "WHERE m.confeiteiro.id = :confeiteiroId " +
            "AND m.tipo = :tipo " +
            "AND m.dataLancamento >= :dataInicio")
    BigDecimal somarPorTipoEPeriodo(
            @Param("confeiteiroId") Long confeiteiroId,
            @Param("tipo") TipoMovimentacao tipo,
            @Param("dataInicio") LocalDateTime dataInicio
    );
}
