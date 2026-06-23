package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.model.entity.*;
import com.app.confeitaria.docelivery.model.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
public class AdminController {

    @Autowired private AdminRepository adminRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private LojaRepository lojaRepository;
    @Autowired private PedidoRepository pedidoRepository;


    // --- DASHBOARD GERAL ---
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> obterEstatisticas() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsuarios", usuarioRepository.count());
        stats.put("totalLojas", lojaRepository.count());
        stats.put("totalPedidos", pedidoRepository.count());
        return ResponseEntity.ok(stats);
    }

    // --- GESTÃO DE USUÁRIOS ---
    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    // Apenas Master pode deletar permanentemente
    @DeleteMapping("/usuarios/{id}/{adminId}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable Long id, @PathVariable Long adminId) {
        Admin admin = adminRepository.findById(adminId).orElse(null);
        if (admin != null && "MASTER".equalsIgnoreCase(admin.getNivelAcesso())) {
            usuarioRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // --- GESTÃO DE LOJAS (Autonomia para Suporte e Master) ---
    @GetMapping("/lojas")
    public List<Loja> listarLojas() {
        return lojaRepository.findAll();
    }

    @PatchMapping("/lojas/{id}/status")
    public ResponseEntity<Void> alterarStatusLoja(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String novoStatus = body.get("status"); // Ex: "ATIVO", "BLOQUEADO"
        return lojaRepository.findById(id).map(loja -> {
            loja.setStatus(novoStatus);
            lojaRepository.save(loja);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // --- GESTÃO DE PEDIDOS ---
    @GetMapping("/pedidos")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    // --- CONTROLE DE EQUIPE (Apenas MASTER) ---
    @PostMapping("/equipe/{solicitanteId}")
    public ResponseEntity<?> cadastrarNovoAdmin(@RequestBody Admin novoAdmin, @PathVariable Long solicitanteId) {
        Admin solicitante = adminRepository.findById(solicitanteId).orElse(null);

        if (solicitante == null || !"MASTER".equalsIgnoreCase(solicitante.getNivelAcesso())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Apenas administradores MASTER podem criar novos acessos.");
        }

        return new ResponseEntity<>(adminRepository.save(novoAdmin), HttpStatus.CREATED);
    }

    @GetMapping("/equipe")
    public List<Admin> listarEquipe() {
        return adminRepository.findAll();
    }
}