package com.app.confeitaria.docelivery.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @PostConstruct
    public void init() {
        // Inicializa o SDK do Mercado Pago com o seu token ao ligar o Spring
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    // Método para processar o pagamento
    public String criarPagamento(BigDecimal valor, String tokenCartao, String emailCliente, String metodoPagamento) {
        try {
            PaymentClient client = new PaymentClient();

            PaymentCreateRequest paymentCreateRequest =
                    PaymentCreateRequest.builder()
                            .transactionAmount(valor)
                            .token(tokenCartao) // Se for PIX, esse campo fica nulo ou vazio
                            .description("Pedido de Doces - Docelivery")
                            .paymentMethodId(metodoPagamento) // ex: "visa", "master", "pix"
                            .payer(PaymentPayerRequest.builder().email(emailCliente).build())
                            .build();

            Payment payment = client.create(paymentCreateRequest);

            // Retorna o status do pagamento (APPROVED, PENDING, REJECTED)
            return payment.getStatus();

        } catch (Exception e) {
            System.err.println("Erro ao processar pagamento no Mercado Pago: " + e.getMessage());
            return "ERRO";
        }
    }
}
