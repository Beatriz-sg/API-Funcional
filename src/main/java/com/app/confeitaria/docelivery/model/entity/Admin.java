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
@DiscriminatorValue("ADMIN")
@Getter
@Setter

public class Admin extends Usuario{

    @Column(nullable = true, length = 20)
    private String nivelAcesso;


}


