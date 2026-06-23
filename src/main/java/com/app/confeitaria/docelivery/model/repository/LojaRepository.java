package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Loja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional; // 👈 Certifique-se de importar o Optional

@Repository
public interface LojaRepository extends JpaRepository<Loja, Long> {

    // Buscar lojas que aguardam aprovação do Admin
    List<Loja> findByStatus(String status);

    // Buscar por nome para filtros na dashboard
    List<Loja> findByNomeFantasiaContainingIgnoreCase(String nome);

    // 🔥 ADICIONE ESTA LINHA AQUI:
    // O Spring Boot vai gerar o SQL automaticamente buscando pela FK confeiteiro_id
    Optional<Loja> findByConfeiteiroId(Long confeiteiroId);
}