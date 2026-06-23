package com.app.confeitaria.docelivery.dto;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public record UsuarioResponseDTO(
        Long id,
        String nome,
        String email,
        String apelido,
        String tipoUsuario,
        Collection<? extends GrantedAuthority> authorities
) {}