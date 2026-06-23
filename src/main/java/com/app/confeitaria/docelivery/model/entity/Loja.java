package com.app.confeitaria.docelivery.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "loja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Loja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mudado: Agora ela apenas reflete o mapeamento que existe no Confeiteiro
    @OneToOne(mappedBy = "loja", fetch = FetchType.EAGER)
    @JsonIgnoreProperties("loja")
    private Confeiteiro confeiteiro;

    @OneToMany(mappedBy = "loja", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Produto> produtos;

    @Column(name = "nome_fantasia", nullable = false, length = 100)
    private String nomeFantasia;

    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String endereco;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    private String status;

    @Column(name = "data_cadastro")
    private LocalDateTime dataCadastro;

    @Column(name = "foto_url")
    private String fotoUrl;

    @PrePersist
    protected void onCreate() {
        this.dataCadastro = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDENTE";
        }
    }

    // AJUSTADO: Método auxiliar corrigido para refletir a inversão
    public void vincularConfeiteiro(Confeiteiro confeiteiro) {
        this.confeiteiro = confeiteiro;
        if (confeiteiro != null && confeiteiro.getLoja() != this) {
            confeiteiro.setLoja(this);
        }
    }
}