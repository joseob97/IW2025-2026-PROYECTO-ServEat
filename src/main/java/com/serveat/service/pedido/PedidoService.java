package com.serveat.service.pedido;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.Pedido;

import java.util.List;

public interface PedidoService {

    // Crea un pedido asociado a una mesa abierta.
    Pedido crearPedidoMesa(Integer numeroMesa);

    // Devuelve un pedido por su código con detalle.
    Pedido obtenerPorCodigo(String codigo);

    // Devuelve todos los pedidos.
    List<Pedido> listarPedidos();

    //Devuelve todos los pedidos ordenados por fecha de creación descendente.
    List<Pedido> listarTodosOrdenadosPorFecha();

    // Devuelve pedidos filtrados por estado.
    List<Pedido> buscarPorEstado(EstadoPedido estado);

    // Devuelve pedidos filtrados por estado de cocina (para cocinero).
    List<Pedido> obtenerPedidosPorEstado(com.serveat.domain.pedido.EstadoCocina estado);

    // NUEVO: Devuelve pedidos filtrados por mesa (ordenados por fecha).
    List<Pedido> obtenerPedidosPorMesa(Integer numeroMesa);

    // NUEVO: Devuelve pedidos filtrados por estado y mesa (ordenados por fecha).
    List<Pedido> obtenerPedidosPorEstadoYMesa(com.serveat.domain.pedido.EstadoCocina estado, Integer numeroMesa);

    // Añade un producto a un pedido persistido.
    Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad);

    // Actualiza cantidad de un producto en un pedido persistido.
    Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad);

    // Elimina un producto de un pedido persistido.
    Pedido eliminarProducto(String codigoPedido, String codigoProducto);

    // Añade un producto a un pedido en memoria (carrito).
    Pedido agregarProductoEnMemoria(Pedido pedido, Producto producto, int cantidad);

    // Actualiza cantidad en un pedido en memoria (carrito).
    Pedido actualizarCantidadEnMemoria(Pedido pedido, String codigoProducto, int nuevaCantidad);

    // Elimina un producto de un pedido en memoria (carrito).
    Pedido eliminarProductoEnMemoria(Pedido pedido, String codigoProducto);

    // Confirma un pedido para enviarlo a cocina.
    Pedido confirmarPedido(String codigoPedido);

    // Persiste cambios en un pedido (camarero).
    Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario);

    // Persiste cambios en un pedido del cliente si es modificable.
    Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username);

    // Cancela un pedido si el estado lo permite.
    Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername);

    // Lista pedidos modificables por regla de negocio.
    List<Pedido> listarPedidosModificables();

    // Lista pedidos modificables filtrando por mesa.
    List<Pedido> listarPedidosModificablesPorMesa(Integer numeroMesa);

    // Crea un pedido desde el carrito del cliente (sin pago).
    Pedido crearPedidoDesdeCliente(Pedido pedidoEnMemoria, String username);

    // Lista pedidos de un cliente ordenados por fecha.
    List<Pedido> listarPedidosCliente(String username);

    // Crea un pedido en mesa desde el carrito del cliente.
    Pedido crearPedidoMesaDesdeCliente(Pedido pedidoEnMemoria, Integer numeroMesa, String username);

    // Carga el pedido de un cliente validando pertenencia.
    Pedido cargarDetalleCliente(String codigo, String username);

    // Indica si un pedido es modificable por el cliente.
    boolean puedeModificarCliente(Pedido pedido);

    // Inicia un pago pendiente creando el pedido del carrito.
    Pago iniciarPagoOnline(Pedido carrito, String username, MetodoPago metodo);

    // Devuelve un pago validando que pertenece al cliente.
    Pago obtenerPagoCliente(Long pagoId, String username);

    // Confirma el pago y envía el pedido a cocina.
    Pedido confirmarPagoOnline(Long pagoId, String username, String referencia);

    // Marca el pago como fallido (sin confirmar pedido).
    Pedido marcarPagoOnlineFallido(Long pagoId, String username, String motivo);

    // Obtiene un pedido por su ID (UUID).
    Pedido obtenerPedidoPorId(java.util.UUID id);

    // Cambia el estado de cocina de un pedido.
    Pedido cambiarEstadoCocina(java.util.UUID id, com.serveat.domain.pedido.EstadoCocina nuevoEstado);
}
