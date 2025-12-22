package com.serveat.service.cocina;

import com.serveat.domain.pedido.Pedido;

import java.util.List;

public interface CocineroService {

    List<Pedido> listarPendientes();

    // Pedidos pendientes de aceptar
    List<Pedido> listarPendientesAceptacion();

    // Pedidos aceptados o en preparación
    List<Pedido> listarPedidosEnCurso();

    // Aceptar pedido
    Pedido aceptarPedido(String codigoPedido, String cocineroUsername);

    // Marcar en preparación
    Pedido marcarEnPreparacion(String codigoPedido, String cocineroUsername);

    // Marcar listo
    Pedido marcarListo(String codigoPedido, String cocineroUsername);

    // Cancelar desde cocina (opcional)
    Pedido cancelarDesdeCocina(String codigoPedido, String motivo, String cocineroUsername);
}