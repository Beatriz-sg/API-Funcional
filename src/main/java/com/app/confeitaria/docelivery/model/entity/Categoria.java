package com.app.confeitaria.docelivery.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "categoria")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = true)
    private LocalDateTime dataHoraAgendamento;

    @Column(nullable = false, length = 100)
    private String descricao; // O campo correto é este!

    public Categoria(Long id) {
        this.id = id;
    }
}