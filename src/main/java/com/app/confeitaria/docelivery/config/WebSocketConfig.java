package com.app.confeitaria.docelivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Ponto de conexão que o Frontend vai usar para se conectar
        registry.addEndpoint("/ws-docelivery")
                .setAllowedOriginPatterns("*") // Permite conexões de fora (CORS)
                .withSockJS(); // Libera fallback caso o navegador seja antigo
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Canal por onde o servidor vai enviar as atualizações para o confeiteiro
        registry.enableSimpleBroker("/topico");
        // Prefixo para mensagens que o cliente enviaria (se necessário)
        registry.setApplicationDestinationPrefixes("/app");
    }
}