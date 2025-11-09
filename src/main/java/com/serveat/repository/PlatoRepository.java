package com.serveat.repository;

import com.serveat.domain.menu.Plato;
import com.serveat.domain.menu.Categoria;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PlatoRepository extends JpaRepository<Plato, UUID> {

    List<Plato> findByCategoriaOrderByNombreAsc(Categoria categoria);
    List<Plato> findByNombreContainingIgnoreCaseOrderByNombreAsc(String q);
    @EntityGraph(attributePaths = "categoria")
    List<Plato> findAll();
}