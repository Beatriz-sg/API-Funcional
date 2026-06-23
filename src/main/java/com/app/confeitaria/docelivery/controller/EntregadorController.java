package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.model.entity.Entregador;
import com.app.confeitaria.docelivery.model.repository.EntregadorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entregador")
@CrossOrigin("*")
public class EntregadorController {

    @Autowired
    private EntregadorRepository entregadorRepository;

    @GetMapping("/buscar/{id}")
    public ResponseEntity<?> buscar(@PathVariable Long id) {
        return entregadorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("entregador/atualizar/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Entregador dados) {
        return entregadorRepository.findById(id).map(entregador -> {
            if (dados.getVeiculo() != null) entregador.setVeiculo(dados.getVeiculo());
            // ... demais campos ...
            return ResponseEntity.ok(entregadorRepository.save(entregador));
        }).orElse(ResponseEntity.notFound().build());
    }
}