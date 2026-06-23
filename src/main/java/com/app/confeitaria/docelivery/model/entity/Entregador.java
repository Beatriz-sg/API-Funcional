package com.app.confeitaria.docelivery.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("ENTREGADOR")
@Getter
@Setter

public class Entregador extends Usuario{

    @Column(name = "cnh", nullable = true)
    private String cnh;

    @Column(name = "placa_veiculo", nullable = true)
    private String placaVeiculo;

    @Column(name = "veiculo", nullable = true)
    private String veiculo;
}
