package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.ItemPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, Long> {

    // Busca todos os itens de um pedido específico
    List<ItemPedido> findByPedidoId(Long pedidoId);

    // Busca todos os itens que contêm um produto específico (Útil para relatórios)
    List<ItemPedido> findByProdutoId(Long produtoId);
}