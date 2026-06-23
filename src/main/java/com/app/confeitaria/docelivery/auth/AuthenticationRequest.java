package com.app.confeitaria.docelivery.auth;

// Classe responsavel para efetuar o login de usuario

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor

public class AuthenticationRequest {


    private String email;
    private String senha;
}
