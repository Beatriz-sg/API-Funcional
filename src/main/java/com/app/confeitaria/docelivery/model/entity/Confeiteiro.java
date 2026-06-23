package com.app.confeitaria.docelivery.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@DiscriminatorValue("CONFEITEIRO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Confeiteiro extends Usuario {

    // AGORA SIM: O Confeiteiro vira o dono da relação e cria a coluna física 'loja_id'
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "loja_id", referencedColumnName = "id") // Garante o nome padrão e limpo
    @JsonIgnoreProperties("confeiteiro")
    private Loja loja;

    private String proprietario;
    private String categoria;
    private Boolean promocao;
}