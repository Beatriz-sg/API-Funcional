package com.app.confeitaria.docelivery.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("CLIENTE")
@Getter
@Setter
@NoArgsConstructor // ⬅️ Essencial para o Jackson conseguir dar um "new Cliente()"
public class Cliente extends Usuario {

    @Column(length = 20)
    private String apelido;

    // Se você não usa @SuperBuilder, o Jackson precisa que os campos de Usuário
    // mapeados na Single Table sejam preenchidos via setters padrão, o que o @NoArgsConstructor já permite!
}