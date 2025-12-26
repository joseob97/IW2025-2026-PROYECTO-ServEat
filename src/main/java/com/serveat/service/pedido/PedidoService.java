package com.serveat.service.pedido;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.*;

import java.util.List;
import java.util.UUID;

public interface PedidoService {

    // --- Backoffice / empleados ---
    Pedido crearPedidoMesa(Integer numeroMesa);

    Pedido obtenerPorCodigo(String codigo);

    List<Pedido> listarPedidos();

    List<Pedido> listarTodosOrdenadosPorFecha();

    List<Pedido> buscarPorEstado(EstadoPedido estado);

    List<Pedido> obtenerPedidosPorEstado(EstadoCocina estado);

    List<Pedido> obtenerPedidosPorMesa(Integer numeroMesa);

    List<Pedido> obtenerPedidosPorEstadoYMesa(EstadoCocina estado, Integer numeroMesa);

    Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad);

    Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad);

    Pedido eliminarProducto(String codigoPedido, String codigoProducto);

    // --- Carrito (memoria) ---
    Pedido agregarProductoEnMemoria(Pedido pedido, Producto producto, int cantidad);

    Pedido actualizarCantidadEnMemoria(Pedido pedido, String codigoProducto, int nuevaCantidad);

    Pedido eliminarProductoEnMemoria(Pedido pedido, String codigoProducto);

    // --- Confirmaciones ---
    Pedido confirmarPedido(String codigoPedido);

    Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario);

    Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username);

    // --- Cliente ---
    Pedido cargarDetalleCliente(String codigo, String username);

    Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername);

    boolean puedeModificarCliente(Pedido pedido);

    List<Pedido> listarPedidosModificables();

    List<Pedido> listarPedidosModificablesPorMesa(Integer numeroMesa);

    List<Pedido> listarPedidosCliente(String username);

    // Crear Pedidos
    Pedido crearPedidoClienteRecoger(Pedido carrito, String username);

    Pedido crearPedidoClienteDomicilio(Pedido carrito, String username, String direccionEntrega);

    Pedido crearPedidoClienteMesa(Pedido carrito, String username, Integer numeroMesa);

    // --- Pago online ---
    Pago iniciarPagoOnline(Pedido carrito, String username, MetodoPago metodo);

    Pago obtenerPagoCliente(Long pagoId, String username);

    Pedido confirmarPagoOnline(Long pagoId, String username, String referencia);

    Pedido marcarPagoOnlineFallido(Long pagoId, String username, String motivo);

    // --- Cocina ---
    Pedido obtenerPedidoPorId(UUID id);

    Pedido cambiarEstadoCocina(UUID id, EstadoCocina nuevoEstado);
}