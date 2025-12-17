package com.serveat.service.pedido;

import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.Pedido;

import java.util.List;

public interface PedidoService {

    // Pedido genérico (online / sin mesa)
    Pedido crearPedido();

    // Pedido asociado a mesa (camarero)
    Pedido crearPedidoMesa(Integer numeroMesa);

    // Devuelve el pedido con lineas+producto cargadas
    Pedido obtenerPorCodigo(String codigo);

    // Suma cantidades si el producto ya existe
    Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad);

    // Cambia la cantidad de un producto ya añadido (si nuevaCantidad <= 0 => elimina)
    Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad);

    // Elimina directamente el producto del pedido
    Pedido eliminarProducto(String codigoPedido, String codigoProducto);

    Pedido confirmarPedido(String codigoPedido);

    List<Pedido> buscarPorEstado(EstadoPedido estado);

    Pedido cambiarEstado(String codigoPedido, EstadoPedido nuevoEstado);

    void eliminarPedido(String codigoPedido);

    List<Pedido> listarPedidos();

    List<Pedido> listarPedidosCancelables();
    List<Pedido> listarPedidosCancelablesPorMesa(Integer numeroMesa);

    Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername);
}