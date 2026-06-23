package com.app.confeitaria.docelivery.dto;

import com.app.confeitaria.docelivery.model.enums.TipoUsuario;// Ajuste o pacote do seu Enum se necessário
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConfeiteiroDTO {

    private Long id;
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    private LocalDate dataNascimento;
    private TipoUsuario tipoUsuario; // Ex: "CONFEITEIRO"
    private Boolean codStatus;

    // Dados de endereço que o seu front-end já consome
    private String cep;
    private String endereco;
    private String bairro;
    private String cidade;
    private String uf;

    // A SUBCLASSE/OBJETO QUE ESTAVA FALTANDO OU VINDO NULL:
    private LojaDTO loja;
}