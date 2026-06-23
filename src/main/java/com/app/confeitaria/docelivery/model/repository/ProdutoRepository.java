package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Produto;
import com.app.confeitaria.docelivery.model.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // 1. Busca apenas PRODUTOS NORMAIS (Onde a lista de itens está VAZIA)
    @Query("""
    SELECT DISTINCT p
    FROM Produto p
    LEFT JOIN FETCH p.itens
    LEFT JOIN FETCH p.categoria
    WHERE p.confeiteiro.id = :id
    AND p.itens IS EMPTY
""")
    List<Produto> findProdutosComunsByConfeiteiroId(@Param("id") Long id);

    // 2. Busca apenas KITS (Onde a lista de itens NÃO está vazia)
    @Query("""
    SELECT DISTINCT p
    FROM Produto p
    LEFT JOIN FETCH p.itens
    LEFT JOIN FETCH p.categoria
    WHERE p.confeiteiro.id = :id
    AND p.itens IS NOT EMPTY
""")
    List<Produto> findKitsByConfeiteiroId(@Param("id") Long id);

    // Mantém sua busca de categorias intacta
    @Query("SELECT DISTINCT p.categoria FROM Produto p WHERE p.categoria IS NOT NULL")
    List<Categoria> findDistinctCategorias();
}