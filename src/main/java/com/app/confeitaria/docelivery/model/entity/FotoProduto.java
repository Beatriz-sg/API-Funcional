package com.app.confeitaria.docelivery.model.entity;

import jakarta.persistence.*; // Verifique se este import está presente
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "foto_produto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FotoProduto {

    // Getters e Setters
    @Id // O ERRO ESTÁ AQUI: Esta anotação deve estar exatamente acima do id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "nome_arquivo")
    private String nomeArquivo;

    @Lob
    @Column(name = "dados_binarios", columnDefinition = "VARBINARY(MAX)")
    private byte[] dadosBinarios;

    @Column(name = "content_type")
    private String contentType;



    public void setId(int id) {
        this.id = id;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public void setDadosBinarios(byte[] dadosBinarios) {
        this.dadosBinarios = dadosBinarios;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}

