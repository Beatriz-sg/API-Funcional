package com.app.confeitaria.docelivery.service;

import com.app.confeitaria.docelivery.model.entity.Usuario;
import com.auth0.jwt.JWT; // Importação correta
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

// REMOVIDO: import static org.springframework.security.config.Elements.JWT;

@Service
public class TokenService {

    @Value("${api.security.token.secret:secret-key-123}")
    private String secret;

    public String generateToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("docelivery-api")
                    .withSubject(usuario.getEmail())
                    .withExpiresAt(genExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("docelivery-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            // Se o token estiver inválido, retorna vazio para o filtro barrar
            return "";
        }
    }

    private Instant genExpirationDate() {
        // Define expiração para 2 horas no fuso horário de Brasília (-03:00)
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}