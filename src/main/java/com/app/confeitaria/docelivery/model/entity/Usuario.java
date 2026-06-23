package com.app.confeitaria.docelivery.model.entity;

import com.app.confeitaria.docelivery.model.enums.TipoUsuario;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "usuario")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_usuario", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;
    private String cpf;
    private String cep;
    private String endereco; // ⚠️ Nota: Este deve ser o endereço RESIDENCIAL do usuário
    private String bairro;
    private String cidade;
    private String uf;
    private String telefone; // ⚠️ Nota: Este deve ser o celular PESSOAL do usuário

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    private LocalDate dataNascimento;

    // 🔴 REMOVIDO: O campo 'private Loja loja' saiu daqui.
    // Clientes e outros usuários não possuem loja.

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_usuario", insertable = false, updatable = false)
    private TipoUsuario tipoUsuario;

    @Column(name = "cod_status")
    private Boolean codStatus = true;

    @PrePersist
    protected void onCreate() {
        if (this.codStatus == null) {
            this.codStatus = true;
        }
    }

    @Override public String getPassword() { return this.senha; }
    @Override public String getUsername() { return this.email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return Boolean.TRUE.equals(this.codStatus); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = (this.tipoUsuario != null) ? this.tipoUsuario.name() : "CLIENTE";
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleName));
    }
}