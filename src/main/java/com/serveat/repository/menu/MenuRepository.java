package com.serveat.repository.menu;

import com.serveat.domain.menu.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, UUID> {

    // 🔹 Método que ya usabas (admin, otros usos)
    List<Menu> findByActivoTrue();

    // 🔹 NUEVO: para clientes, carga menús + productos (evita LazyInitializationException)
    @Query("""
        SELECT DISTINCT m
        FROM Menu m
        LEFT JOIN FETCH m.productos
        WHERE m.activo = true
    """)
    List<Menu> findMenusActivosConProductos();
}
