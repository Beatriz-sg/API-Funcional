package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.model.entity.Confeiteiro;
import com.app.confeitaria.docelivery.model.entity.Produto; // Entidade corrigida
import com.app.confeitaria.docelivery.model.repository.ConfeiteiroRepository;
import com.app.confeitaria.docelivery.model.repository.ProdutoRepository; // Repositório corrigido
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@CrossOrigin("*")
public class LojasController {

    @Autowired
    private ConfeiteiroRepository confeiteiroRepository;

    @Autowired
    private ProdutoRepository produtoRepository; // Nome da variável corrigido

    // GET /api/stores - Retorna todas as lojas (confeiteiros)
    @GetMapping
    public ResponseEntity<List<Confeiteiro>> listarLojas() {
        List<Confeiteiro> lojas = confeiteiroRepository.findAll();
        return ResponseEntity.ok(lojas);
    }

    // GET /api/stores/{id} - Retorna uma loja específica
    @GetMapping("/{id}")
    public ResponseEntity<Confeiteiro> buscarLojaPorId(@PathVariable Long id) {
        return confeiteiroRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // RESOLVE O ERRO 500: Adiciona o endpoint de menu com a nomenclatura em português
    @GetMapping("/{id}/menu")
    public ResponseEntity<List<Produto>> buscarMenuDaLoja(@PathVariable Long id) {
        // Busca os produtos vinculados ao ID do confeiteiro
        List<Produto> menu = produtoRepository.findKitsByConfeiteiroId(id);
        return ResponseEntity.ok(menu);
    }
}