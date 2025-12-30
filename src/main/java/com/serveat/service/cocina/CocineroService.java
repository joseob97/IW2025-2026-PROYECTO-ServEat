package com.serveat.service.cocina;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
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

    Page<Pedido> buscarPendientesAceptacion(LocalDateTime desde,
                                            LocalDateTime hasta,
                                            Integer mesa,
                                            Pageable pageable);

    Page<Pedido> buscarPedidosCocinaHoy(LocalDateTime desde,
                                        LocalDateTime hasta,
                                        EstadoCocina estado,
                                        Integer mesa,
                                        Pageable pageable);

    Page<Pedido> buscarPedidosCocinaHistorico(LocalDateTime desde,
                                              LocalDateTime hasta,
                                              EstadoCocina estado,
                                              Integer mesa,
                                              Pageable pageable);

}