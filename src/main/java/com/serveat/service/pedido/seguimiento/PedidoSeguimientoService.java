package com.serveat.service.pedido.seguimiento;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface PedidoSeguimientoService {

    Page<Pedido> buscarActivosCliente(String username,
                                      LocalDateTime desde,
                                      LocalDateTime hasta,
                                      EstadoPedido estadoPedido,
                                      EstadoCocina estadoCocina,
                                      EstadoReparto estadoReparto,
                                      Pageable pageable);

    Page<Pedido> buscarAnterioresCliente(String username,
                                         LocalDateTime desde,
                                         LocalDateTime hasta,
                                         EstadoPedido estadoPedido,
                                         EstadoCocina estadoCocina,
                                         EstadoReparto estadoReparto,
                                         Pageable pageable);

    PedidoSeguimientoDTO obtenerSeguimientoPedidoCliente(String codigoPedido, String username);
}