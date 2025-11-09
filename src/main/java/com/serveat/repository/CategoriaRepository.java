package com.serveat.repository;

import com.serveat.domain.menu.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> { }