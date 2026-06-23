package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Confeiteiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfeiteiroRepository extends JpaRepository<Confeiteiro, Long> {

    // O JOIN FETCH força o banco a trazer a loja grudada com o confeiteiro na mesma consulta
    @Query("SELECT c FROM Confeiteiro c LEFT JOIN FETCH c.loja WHERE c.id = :id")
    Optional<Confeiteiro> buscarComLojaPorId(@Param("id") Long id);

    // Seus outros métodos continuam iguais abaixo...
    Optional<Confeiteiro> findByEmail(String email);
    Optional<Confeiteiro> findByLoja_Cnpj(String cnpj);

    @Query("SELECT c FROM Confeiteiro c WHERE UPPER(c.cidade) = UPPER(:cidade)")
    List<Confeiteiro> findByCidadeIgnoreCase(@Param("cidade") String cidade);

    Optional<Confeiteiro> findByLoja_NomeFantasiaIgnoreCase(String nomeLoja);
}