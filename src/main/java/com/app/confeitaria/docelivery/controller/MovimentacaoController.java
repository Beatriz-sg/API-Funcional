package com.app.confeitaria.docelivery.controller;


import com.app.confeitaria.docelivery.dto.FinanceiroResumoDTO;
import com.app.confeitaria.docelivery.model.entity.MovimentacaoFinanceira;
import com.app.confeitaria.docelivery.model.repository.ConfeiteiroRepository;
import com.app.confeitaria.docelivery.service.MovimentacaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/financeiro")
@CrossOrigin(origins = "*") // Ajuste de acordo com a sua configuração global de CORS
public class MovimentacaoController {

    @Autowired
    private MovimentacaoService movimentacaoService;

    // 1. BUSCA O RESUMO CONSOLIDADO (Para os cards de rendimento e ticket médio)
    @GetMapping("/resumo/{confeiteiroId}")
    public ResponseEntity<FinanceiroResumoDTO> getResumo(
            @PathVariable Long confeiteiroId,
            @RequestParam(defaultValue = "1") int meses) {

        FinanceiroResumoDTO resumo = movimentacaoService.obterResumoFinanceiro(confeiteiroId, meses);
        return ResponseEntity.ok(resumo);
    }

    // 2. BUSCA O HISTÓRICO DE FLUXO DE CAIXA (Para a tabela de transações)
    @GetMapping("/fluxo-caixa/{confeiteiroId}")
    public ResponseEntity<List<MovimentacaoFinanceira>> getFluxoCaixa(@PathVariable Long confeiteiroId) {
        List<MovimentacaoFinanceira> historico = movimentacaoService.buscarFluxoCaixa(confeiteiroId);
        return ResponseEntity.ok(historico);
    }

    // 3. BUSCA AS VENDAS SEMANAIS FORMATADAS (Para o gráfico de linha semanal)
    @GetMapping("/vendas-semana/{confeiteiroId}")
    public ResponseEntity<List<Map<String, Object>>> getVendasSemana(@PathVariable Long confeiteiroId) {
        Map<String, BigDecimal> vendasSemanaMap = movimentacaoService.obterVendasSemanaAtual(confeiteiroId);

        // Converte o Map do Java para o formato de Lista de Objetos que o Chart.js adora: [{ "day": "Seg", "total": 150.00 }]
        List<Map<String, Object>> graficoFormatado = new ArrayList<>();
        vendasSemanaMap.forEach((dia, total) -> {
            graficoFormatado.add(Map.of(
                    "day", dia,
                    "total", total
            ));
        });

        return ResponseEntity.ok(graficoFormatado);
    }

    // 4. REGISTRA UMA DESPESA MANUAL (Saídas de insumos, embalagens, marketing)
    // 1. Adicione a injeção do repositório do seu confeiteiro no topo do seu MovimentacaoController
    @Autowired
    private com.app.confeitaria.docelivery.model.repository.ConfeiteiroRepository confeiteiroRepository;
// Nota: Ajuste o nome acima caso seu repositório se chame "UsuarioRepository" ou algo do tipo.

    // 2. Substitua o método antigo por este:
    @PostMapping("/despesa/{confeiteiroId}")
    public ResponseEntity<?> registrarDespesa(
            @PathVariable Long confeiteiroId,
            @RequestBody MovimentacaoFinanceira despesa) {

        // 🟢 BUSCA CORRETA: Procura o confeiteiro real no banco de dados pelo ID da URL
        return confeiteiroRepository.findById(confeiteiroId)
                .map(confeiteiroExistente -> {
                    // Vincula o confeiteiro encontrado à despesa recebida no corpo da requisição
                    despesa.setConfeiteiro(confeiteiroExistente);

                    // Salva a despesa através do service
                    MovimentacaoFinanceira novaDespesa = movimentacaoService.registrarDespesa(despesa);
                    return ResponseEntity.status(HttpStatus.CREATED).body(novaDespesa);
                })
                // Caso o ID enviado na URL não exista no banco, retorna um erro 404 amigável
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}