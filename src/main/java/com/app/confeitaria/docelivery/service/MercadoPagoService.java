package com.app.confeitaria.docelivery.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.common.IdentificationRequest;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.resources.payment.Payment;
import com.app.confeitaria.docelivery.model.entity.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.public-key:not-set}")
    private String publicKey;  // leio para logar, não uso no backend

    @PostConstruct
    public void init() {
        // ✅ DIAGNÓSTICO: loga metadados do Access Token SEM expor o segredo completo
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("MERCADO PAGO — Configuração Carregada");
        System.out.println("════════════════════════════════════════════════════════════");
        
        if (accessToken == null || accessToken.isBlank()) {
            System.err.println("❌ ERRO CRÍTICO: Access Token NÃO ENCONTRADO");
            System.err.println("   Verifique se 'mercadopago.access-token' está definido em:");
            System.err.println("   - application.properties");
            System.err.println("   - Variável de ambiente MERCADOPAGO_ACCESS_TOKEN");
        } else {
            // Mostra prefixo + sufixo sem expor o meio (padrão: APP_USR-xxxx...yyyy)
            String prefixo = accessToken.length() > 12 
                ? accessToken.substring(0, 12) + "..." 
                : accessToken;
            String sufixo = accessToken.length() > 6 
                ? "..." + accessToken.substring(accessToken.length() - 6) 
                : "";
            
            System.out.println("Access Token (parcial): " + prefixo + sufixo);
            System.out.println("Tamanho completo      : " + accessToken.length() + " caracteres");
            System.out.println("Fonte                 : application.properties (mercadopago.access-token)");
            
            // Valida se é token de teste (TEST) ou produção (sem TEST no prefixo)
            if (accessToken.contains("TEST")) {
                System.out.println("⚠️  MODO                 : TESTE (credenciais sandbox)");
            } else if (accessToken.startsWith("APP_USR-")) {
                System.out.println("🔴 MODO                 : PRODUÇÃO (credenciais reais)");
            } else {
                System.err.println("⚠️  AVISO: formato inesperado (esperado APP_USR-... ou TEST-...)");
            }
        }
        
        if (publicKey != null && !publicKey.equals("not-set")) {
            String pkPrefixo = publicKey.length() > 12 
                ? publicKey.substring(0, 12) + "..." 
                : publicKey;
            System.out.println("Public Key (parcial)  : " + pkPrefixo + "...");
            System.out.println("Fonte                 : application.properties (mercadopago.public-key)");
        } else {
            System.out.println("Public Key            : NÃO configurada no backend (normal — usada apenas no mobile/web)");
        }
        
        System.out.println("════════════════════════════════════════════════════════════");

        // Inicializa o SDK do Mercado Pago com o seu token ao ligar o Spring
        MercadoPagoConfig.setAccessToken(accessToken);
        System.out.println("✅ SDK do Mercado Pago inicializado com sucesso");

        // ── VERIFICAÇÃO DE CONTA ─────────────────────────────────────────
        // Chama GET /v1/users/me com o Access Token configurado para confirmar
        // que a credencial é válida e pertence à conta esperada.
        System.out.println("════════════════════════════════════════════════════════════");
        System.out.println("MERCADO PAGO — Verificação da Conta Autenticada");
        System.out.println("════════════════════════════════════════════════════════════");
        try {
            com.mercadopago.client.user.UserClient userClient =
                    new com.mercadopago.client.user.UserClient();
            com.mercadopago.resources.user.User user = userClient.get();

            System.out.println("✅ Access Token válido — conta autenticada com sucesso");
            System.out.println("Account ID       : " + user.getId());
            System.out.println("Nickname         : " + user.getNickname());
            System.out.println("Nome             : " + user.getFirstName() + " " + user.getLastName());
            System.out.println("E-mail da conta  : " + user.getEmail());
            System.out.println("Site ID          : " + user.getSiteId()
                    + "  (MLB = Brasil, MLA = Argentina, MLM = México, etc.)");
            System.out.println("Country ID       : " + user.getCountryId());

            // Determina se a conta está em modo TEST ou PRODUCTION pelo Access Token injetado
            if (accessToken != null && accessToken.contains("TEST")) {
                System.out.println("Modo da conta    : ⚠️  TESTE (sandbox) — pagamentos não são reais");
            } else {
                System.out.println("Modo da conta    : 🔴 PRODUÇÃO — pagamentos são reais");
            }

        } catch (com.mercadopago.exceptions.MPApiException apiEx) {
            System.err.println("❌ Falha na autenticação com Mercado Pago");
            System.err.println("   HTTP Status : " + apiEx.getStatusCode());
            com.mercadopago.net.MPResponse resp = apiEx.getApiResponse();
            if (resp != null) {
                System.err.println("   HTTP Status (response) : " + resp.getStatusCode());
                System.err.println("   Corpo da resposta      : " + resp.getContent());
            } else {
                System.err.println("   Corpo da resposta      : (vazio)");
            }
            apiEx.printStackTrace(System.err);
        } catch (Exception ex) {
            System.err.println("❌ Erro inesperado ao verificar conta Mercado Pago: "
                    + ex.getClass().getName() + " — " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        System.out.println("════════════════════════════════════════════════════════════");
    }

    // Método para processar o pagamento
    public String criarPagamento(BigDecimal valor, String tokenCartao, String emailCliente,
                                 String metodoPagamento, Integer parcelas) {
        try {
            PaymentClient client = new PaymentClient();

            // Lê o CPF do cliente autenticado a partir do SecurityContext —
            // o SecurityFilter já carregou a entidade Usuario completa (incluindo cpf) do banco.
            String cpfCliente = null;
            Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getPrincipal()
                    : null;
            if (principal instanceof Usuario) {
                cpfCliente = ((Usuario) principal).getCpf();
            }

            // Monta a identificação do pagador (CPF obrigatório no Brasil em produção)
            IdentificationRequest identification = null;
            if (cpfCliente != null && !cpfCliente.isBlank()) {
                // Remove pontuação caso o CPF esteja armazenado formatado (ex: "123.456.789-09")
                String cpfNumerico = cpfCliente.replaceAll("[^0-9]", "");
                identification = IdentificationRequest.builder()
                        .type("CPF")
                        .number(cpfNumerico)
                        .build();
            }

            // Constrói o PayerRequest mantendo o email original e adicionando a identificação
            // ⚠️ DIAGNÓSTICO TEMPORÁRIO: payer.email fixado em "test_user_br@testuser.com"
            // para isolar se o HTTP 401 é causado pelo email recebido do mobile.
            // TODO: reverter — substituir pela linha abaixo após o teste:
            // .email(emailCliente)

            PaymentPayerRequest payerRequest = PaymentPayerRequest.builder()
                    .email("test_user_br@testuser.com") // TEMP: hardcoded para diagnóstico
                    .identification(identification)
                    .build();

            PaymentCreateRequest paymentCreateRequest =
                    PaymentCreateRequest.builder()
                            .transactionAmount(valor)
                            .token(tokenCartao) // Se for PIX, esse campo fica nulo ou vazio
                            .description("Pedido de Doces - Docelivery")
                            .paymentMethodId(metodoPagamento) // ex: "visa", "master", "pix"
                            .installments(parcelas)           // mapeado de "parcelas" no payload do mobile
                            .payer(payerRequest)
                            .build();

            // 🔍 DEBUG — SERIALIZAÇÃO COMPLETA: captura o JSON exato que o SDK enviará ao Mercado Pago
            // Serializer.serializeToJson() usa o mesmo Gson+@SerializedName que o SDK usa internamente,
            // então o resultado é byte-a-byte idêntico ao payload da requisição HTTP.
            System.out.println("════════════════════════════════════════════════════════════");
            System.out.println("PAYMENT REQUEST — JSON serializado pelo SDK (pré-HTTP)");
            System.out.println("════════════════════════════════════════════════════════════");
            try {
                com.google.gson.JsonObject json =
                        com.mercadopago.serialization.Serializer.serializeToJson(paymentCreateRequest);

                // Mascara o token no JSON antes de imprimir: nunca logar o valor completo
                if (json.has("token") && !json.get("token").isJsonNull()) {
                    String tokenValor = json.get("token").getAsString();
                    String tokenMascarado = tokenValor.substring(0, Math.min(8, tokenValor.length()))
                            + "...[MASKED, len=" + tokenValor.length() + "]";
                    json.addProperty("token", tokenMascarado);
                }

                // Pretty-print para facilitar a leitura no console
                com.google.gson.Gson prettyGson = new com.google.gson.GsonBuilder()
                        .setPrettyPrinting().create();
                System.out.println(prettyGson.toJson(json));

                // Análise de campos críticos diretamente no JsonObject serializado
                System.out.println("════════════════════════════════════════════════════════════");
                System.out.println("ANÁLISE DE CAMPOS CRÍTICOS (nomes JSON reais)");
                System.out.println("════════════════════════════════════════════════════════════");
                String[] camposCriticos = {
                    "transaction_amount", "payment_method_id", "installments",
                    "token", "description", "issuer_id", "capture",
                    "binary_mode", "processing_mode", "notification_url",
                    "external_reference"
                };
                for (String campo : camposCriticos) {
                    String valor2 = json.has(campo)
                            ? (json.get(campo).isJsonNull() ? "null (omitido pelo Gson)" : json.get(campo).toString())
                            : "AUSENTE (não incluído no JSON)";
                    System.out.println(String.format("  %-26s : %s", campo, valor2));
                }
                // Campos do payer
                if (json.has("payer") && json.get("payer").isJsonObject()) {
                    com.google.gson.JsonObject pj = json.getAsJsonObject("payer");
                    System.out.println("  payer.email              : "
                            + (pj.has("email") ? pj.get("email") : "AUSENTE"));
                    System.out.println("  payer.identification     : "
                            + (pj.has("identification") ? pj.get("identification") : "AUSENTE"));
                } else {
                    System.out.println("  payer                    : AUSENTE ← PROBLEMA CRÍTICO");
                }
            } catch (Exception serEx) {
                System.err.println("Falha ao serializar PaymentCreateRequest para log: " + serEx.getMessage());
                // Fallback: loga campos via getters para não perder visibilidade
                System.out.println("transactionAmount  : " + paymentCreateRequest.getTransactionAmount());
                System.out.println("paymentMethodId    : " + paymentCreateRequest.getPaymentMethodId());
                System.out.println("installments       : " + paymentCreateRequest.getInstallments());
                String tokenLog = (paymentCreateRequest.getToken() == null) ? "null"
                        : paymentCreateRequest.getToken().substring(0, Math.min(8, paymentCreateRequest.getToken().length()))
                          + "... [len=" + paymentCreateRequest.getToken().length() + "]";
                System.out.println("token (masked)     : " + tokenLog);
            }
            System.out.println("════════════════════════════════════════════════════════════");

            // 🔍 VALIDAÇÃO DO TOKEN — verifica liveMode, BIN e compatibilidade com as credenciais
            // CardTokenClient.get() chama GET /v1/card_tokens/{id} com o Access Token configurado.
            // Se o token foi gerado em sandbox e o Access Token é de produção (ou vice-versa),
            // liveMode no token será incompatível — essa é a causa mais direta do HTTP 401.
            System.out.println("════════════════════════════════════════════════════════════");
            System.out.println("CARD TOKEN — validação pré-pagamento");
            System.out.println("════════════════════════════════════════════════════════════");
            if (tokenCartao != null && !tokenCartao.isBlank()) {
                try {
                    com.mercadopago.client.cardtoken.CardTokenClient cardTokenClient =
                            new com.mercadopago.client.cardtoken.CardTokenClient();
                    com.mercadopago.resources.CardToken ct = cardTokenClient.get(tokenCartao);

                    System.out.println("token.id              : " + ct.getId());
                    System.out.println("token.status          : " + ct.getStatus());
                    System.out.println("token.liveMode        : " + ct.getLiveMode()
                            + (Boolean.TRUE.equals(ct.getLiveMode())
                                ? " → PRODUÇÃO (token real)"
                                : " → SANDBOX (token de teste) ← verifique se Access Token é de teste"));
                    System.out.println("token.firstSixDigits  : " + ct.getFirstSixDigits());
                    System.out.println("token.lastFourDigits  : " + ct.getLastFourDigits());
                    System.out.println("token.expiration      : "
                            + ct.getExpirationMonth() + "/" + ct.getExpirationYear());
                    System.out.println("token.cardNumberLength: " + ct.getCardNumberLength());
                    System.out.println("token.luhnValidation  : " + ct.getLuhnValidation());
                    System.out.println("token.requireEsc      : " + ct.getRequireEsc());

                    // Verifica liveMode vs Access Token environment
                    boolean tokenIsLive = Boolean.TRUE.equals(ct.getLiveMode());
                    boolean atIsTest    = accessToken != null && accessToken.contains("TEST");
                    if (tokenIsLive && atIsTest) {
                        System.err.println("⚠️  CONFLITO DE AMBIENTE: token é PRODUÇÃO mas Access Token parece ser TESTE");
                    } else if (!tokenIsLive && !atIsTest) {
                        System.err.println("⚠️  CONFLITO DE AMBIENTE: token é SANDBOX mas Access Token é PRODUÇÃO"
                                + " ← causa provável do HTTP 401");
                    } else {
                        System.out.println("✅ Ambiente do token compatível com o Access Token");
                    }

                    // Detecta o payment_method_id real pelo BIN (primeiros 6 dígitos)
                    String bin = ct.getFirstSixDigits();
                    if (bin != null && !bin.isBlank()) {
                        try {
                            com.mercadopago.client.paymentmethod.PaymentMethodClient pmClient =
                                    new com.mercadopago.client.paymentmethod.PaymentMethodClient();
                            com.mercadopago.net.MPResourceList<
                                    com.mercadopago.resources.paymentmethod.PaymentMethod> methods =
                                    pmClient.list();

                            String metodoPorBin  = null;
                            String patternMatch  = null;
                            if (methods != null && methods.getResults() != null) {
                                for (com.mercadopago.resources.paymentmethod.PaymentMethod pm
                                        : methods.getResults()) {
                                    if (pm.getSettings() == null) continue;
                                    for (com.mercadopago.resources.paymentmethod.PaymentMethodSettings s
                                            : pm.getSettings()) {
                                        if (s.getBin() == null || s.getBin().getPattern() == null) continue;
                                        try {
                                            if (bin.matches(s.getBin().getPattern())) {
                                                // Also check exclusion pattern
                                                boolean excluded = s.getBin().getExclusionPattern() != null
                                                        && bin.matches(s.getBin().getExclusionPattern());
                                                if (!excluded) {
                                                    metodoPorBin = pm.getId();
                                                    patternMatch = s.getBin().getPattern();
                                                    break;
                                                }
                                            }
                                        } catch (Exception ignored) { /* padrão regex inválido */ }
                                    }
                                    if (metodoPorBin != null) break;
                                }
                            }

                            System.out.println("────────────────────────────────────────────────────────");
                            System.out.println("BIN detection (firstSixDigits = " + bin + ")");
                            if (metodoPorBin != null) {
                                System.out.println("  Método detectado pelo BIN   : " + metodoPorBin
                                        + "  (pattern: " + patternMatch + ")");
                                System.out.println("  Método enviado na request   : " + metodoPagamento);
                                if (metodoPorBin.equalsIgnoreCase(metodoPagamento)) {
                                    System.out.println("  ✅ payment_method_id COMPATÍVEL com o BIN do token");
                                } else {
                                    System.err.println("  ❌ INCOMPATIBILIDADE: BIN pertence a '"
                                            + metodoPorBin + "' mas request envia '"
                                            + metodoPagamento + "' ← causa provável do HTTP 401");
                                }
                            } else {
                                System.out.println("  Nenhum método identificado para BIN=" + bin
                                        + " (BIN de teste ou não cadastrado nas payment methods)");
                                System.out.println("  Método enviado na request   : " + metodoPagamento);
                            }
                        } catch (Exception pmEx) {
                            System.err.println("Falha ao consultar PaymentMethodClient: " + pmEx.getMessage());
                        }
                    }

                } catch (com.mercadopago.exceptions.MPApiException tokenApiEx) {
                    System.err.println("❌ CardTokenClient.get() falhou — HTTP "
                            + tokenApiEx.getStatusCode());
                    com.mercadopago.net.MPResponse tr = tokenApiEx.getApiResponse();
                    if (tr != null) {
                        System.err.println("   Corpo: " + tr.getContent());
                    }
                    System.err.println("   Possíveis causas: token expirado, token de outro app/ambiente, "
                            + "ou Access Token sem permissão para ler este token");
                    tokenApiEx.printStackTrace(System.err);
                } catch (Exception tokenEx) {
                    System.err.println("Erro inesperado ao validar token: "
                            + tokenEx.getClass().getName() + " — " + tokenEx.getMessage());
                }
            } else {
                System.out.println("token                 : null/vazio — ignorando validação (PIX/boleto)");
            }
            System.out.println("════════════════════════════════════════════════════════════");

            Payment payment = client.create(paymentCreateRequest);

            // Retorna o status do pagamento (APPROVED, PENDING, REJECTED)
            return payment.getStatus();

        } catch (Exception e) {
            // 🔍 DEBUG: loga tipo, mensagem e stack trace completo para diagnóstico
            System.err.println("=== ERRO AO PROCESSAR PAGAMENTO ===");
            System.err.println("Tipo     : " + e.getClass().getName());
            System.err.println("Mensagem : " + e.getMessage());

            // Extrai e loga o corpo completo da resposta quando o SDK lança MPApiException
            if (e instanceof com.mercadopago.exceptions.MPApiException) {
                com.mercadopago.exceptions.MPApiException mpEx = (com.mercadopago.exceptions.MPApiException) e;
                System.err.println("HTTP Status (MPApiException) : " + mpEx.getStatusCode());
                com.mercadopago.net.MPResponse apiResponse = mpEx.getApiResponse();
                if (apiResponse != null) {
                    System.err.println("HTTP Status (MPResponse)     : " + apiResponse.getStatusCode());
                    System.err.println("Corpo da resposta MP         : " + apiResponse.getContent());
                } else {
                    System.err.println("MPResponse                   : nulo (sem corpo de resposta)");
                }
            }

            if (e.getCause() != null) {
                System.err.println("Causa    : " + e.getCause().getClass().getName() + " — " + e.getCause().getMessage());
            }
            e.printStackTrace(System.err);
            System.err.println("====================================");

            // Monta a mensagem de retorno — inclui o corpo da API quando disponível
            // TODO: remover o campo "detalhe" antes de ir para produção
            String detalhe = e.getClass().getSimpleName() + " — " + e.getMessage();
            if (e instanceof com.mercadopago.exceptions.MPApiException) {
                com.mercadopago.net.MPResponse apiResponse = ((com.mercadopago.exceptions.MPApiException) e).getApiResponse();
                if (apiResponse != null && apiResponse.getContent() != null) {
                    detalhe = "HTTP " + ((com.mercadopago.exceptions.MPApiException) e).getStatusCode()
                            + " | " + apiResponse.getContent();
                }
            }
            return "ERRO: " + detalhe;
        }
    }
}
