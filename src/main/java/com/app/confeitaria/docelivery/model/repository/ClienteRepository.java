package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    // Mantém esse que já funciona
    Optional<Cliente> findByEmail(String email);

    // Esta Query força o Spring a buscar o Cliente comparando diretamente o e-mail
    // com o e-mail do usuário logado (já que eles estão conectados)
    @Query("SELECT c FROM Cliente c WHERE c.email = :email")
    Optional<Cliente> buscarPorEmailDoUsuario(@Param("email") String email);
}