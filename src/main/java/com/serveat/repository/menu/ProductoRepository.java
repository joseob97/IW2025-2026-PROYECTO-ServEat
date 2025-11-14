package com.serveat.repository.menu;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {
    List<Producto> findByCategorias(Categoria categoria);
    Optional<Producto> findByNombre(String nombre);
    List<Producto> findByDescripcion(String descripcion);
    List<Producto> findByNombreLike(String nombre);
    List<Producto> findByDescripcionLike(String descripcion);
    List<Producto> findByPrecio(BigDecimal precio);
}