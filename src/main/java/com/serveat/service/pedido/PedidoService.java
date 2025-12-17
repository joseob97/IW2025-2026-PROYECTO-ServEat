package com.serveat.service.pedido;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.Pedido;

import java.util.List;

public interface PedidoService {

    // CREACIÓN

    Pedido crearPedido();

    Pedido crearPedidoMesa(Integer numeroMesa);

    // CONSULTA

    Pedido obtenerPorCodigo(String codigo);

    List<Pedido> listarPedidos();

    List<Pedido> buscarPorEstado(EstadoPedido estado);

    // MODIFICACIÓN DIRECTA (SE GUARDA)

    Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad);

    Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad);

    Pedido eliminarProducto(String codigoPedido, String codigoProducto);

    // MODIFICACIÓN EN MEMORIA (NO SE GUARDA)

    Pedido agregarProductoEnMemoria(Pedido pedido, Producto producto, int cantidad);

    Pedido actualizarCantidadEnMemoria(Pedido pedido, String codigoProducto, int nuevaCantidad);

    Pedido eliminarProductoEnMemoria(Pedido pedido, String codigoProducto);

    // CONFIRMACIONES

    Pedido confirmarPedido(String codigoPedido);

    Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario);

    // CANCELACIÓN

    Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername);

    // LISTADOS ESPECIALES

    List<Pedido> listarPedidosModificables();

    List<Pedido> listarPedidosModificablesPorMesa(Integer numeroMesa);
}