package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Entregador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntregadorRepository extends JpaRepository<Entregador, Long> {

    // Busca por CNH (importante para conferência de documentos)
    Optional<Entregador> findByCnh(String cnh);

    // Busca pela placa do veículo
    Optional<Entregador> findByPlacaVeiculo(String placaVeiculo);

    // Listar entregadores por tipo de veículo (ex: Moto, Carro, Bicicleta)
    List<Entregador> findByVeiculoIgnoreCase(String veiculo);

    // Buscar entregadores em um bairro específico (campo herdado de Usuario)
    List<Entregador> findByBairroIgnoreCase(String bairro);
}