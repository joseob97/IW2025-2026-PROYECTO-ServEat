package com.serveat.service.repartidor.impl;

import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.repartidor.RepartidorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class RepartidorServiceImpl implements RepartidorService {

    private final PedidoRepository pedidoRepo;
    private final EmpleadoRepository empleadoRepo;

    public RepartidorServiceImpl(PedidoRepository pedidoRepo, EmpleadoRepository empleadoRepo) {
        this.pedidoRepo = pedidoRepo;
        this.empleadoRepo = empleadoRepo;
    }

    // Pedidos a domicilio listos para asignación
    @Override
    public List<Pedido> listarPedidosPendientes() {
        return pedidoRepo.findByTipoPedidoAndEstadoReparto(
                TipoPedidoCliente.DOMICILIO,
                EstadoReparto.PENDIENTE_ASIGNACION
        );
    }

    // Pedidos asignados a este repartidor
    @Override
    public List<Pedido> listarMisPedidos(String repartidorUsername) {
        if (repartidorUsername == null || repartidorUsername.isBlank()) {
            throw new IllegalArgumentException("Repartidor inválido");
        }
        return pedidoRepo.findByRepartidor_Username(repartidorUsername);
    }

    // Asignarse un pedido
    @Override
    public Pedido asignarmePedido(String codigoPedido, String repartidorUsername) {

        if (codigoPedido == null || codigoPedido.isBlank()) {
            throw new IllegalArgumentException("Código de pedido inválido");
        }
        if (repartidorUsername == null || repartidorUsername.isBlank()) {
            throw new IllegalArgumentException("Repartidor inválido");
        }

        Pedido pedido = pedidoRepo.findWithDetalleByCodigo(codigoPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        if (pedido.getTipoPedido() != TipoPedidoCliente.DOMICILIO) {
            throw new IllegalArgumentException("Este pedido no es a domicilio");
        }

        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido está anulado");
        }

        if (pedido.getEstadoReparto() != EstadoReparto.PENDIENTE_ASIGNACION) {
            throw new IllegalArgumentException("Pedido no disponible para asignación");
        }

        Empleado repartidor = empleadoRepo.findByUsername(repartidorUsername)
                .orElseThrow(() -> new IllegalArgumentException("Repartidor no encontrado"));

        pedido.setRepartidor(repartidor);
        pedido.setEstadoReparto(EstadoReparto.ASIGNADO);
        pedido.setFechaAsignacionReparto(LocalDateTime.now());

        return pedidoRepo.save(pedido);
    }

    // Marcar salida
    @Override
    public Pedido marcarEnReparto(String codigoPedido, String repartidorUsername) {

        Pedido pedido = validarPedidoRepartidor(codigoPedido, repartidorUsername);

        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido está anulado");
        }

        if (pedido.getEstadoReparto() != EstadoReparto.ASIGNADO) {
            throw new IllegalArgumentException("Solo puedes marcar EN_REPARTO si está ASIGNADO");
        }

        pedido.setEstadoReparto(EstadoReparto.EN_REPARTO);
        pedido.setFechaSalidaReparto(LocalDateTime.now());

        return pedidoRepo.save(pedido);
    }

    // Marcar entregado
    @Override
    public Pedido marcarEntregado(String codigoPedido, String repartidorUsername) {

        Pedido pedido = validarPedidoRepartidor(codigoPedido, repartidorUsername);

        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido está anulado");
        }

        if (pedido.getEstadoReparto() != EstadoReparto.EN_REPARTO) {
            throw new IllegalArgumentException("Solo puedes marcar ENTREGADO si está EN_REPARTO");
        }

        pedido.setEstadoReparto(EstadoReparto.ENTREGADO);
        pedido.setFechaEntrega(LocalDateTime.now());

        return pedidoRepo.save(pedido);
    }

    // Marcar incidencia
    @Override
    public Pedido marcarIncidencia(String codigoPedido, String repartidorUsername, String motivo) {

        Pedido pedido = validarPedidoRepartidor(codigoPedido, repartidorUsername);

        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido está anulado");
        }

        if (pedido.getEstadoReparto() == EstadoReparto.ENTREGADO) {
            throw new IllegalArgumentException("No puedes marcar incidencia: el pedido ya está entregado");
        }

        String motivoFinal = (motivo == null || motivo.isBlank())
                ? "Incidencia reportada por el repartidor"
                : motivo.trim();

        pedido.setEstadoReparto(EstadoReparto.INCIDENCIA);
        pedido.setIncidenciaReparto(motivoFinal);

        return pedidoRepo.save(pedido);
    }

    // Valida que el pedido exista y esté asignado al repartidor
    private Pedido validarPedidoRepartidor(String codigoPedido, String repartidorUsername) {

        if (codigoPedido == null || codigoPedido.isBlank()) {
            throw new IllegalArgumentException("Código de pedido inválido");
        }
        if (repartidorUsername == null || repartidorUsername.isBlank()) {
            throw new IllegalArgumentException("Repartidor inválido");
        }

        Pedido pedido = pedidoRepo.findWithDetalleByCodigo(codigoPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        if (pedido.getRepartidor() == null
                || pedido.getRepartidor().getUsername() == null
                || !pedido.getRepartidor().getUsername().equals(repartidorUsername)) {
            throw new IllegalArgumentException("Pedido no asignado a este repartidor");
        }

        if (pedido.getTipoPedido() != TipoPedidoCliente.DOMICILIO) {
            throw new IllegalArgumentException("Este pedido no es a domicilio");
        }

        EstadoReparto er = pedido.getEstadoReparto();
        if (er == null || er == EstadoReparto.NO_APLICA) {
            throw new IllegalArgumentException("Este pedido no tiene reparto asociado");
        }

        return pedido;
    }
}