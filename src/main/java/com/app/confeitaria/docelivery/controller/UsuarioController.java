package com.app.confeitaria.docelivery.controller;

import com.app.confeitaria.docelivery.model.entity.Usuario;
import com.app.confeitaria.docelivery.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuario")
@CrossOrigin("*") // Permite que o seu Frontend (React) acesse a API
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Usuario loginData) {
        return usuarioService.realizarLogin(loginData.getEmail(), loginData.getSenha())
                .map(usuario -> ResponseEntity.ok(usuario))
                .orElse(ResponseEntity.status(401).build());
    }
}