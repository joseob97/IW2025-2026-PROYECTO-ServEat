package com.serveat.repository.menu;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductoRepository extends JpaRepository<Producto, UUID> {
    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByCategoria(Categoria categoria);

    @EntityGraph(attributePaths = "categoria")
    List<Producto> findByNombreLike(String nombre);

    Optional<Producto> findByNombre(String nombre);
    List<Producto> findByDescripcion(String descripcion);
    List<Producto> findByDescripcionLike(String descripcion);
    List<Producto> findByPrecio(BigDecimal precio);
    Optional<Producto> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"categoria", "ingredientes", "ingredientes.ingrediente"})
    Optional<Producto> findWithIngredientesByCodigo(String codigo);

    @EntityGraph(attributePaths = {"categoria", "ingredientes", "ingredientes.ingrediente"})
    List<Producto> findAll();

    @Query("""
    select (count(pi) > 0)
    from ProductoIngrediente pi
    where pi.producto.codigo = :codigo
    """)
    boolean productoTieneIngredientes(@Param("codigo") String codigo);

}