package com.serveat.service.cocina.impl;

import com.serveat.domain.pedido.*;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.cocina.CocineroService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CocineroServiceImpl implements CocineroService {

    private final PedidoRepository pedidoRepo;

    public CocineroServiceImpl(PedidoRepository pedidoRepo) {
        this.pedidoRepo = pedidoRepo;
    }

    @Override
    public List<Pedido> listarPendientes() {
        return pedidoRepo.findByEstadoAndEstadoCocina(
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    @Override
    public List<Pedido> listarPendientesAceptacion() {
        // Busca pedidos de cliente en EN_CURSO o ya en EN_COCINA pendientes de aceptación
        return pedidoRepo.findByEstadoOrEstadoAndEstadoCocina(
                EstadoPedido.EN_CURSO,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    @Override
    public List<Pedido> listarPedidosEnCurso() {
        return pedidoRepo.findAll().stream()
                .filter(p ->
                        p.getEstado() == EstadoPedido.EN_COCINA &&
                                (p.getEstadoCocina() == EstadoCocina.ACEPTADO
                                        || p.getEstadoCocina() == EstadoCocina.EN_PREPARACION)
                )
                .toList();
    }

    @Override
    public Pedido aceptarPedido(String codigoPedido, String cocineroUsername) {
        Pedido p = cargar(codigoPedido);

        if (p.getEstadoCocina() != EstadoCocina.PENDIENTE_ACEPTACION) {
            throw new IllegalArgumentException("Pedido no aceptable");
        }

        p.setEstado(EstadoPedido.EN_COCINA);
        p.setEstadoCocina(EstadoCocina.ACEPTADO);

        p.setModificadoPor(cocineroUsername);
        p.setFechaUltimaModificacion(LocalDateTime.now());

        return pedidoRepo.save(p);
    }

    @Override
    public Pedido marcarEnPreparacion(String codigoPedido, String cocineroUsername) {
        Pedido p = cargar(codigoPedido);

        if (p.getEstadoCocina() != EstadoCocina.ACEPTADO) {
            throw new IllegalArgumentException("El pedido no está aceptado");
        }

        p.setEstadoCocina(EstadoCocina.EN_PREPARACION);

        p.setModificadoPor(cocineroUsername);
        p.setFechaUltimaModificacion(LocalDateTime.now());

        return pedidoRepo.save(p);
    }

    @Override
    public Pedido marcarListo(String codigoPedido, String cocineroUsername) {
        Pedido p = cargar(codigoPedido);

        if (p.getEstadoCocina() != EstadoCocina.EN_PREPARACION) {
            throw new IllegalArgumentException("El pedido no está en preparación");
        }

        p.setEstadoCocina(EstadoCocina.LISTO);

        // Si es domicilio, queda listo para reparto
        if (p.getTipoPedido() == TipoPedidoCliente.DOMICILIO) {
            p.setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);
        }

        p.setModificadoPor(cocineroUsername);
        p.setFechaUltimaModificacion(LocalDateTime.now());

        return pedidoRepo.save(p);
    }

    @Override
    public Pedido cancelarDesdeCocina(String codigoPedido, String motivo, String cocineroUsername) {
        Pedido p = cargar(codigoPedido);

        p.setEstadoCocina(EstadoCocina.CANCELADO);
        p.setEstado(EstadoPedido.ANULADO);
        p.setCanceladoPor(cocineroUsername);
        p.setMotivoCancelacion(motivo);
        p.setFechaCancelacion(LocalDateTime.now());

        p.setModificadoPor(cocineroUsername);
        p.setFechaUltimaModificacion(LocalDateTime.now());

        return pedidoRepo.save(p);
    }

    private Pedido cargar(String codigo) {
        return pedidoRepo.findWithDetalleByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Pedido> buscarPendientesAceptacion(LocalDateTime desde,
                                                   LocalDateTime hasta,
                                                   Integer mesa,
                                                   Pageable pageable) {
        return pedidoRepo.buscarPedidosCocinaHistorico(
                desde, hasta, EstadoCocina.PENDIENTE_ACEPTACION, mesa, pageable
        );
    }

    public Page<Pedido> buscarPedidosCocinaHoy(LocalDateTime desde,
                                               LocalDateTime hasta,
                                               EstadoCocina estado,
                                               Integer mesa,
                                               Pageable pageable) {
        return pedidoRepo.buscarPedidosCocinaHoy(desde, hasta, estado, mesa, pageable);
    }

    public Page<Pedido> buscarPedidosCocinaHistorico(LocalDateTime desde,
                                                     LocalDateTime hasta,
                                                     EstadoCocina estado,
                                                     Integer mesa,
                                                     Pageable pageable) {
        return pedidoRepo.buscarPedidosCocinaHistorico(desde, hasta, estado, mesa, pageable);
    }


}