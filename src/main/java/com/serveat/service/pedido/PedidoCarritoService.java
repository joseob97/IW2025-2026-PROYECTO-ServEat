package com.serveat.service.pedido;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.Pedido;

import java.util.Map;
import java.util.UUID;

public interface PedidoCarritoService {

    Pedido agregarProducto(
            Pedido carrito,
            Producto producto,
            int cantidad
    );

    Pedido agregarProductoPersonalizado(
            Pedido carrito,
            String codigoProducto,
            int cantidad,
            Map<UUID, Boolean> incluidoPorIngrediente,
            Map<UUID, Integer> extraPorIngrediente
    );

    Pedido actualizarCantidadLinea(
            Pedido carrito,
            String codigoLinea,
            int nuevaCantidad
    );

    Pedido eliminarLinea(
            Pedido carrito,
            String codigoLinea
    );

    void volcarCarritoEnPedido(
            String codigoPedido,
            Pedido carrito
    );
}