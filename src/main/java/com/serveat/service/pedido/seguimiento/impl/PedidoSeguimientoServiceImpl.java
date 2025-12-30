package com.serveat.service.pedido.seguimiento.impl;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.repository.pedido.PedidoSeguimientoRepository;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoDTO;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class PedidoSeguimientoServiceImpl implements PedidoSeguimientoService {

    private final PedidoSeguimientoRepository seguimientoRepo;

    public PedidoSeguimientoServiceImpl(PedidoSeguimientoRepository seguimientoRepo) {
        this.seguimientoRepo = seguimientoRepo;
    }

    @Override
    public Page<Pedido> buscarActivosCliente(String username,
                                             LocalDateTime desde,
                                             LocalDateTime hasta,
                                             EstadoPedido estadoPedido,
                                             EstadoCocina estadoCocina,
                                             EstadoReparto estadoReparto,
                                             Pageable pageable) {
        validarUsername(username);
        validarRango(desde, hasta);

        return seguimientoRepo.buscarActivosClienteFiltrados(
                username, desde, hasta, estadoPedido, estadoCocina, estadoReparto, pageable
        );
    }

    @Override
    public Page<Pedido> buscarAnterioresCliente(String username,
                                                LocalDateTime desde,
                                                LocalDateTime hasta,
                                                EstadoPedido estadoPedido,
                                                EstadoCocina estadoCocina,
                                                EstadoReparto estadoReparto,
                                                Pageable pageable) {
        validarUsername(username);
        validarRango(desde, hasta);

        return seguimientoRepo.buscarAnterioresClienteFiltrados(
                username, desde, hasta, estadoPedido, estadoCocina, estadoReparto, pageable
        );
    }

    @Override
    public PedidoSeguimientoDTO obtenerSeguimientoPedidoCliente(String codigoPedido, String username) {
        if (codigoPedido == null || codigoPedido.isBlank()) throw new IllegalArgumentException("Código inválido");
        validarUsername(username);

        Pedido p = seguimientoRepo.findWithDetalleByCodigoAndCliente_Username(codigoPedido, username)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado o no pertenece al cliente"));

        String estadoPedido = safeEnum(p.getEstado());
        String estadoCocina = safeEnum(p.getEstadoCocina());
        String estadoReparto = safeEnum(p.getEstadoReparto());

        String estiloPedido = estiloEstadoPedido(p.getEstado());
        String estiloCocina = estiloEstadoCocina(p.getEstadoCocina());
        String estiloReparto = estiloEstadoReparto(p.getEstadoReparto());

        String msg = (p.getIncidenciaReparto() != null && !p.getIncidenciaReparto().isBlank())
                ? p.getIncidenciaReparto()
                : null;

        Estimacion estimacion = estimarTiempoYProgreso(p);

        return new PedidoSeguimientoDTO(
                p.getCodigo(),
                estadoPedido,
                estadoCocina,
                estadoReparto,
                estimacion.etiquetaTiempo,
                estimacion.progreso,
                msg,
                estiloPedido,
                estiloCocina,
                estiloReparto
        );
    }

    private Estimacion estimarTiempoYProgreso(Pedido p) {
        if (p == null) return Estimacion.indeterminada();

        if (p.getEstado() == EstadoPedido.ANULADO || p.getEstadoCocina() == EstadoCocina.CANCELADO) {
            return new Estimacion("-", 1.0);
        }

        LocalDateTime inicio = (p.getFechaCreacion() != null) ? p.getFechaCreacion() : LocalDateTime.now();
        long elapsedSec = Math.max(0, Duration.between(inicio, LocalDateTime.now()).getSeconds());

        long totalEstSec = estimacionTotalSegundos(p);

        if (totalEstSec <= 0) {
            return Estimacion.indeterminada();
        }

        long restantes = Math.max(0, totalEstSec - elapsedSec);
        double prog = Math.min(1.0, Math.max(0.0, (double) elapsedSec / (double) totalEstSec));

        return new Estimacion(formatoTiempo(restantes), prog);
    }

    private long estimacionTotalSegundos(Pedido p) {
        TipoPedidoCliente tipo = p.getTipoPedido();

        long cocinaMin = minutosPorEstadoCocina(p.getEstadoCocina());
        long repartoMin = 0;

        if (tipo == TipoPedidoCliente.DOMICILIO) {
            repartoMin = minutosPorEstadoReparto(p.getEstadoReparto());
        }

        long totalMin = Math.max(0, cocinaMin + repartoMin);

        if (p.getEstadoCocina() == EstadoCocina.LISTO && tipo != TipoPedidoCliente.DOMICILIO) {
            totalMin = 0;
        }

        if (p.getFechaEntrega() != null || p.getEstadoReparto() == EstadoReparto.ENTREGADO) {
            totalMin = 0;
        }

        return totalMin * 60;
    }

    private long minutosPorEstadoCocina(EstadoCocina ec) {
        if (ec == null) return 15;
        return switch (ec) {
            case PENDIENTE_ACEPTACION -> 15;
            case ACEPTADO -> 12;
            case EN_PREPARACION -> 8;
            case LISTO -> 0;
            case CANCELADO -> 0;
        };
    }

    private long minutosPorEstadoReparto(EstadoReparto er) {
        if (er == null) return 20;
        return switch (er) {
            case NO_APLICA -> 0;
            case PENDIENTE_ASIGNACION -> 20;
            case ASIGNADO -> 18;
            case EN_REPARTO -> 10;
            case ENTREGADO -> 0;
            case INCIDENCIA -> 0;
        };
    }

    private String estiloEstadoPedido(EstadoPedido ep) {
        if (ep == null) return "NEUTRO";
        return switch (ep) {
            case ANULADO -> "ERROR";
            case EN_COCINA, EN_CURSO -> "INFO";
        };
    }

    private String estiloEstadoCocina(EstadoCocina ec) {
        if (ec == null) return "NEUTRO";
        return switch (ec) {
            case CANCELADO -> "ERROR";
            case LISTO -> "OK";
            case PENDIENTE_ACEPTACION, ACEPTADO, EN_PREPARACION -> "INFO";
        };
    }

    private String estiloEstadoReparto(EstadoReparto er) {
        if (er == null) return "NEUTRO";
        return switch (er) {
            case INCIDENCIA -> "ERROR";
            case ENTREGADO -> "OK";
            case NO_APLICA -> "NEUTRO";
            case PENDIENTE_ASIGNACION, ASIGNADO, EN_REPARTO -> "INFO";
        };
    }

    private String safeEnum(Object e) {
        return e != null ? e.toString() : "-";
    }

    private String formatoTiempo(long segundos) {
        if (segundos <= 0) return "0 min 00 s";
        long m = segundos / 60;
        long s = segundos % 60;
        return m + " min " + (s < 10 ? "0" : "") + s + " s";
    }

    private void validarUsername(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario inválido");
    }

    private void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("Rango de fechas inválido: 'desde' > 'hasta'");
        }
    }

    private record Estimacion(String etiquetaTiempo, Double progreso) {
        static Estimacion indeterminada() {
            return new Estimacion("-", null);
        }
    }
}