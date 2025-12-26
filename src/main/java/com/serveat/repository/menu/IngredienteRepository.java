package com.serveat.repository.menu;

import com.serveat.domain.menu.Ingrediente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IngredienteRepository extends JpaRepository<Ingrediente, UUID> {

    Optional<Ingrediente> findByNombre(String nombre);

    List<Ingrediente> findByNombreContainingIgnoreCaseOrderByNombreAsc(String nombre);
}