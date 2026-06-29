package com.app.confeitaria.docelivery.config;

import org.springframework.web.filter.CorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    // 🟢 Voltamos a injetar o CORS que vem da sua classe CorsConfig antiga
    public SecurityConfig(SecurityFilter securityFilter, CorsConfigurationSource corsConfigurationSource) {
        this.securityFilter = securityFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    // Garante que o filtro do CORS antigo seja injetado com prioridade máxima antes do Security barrar
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(
                new CorsFilter(this.corsConfigurationSource)
        );
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/ws-docelivery/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Mobile Delivery App — rotas publicas
                        .requestMatchers(HttpMethod.POST, "/api/entregadores/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/entregadores").permitAll()
                        // Mobile Delivery App — rotas autenticadas
                        .requestMatchers(HttpMethod.GET, "/api/entregadores/perfil").authenticated()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/imagens/**").permitAll()

                        // 1. ROTAS PÚBLICAS (GET)
                        .requestMatchers(HttpMethod.GET, "/api/stores/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/produtos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/combos/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/confeiteiro/public/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/confeiteiro/profile").permitAll()

                        // 2. GERENCIAMENTO DE PRODUTOS (Batendo com as Roles maiúsculas da sua classe Usuario)
                        .requestMatchers(HttpMethod.POST, "/api/produtos/**").hasAnyAuthority("ROLE_CONFEITEIRO", "ROLE_MASTER")
                        .requestMatchers(HttpMethod.PUT, "/api/produtos/**").hasAnyAuthority("ROLE_CONFEITEIRO", "ROLE_MASTER")
                        .requestMatchers(HttpMethod.DELETE, "/api/produtos/**").hasAnyAuthority("ROLE_CONFEITEIRO", "ROLE_MASTER")

                        // 3. PERFIL DO CONFEITEIRO, LOJA E PEDIDOS
                        .requestMatchers(HttpMethod.PUT, "/api/confeiteiro/loja/atualizar/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/confeiteiro/atualizar/**").authenticated()
                        .requestMatchers("/api/pedidos/confeiteiro/**", "/api/pedidos/confeiteiros/**").hasAnyAuthority("ROLE_CONFEITEIRO", "ROLE_MASTER")
                        .requestMatchers(HttpMethod.GET, "/api/confeiteiro/**").authenticated()

                        // 4. ATUALIZAÇÃO E ENVIO DE PEDIDOS
                        .requestMatchers(HttpMethod.POST, "/api/pedidos", "/api/pedidos/**").hasAnyAuthority("ROLE_CLIENTE", "ROLE_MASTER")
                        .requestMatchers(HttpMethod.PATCH, "/api/pedidos", "/api/pedidos/**").hasAnyAuthority("ROLE_CONFEITEIRO", "ROLE_MASTER")
                        .requestMatchers(HttpMethod.GET, "/api/pedidos", "/api/pedidos/**").authenticated()

                        // 5. LIBERAÇÃO DO MÓDULO FINANCEIRO
                        .requestMatchers("/api/financeiro/**").hasAnyAuthority("ROLE_CONFEITEIRO", "ROLE_MASTER")

                        // 6. ÁREAS ESPECÍFICAS
                        .requestMatchers("/api/cliente/foto").hasAnyAuthority("ROLE_CLIENTE", "ROLE_MASTER")
                        .requestMatchers("/api/cliente/**", "/api/clientes/**").hasAnyAuthority("ROLE_CLIENTE", "ROLE_MASTER")
                        .requestMatchers("/api/entregador/**").hasAnyAuthority("ROLE_ENTREGADOR", "ROLE_MASTER")
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ROLE_MASTER", "ROLE_SUPORTE")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}