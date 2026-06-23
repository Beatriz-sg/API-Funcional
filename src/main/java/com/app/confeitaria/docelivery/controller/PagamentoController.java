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
        BigDecimal valor = new BigDecimal(dados.get("valor").toString());
        String tokenCartao = (String) dados.get("tokenCartao");
        String email = (String) dados.get("email");
        String metodo = (String) dados.get("metodo"); // "pix" ou "visa", etc.

        String status = mercadoPagoService.criarPagamento(valor, tokenCartao, email, metodo);

        return ResponseEntity.ok(Map.of("status", status));
    }
}
