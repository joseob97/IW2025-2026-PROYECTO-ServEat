package com.serveat.service.pedido;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.Pedido;

import java.util.List;

public interface PedidoService {

    Pedido crearPedidoMesa(Integer numeroMesa);

    Pedido obtenerPorCodigo(String codigo);

    List<Pedido> listarPedidos();

    List<Pedido> buscarPorEstado(EstadoPedido estado);

    Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad);

    Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad);

    Pedido eliminarProducto(String codigoPedido, String codigoProducto);

    Pedido agregarProductoEnMemoria(Pedido pedido, Producto producto, int cantidad);

    Pedido actualizarCantidadEnMemoria(Pedido pedido, String codigoProducto, int nuevaCantidad);

    Pedido eliminarProductoEnMemoria(Pedido pedido, String codigoProducto);

    Pedido confirmarPedido(String codigoPedido);

    Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario);

    Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username);

    Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername);

    List<Pedido> listarPedidosModificables();

    List<Pedido> listarPedidosModificablesPorMesa(Integer numeroMesa);

    Pedido crearPedidoDesdeCliente(Pedido pedidoEnMemoria, String username);

    List<Pedido> listarPedidosCliente(String username);

    Pedido crearPedidoMesaDesdeCliente(Pedido pedidoEnMemoria, Integer numeroMesa, String username);

    Pedido cargarDetalleCliente(String codigo, String username);

    boolean puedeModificarCliente(Pedido pedido);

    Pago iniciarPagoOnline(Pedido carrito, String username, MetodoPago metodo);

    Pago obtenerPagoCliente(Long pagoId, String username);

    Pedido confirmarPagoOnline(Long pagoId, String username, String referencia);

    Pedido marcarPagoOnlineFallido(Long pagoId, String username, String motivo);
}