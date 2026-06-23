package com.app.confeitaria.docelivery.auth;

import com.app.confeitaria.docelivery.model.enums.TipoUsuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


// Classe responsavel pelo cadastro do Usuario ( ADMIN, CLIENTE, CONFEITEIRO, ENTREGADOR)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor


public class RegisterRequest {
    private  String nome;
    private  String email;
    private String senha;
    private TipoUsuario tipoUsuario;
}
