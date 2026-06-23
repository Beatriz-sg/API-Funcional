package com.app.confeitaria.docelivery.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.app.confeitaria.docelivery.model.enums.Permission.*;
@Getter
@AllArgsConstructor
public enum TipoUsuario {

    ADMIN(

            Set.of(
                    ADMIN_READ,
                    ADMIN_CREATE,
                    ADMIN_UPDATE,
                    ADMIN_DELETE,
                    CONFEITEIRO_READ,
                    CONFEITEIRO_CREATE,
                    CONFEITEIRO_UPDATE,
                    CONFEITEIRO_DELETE,
                    ENTREGADOR_READ,
                    ENTREGADOR_CREATE,
                    ENTREGADOR_UPDATE,
                    ENTREGADOR_DELETE
            )
    ),
    CLIENTE(

            Set.of(

                    CLIENTE_READ,
                    CLIENTE_CREATE,
                    CLIENTE_UPDATE,
                    CLIENTE_DELETE
    )
    ),
    CONFEITEIRO(

            Set.of(
                    CONFEITEIRO_READ,
                    CONFEITEIRO_CREATE,
                    CONFEITEIRO_UPDATE,
                    CONFEITEIRO_DELETE
            )
    ),
    ENTREGADOR(

            Set.of(
                    ENTREGADOR_READ,
                    ENTREGADOR_CREATE,
                    ENTREGADOR_UPDATE,
                    ENTREGADOR_DELETE
            )
    );

    private final Set<Permission> permissions;

    public List<SimpleGrantedAuthority> getGrantedAuthorities() {
        var authorities = getPermissions()
                .stream()
                .map(permission-> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    }


}
