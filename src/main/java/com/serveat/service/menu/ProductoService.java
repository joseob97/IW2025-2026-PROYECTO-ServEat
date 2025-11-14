package com.serveat.service.menu;

import com.serveat.domain.menu.Producto;
import java.math.BigDecimal;
import java.util.List;

public interface ProductoService {

    Producto crearProducto(String nombre, String descripcion, BigDecimal precio, String categoriaNombre);

    Producto obtenerPorCodigo(String codigo);

    List<Producto> buscarPorCategoria(String categoriaNombre);

    List<Producto> buscarPorNombreParcial(String nombre);

    List<Producto> buscarPorDescripcionParcial(String descripcion);

    void eliminarProducto(String codigo);
}