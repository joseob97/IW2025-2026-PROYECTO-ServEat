package com.serveat.repository.menu;

import com.serveat.domain.menu.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoriaRepository extends JpaRepository<Categoria, UUID> {
    Optional<Categoria> findByNombre(String nombre);
}