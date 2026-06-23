package com.app.confeitaria.docelivery.model.repository;

import com.app.confeitaria.docelivery.model.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // 1. Permite buscar uma categoria pelo nome (descrição) exato
    // Útil para validar se uma categoria já existe antes de cadastrar
    Optional<Categoria> findByDescricao(String descricao);

    // 2. Retorna todas as categorias em ordem alfabética
    // Ótimo para preencher o "Select" (ComboBox) no formulário do React
    List<Categoria> findAllByOrderByDescricaoAsc();

    // 3. Busca categorias que contenham parte de um texto (ignore case)
    List<Categoria> findByDescricaoContainingIgnoreCase(String descricao);
}