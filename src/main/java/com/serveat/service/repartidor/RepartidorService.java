package com.serveat.service.repartidor;

import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface RepartidorService {

    // Lista pedidos a domicilio pendientes de asignación
    List<Pedido> listarPedidosPendientes();

    // Lista los pedidos asignados al repartidor
    List<Pedido> listarMisPedidos(String repartidorUsername);

    // Permite al repartidor asignarse un pedido disponible
    Pedido asignarmePedido(String codigoPedido, String repartidorUsername);

    // Marca el pedido como salido a reparto
    Pedido marcarEnReparto(String codigoPedido, String repartidorUsername);

    // Marca el pedido como entregado
    Pedido marcarEntregado(String codigoPedido, String repartidorUsername);

    // Marca una incidencia en el reparto
    Pedido marcarIncidencia(String codigoPedido, String repartidorUsername, String motivo);

    Page<Pedido> buscarPedidosDisponibles(LocalDateTime desde,
                                          LocalDateTime hasta,
                                          Pageable pageable);

    Page<Pedido> buscarMisRepartos(String username,
                                   LocalDateTime desde,
                                   LocalDateTime hasta,
                                   EstadoReparto estadoReparto,
                                   Pageable pageable);
}