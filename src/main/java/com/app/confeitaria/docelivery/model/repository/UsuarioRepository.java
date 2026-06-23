package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional; // Importação necessária

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    // O retorno DEVE ser Optional
    Optional<Usuario> findByEmail(String email);
    boolean existsByCpf(String cpf);
}
