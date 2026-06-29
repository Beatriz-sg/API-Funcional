package com.app.confeitaria.docelivery.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produto")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private Double preco;

    @Column(nullable = false)
    private Integer estoque;

    @Column(nullable = false, length = 255)
    private String descricao;

    @Column(name = "imagem_url")
    private String imagemUrl;

    @Column(name = "cod_status")
    @JsonProperty("disponivel")
    private Boolean codStatus = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confeiteiro_id", nullable = false)
    @JsonIgnore // Evita loops e dados desnecessários na listagem
    private Usuario confeiteiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loja_id")
    @JsonIgnore
    private Loja loja;

    // 🟢 CORREÇÃO: Removido o CascadeType.ALL para evitar que o produto comum seja deletado junto com o kit
    // 🟢 MANTIDO: orphanRemoval=true limpa as linhas da tabela 'kit_item' automaticamente ao esvaziar a lista
    @JsonIgnore
    @OneToMany(mappedBy = "kit", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<KitItem> itens = new ArrayList<>();

    @OneToOne(cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "foto_id") // Ou o nome exato da coluna FK na tabela produto, se houver
    private FotoProduto foto;

    /** Indica se o produto/kit está em promoção */
    @Column(name = "em_oferta", nullable = false)
    @Builder.Default
    private Boolean emOferta = false;

    /** Preço promocional — obrigatório quando emOferta = true */
    @Column(name = "preco_promocional", precision = 10, scale = 2)
    private java.math.BigDecimal precoPromocional;


    // 🔴 REMOVIDO: O bloco @ManyToMany antigo (itensDoKit) foi removido
    // para não inflar o banco com tabelas duplicadas desnecessárias.

    public void setCategoriaId(Long id) {
        if (id != null) {
            this.categoria = new Categoria();
            this.categoria.setId(id);
        }
    }
    
    @JsonProperty("confeiteiroId")
    public Long getConfeiteiroId() {
        return confeiteiro != null ? confeiteiro.getId() : null;
    }
}