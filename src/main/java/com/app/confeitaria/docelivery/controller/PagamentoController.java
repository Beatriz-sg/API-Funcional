package com.app.confeitaria.docelivery.controller;


import com.app.confeitaria.docelivery.service.MercadoPagoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/pagamentos")
@CrossOrigin(origins = "*") // Permite que seu frontend acesse a rota
public class PagamentoController {

    @Autowired
    private MercadoPagoService mercadoPagoService;

    @PostMapping("/processar")
    public ResponseEntity<?> processar(@RequestBody Map<String, Object> dados) {
        try {
            BigDecimal valor = new BigDecimal(dados.get("valor").toString());
            String tokenCartao = (String) dados.get("tokenCartao");
            String email      = (String) dados.get("email");
            String metodo     = (String) dados.get("metodo"); // "pix" ou "visa", etc.

            // Lê "parcelas" enviado pelo mobile; usa 1 como fallback seguro se ausente
            Integer parcelas = (dados.get("parcelas") != null)
                    ? Integer.parseInt(dados.get("parcelas").toString())
                    : 1;

            String status = mercadoPagoService.criarPagamento(valor, tokenCartao, email, metodo, parcelas);

            // 🔍 DEBUG: se o serviço retornou uma string de erro, expõe a causa real
            // TODO: remover o tratamento de debug e restaurar Map.of("status", status) antes de produção
            if (status != null && status.startsWith("ERRO")) {
                return ResponseEntity
                        .status(500)
                        .body(Map.of(
                                "status",  "ERRO",
                                "detalhe", status          // contém tipo + mensagem da exceção
                        ));
            }

            return ResponseEntity.ok(Map.of("status", status));

        } catch (Exception e) {
            // Cobre erros de parsing do payload (ex: "valor" ausente ou nulo)
            System.err.println("=== ERRO NO CONTROLLER DE PAGAMENTO ===");
            System.err.println("Tipo     : " + e.getClass().getName());
            System.err.println("Mensagem : " + e.getMessage());
            e.printStackTrace(System.err);
            System.err.println("========================================");
            return ResponseEntity
                    .status(500)
                    .body(Map.of(
                            "status",  "ERRO",
                            "detalhe", e.getClass().getSimpleName() + " — " + e.getMessage()
                    ));
        }
    }
}
