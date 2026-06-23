package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.model.entity.Cliente;
import com.app.confeitaria.docelivery.model.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ClienteController {

    @Autowired
    private ClienteRepository clienteRepository;

    @PutMapping("/atualizar/{id}")
    @Transactional
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Cliente dados) {
        // Log de depuração - verifique isso no console do Spring!
        System.out.println("Tentando atualizar ID: " + id);
        System.out.println("Nome recebido do Front: " + dados.getNome());

        return clienteRepository.findById(id).map(clienteExistente -> {

            // 1. Atualiza os campos básicos da classe pai (Usuario)
            if (dados.getNome() != null) clienteExistente.setNome(dados.getNome());
            if (dados.getTelefone() != null) clienteExistente.setTelefone(dados.getTelefone());
            if (dados.getEmail() != null) clienteExistente.setEmail(dados.getEmail());
            if (dados.getCpf() != null) clienteExistente.setCpf(dados.getCpf());
            if (dados.getCep() != null) clienteExistente.setCep(dados.getCep());
            if (dados.getEndereco() != null) clienteExistente.setEndereco(dados.getEndereco());
            if (dados.getBairro() != null) clienteExistente.setBairro(dados.getBairro());
            if (dados.getCidade() != null) clienteExistente.setCidade(dados.getCidade());
            if (dados.getUf() != null) clienteExistente.setUf(dados.getUf());

            // 2. Atualiza campos específicos da classe filha (Cliente)
            if (dados.getApelido() != null) clienteExistente.setApelido(dados.getApelido());

            // 3. Salva efetivamente
            Cliente salvo = clienteRepository.save(clienteExistente);
            System.out.println("Salvo com sucesso no banco: " + salvo.getNome());

            return ResponseEntity.ok(salvo);

        }).orElseGet(() -> {
            System.out.println("Usuario com ID " + id + " não encontrado como CLIENTE.");
            return ResponseEntity.notFound().build();
        });
    }
}