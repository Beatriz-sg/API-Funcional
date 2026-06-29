package com.app.confeitaria.docelivery.dto;

/**
 * DTO recebido pelo Mobile Delivery App no cadastro do entregador.
 * Absorve todos os campos enviados pelo formulario de 4 passos
 * e os mapeia para a entidade Entregador + campos de Usuario.
 */
public record CadastroEntregadorDTO(
        // Passo 1 - Dados Pessoais
        String nome,
        String dataNascimento,   // ISO: YYYY-MM-DD
        String cpf,
        String telefone,

        // Passo 2 - Endereco
        String cep,
        String logradouro,       // mapeado para Usuario.endereco
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String estado,           // mapeado para Usuario.uf

        // Passo 3 - Veiculo e CNH
        String tipoVeiculo,      // mapeado para Entregador.veiculo
        String modeloVeiculo,    // concatenado em Entregador.veiculo
        String placaVeiculo,     // mapeado para Entregador.placaVeiculo
        String numeroCnh,        // mapeado para Entregador.cnh
        String categoriaCnh,     // incluido no campo cnh como sufixo
        String validadeCnh,      // incluido no campo cnh como sufixo

        // Passo 4 - Acesso
        String email,
        String senha
) {}