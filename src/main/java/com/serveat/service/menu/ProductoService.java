package com.serveat.service.menu;

import com.serveat.domain.menu.Producto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface ProductoService {

    Producto crearProducto(String nombre, String descripcion, BigDecimal precio, String categoriaNombre);

    Producto crearProductoConIngredientes(
            String nombre,
            String descripcion,
            BigDecimal precio,
            String categoriaNombre,
            Set<String> ingredientesSeleccionados,
            byte[] imagenBytes,
            String nombreArchivo
    );

    Producto actualizarProductoConIngredientes(
            String codigo,
            String nombre,
            String descripcion,
            BigDecimal precio,
            String categoriaNombre,
            Set<String> ingredientesSeleccionados,
            byte[] imagenBytes,
            String nombreArchivo
    );

    Producto obtenerPorCodigo(String codigo);

    Producto obtenerConIngredientesPorCodigo(String codigo);

    List<Producto> listarProductos();

    List<Producto> buscarPorCategoria(String categoriaNombre);

    List<Producto> buscarPorNombreParcial(String nombre);

    List<Producto> buscarPorDescripcionParcial(String descripcion);

    void eliminarProducto(String codigo);

    List<String> listarNombresIngredientes();

    boolean productoTieneIngredientes(String codigoProducto);
}