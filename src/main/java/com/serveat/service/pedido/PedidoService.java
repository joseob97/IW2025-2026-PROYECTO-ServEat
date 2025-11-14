package com.serveat.service.pedido;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.menu.Producto;

import java.util.List;

public interface PedidoService {

    Pedido crearPedido();

    Pedido obtenerPorCodigo(String codigo);

    Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad);

    List<Pedido> buscarPorEstado(String estado);

    Pedido cambiarEstado(String codigoPedido, String nuevoEstado);

    void eliminarPedido(String codigoPedido);
}