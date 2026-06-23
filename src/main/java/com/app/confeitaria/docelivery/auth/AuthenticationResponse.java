package com.app.confeitaria.docelivery.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

// Classe responsavel em gerar a resposta para o cliente, ou seja, retornara o "token"
@Getter
@AllArgsConstructor

public class AuthenticationResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;
}
