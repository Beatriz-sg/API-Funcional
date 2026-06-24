package com.app.confeitaria.docelivery.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CLIENTE")
@Getter
@Setter
@NoArgsConstructor
public class Cliente extends Usuario {

    @Column(length = 20)
    private String apelido;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(name = "numero_endereco", length = 20)
    private String numero;

    @Column(length = 100)
    private String complemento;

    // Lista separada por vírgula — simples, sem tabela extra
    @Column(name = "preferencias", length = 500)
    private String preferencias;

    @Column(name = "restricoes", length = 500)
    private String restricoes;
}
