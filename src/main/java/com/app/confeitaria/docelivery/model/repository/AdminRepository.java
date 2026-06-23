package com.app.confeitaria.docelivery.model.repository;


import com.app.confeitaria.docelivery.model.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {
    List<Admin> findByNivelAcesso(String nivelAcesso);

    // Alterado para Optional para evitar NullPointerException
    Optional<Admin> findByCpf(String cpf);
}