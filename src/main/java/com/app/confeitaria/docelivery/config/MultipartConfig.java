package com.app.confeitaria.docelivery.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        // Criamos o elemento de configuração configurando os limites direto no construtor nativo:
        // Parâmetros: (location, maxFileSize, maxRequestSize, fileSizeThreshold)

        long maxFileSize = 20 * 1024 * 1024;    // 20MB em Bytes
        long maxRequestSize = 25 * 1024 * 1024; // 25MB em Bytes

        return new MultipartConfigElement(null, maxFileSize, maxRequestSize, 0);
    }
}