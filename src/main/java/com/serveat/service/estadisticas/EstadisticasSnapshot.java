package com.serveat.service.estadisticas;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Snapshot de estadísticas para un rango de fechas [desde, hasta].
 * Pensado para cache + precálculo (scheduler) y para alimentar vistas.
 *
 */
public class EstadisticasSnapshot implements Serializable {

    private final LocalDate desde;
    private final LocalDate hasta;

    private final boolean hayDatos;

    // KPIs
    private final long totalPedidos;
    private final long pedidosConfirmados;
    private final long pedidosCancelados;
    private final long pagosConfirmados;
    private final BigDecimal totalFacturado;

    // Rankings opcionales (si no se usan, van vacíos)
    private final List<Map<String, Object>> topUnidades;
    private final List<Map<String, Object>> topFacturacion;

    /** Constructor completo (incluye tops). */
    public EstadisticasSnapshot(
            LocalDate desde,
            LocalDate hasta,
            boolean hayDatos,
            long totalPedidos,
            long pedidosConfirmados,
            long pedidosCancelados,
            long pagosConfirmados,
            BigDecimal totalFacturado,
            List<Map<String, Object>> topUnidades,
            List<Map<String, Object>> topFacturacion
    ) {
        this.desde = desde;
        this.hasta = hasta;
        this.hayDatos = hayDatos;

        this.totalPedidos = totalPedidos;
        this.pedidosConfirmados = pedidosConfirmados;
        this.pedidosCancelados = pedidosCancelados;
        this.pagosConfirmados = pagosConfirmados;
        this.totalFacturado = totalFacturado == null ? BigDecimal.ZERO : totalFacturado;

        this.topUnidades = topUnidades == null ? List.of() : topUnidades;
        this.topFacturacion = topFacturacion == null ? List.of() : topFacturacion;
    }

    /** Constructor cómodo SOLO KPIs (tops vacíos). */
    public EstadisticasSnapshot(
            LocalDate desde,
            LocalDate hasta,
            boolean hayDatos,
            long totalPedidos,
            long pedidosConfirmados,
            long pedidosCancelados,
            long pagosConfirmados,
            BigDecimal totalFacturado
    ) {
        this(desde, hasta, hayDatos,
                totalPedidos, pedidosConfirmados, pedidosCancelados, pagosConfirmados,
                totalFacturado, List.of(), List.of());
    }

    public LocalDate getDesde() { return desde; }

    public LocalDate getHasta() { return hasta; }

    public boolean isHayDatos() { return hayDatos; }

    public long getTotalPedidos() { return totalPedidos; }

    public long getPedidosConfirmados() { return pedidosConfirmados; }

    public long getPedidosCancelados() { return pedidosCancelados; }

    public long getPagosConfirmados() { return pagosConfirmados; }

    public BigDecimal getTotalFacturado() { return totalFacturado; }

    public List<Map<String, Object>> getTopUnidades() { return topUnidades; }

    public List<Map<String, Object>> getTopFacturacion() { return topFacturacion; }

    /** Factory útil para "No hay datos disponibles" (todo a 0). */
    public static EstadisticasSnapshot vacio(LocalDate desde, LocalDate hasta) {
        return new EstadisticasSnapshot(
                desde,
                hasta,
                false,
                0L, 0L, 0L, 0L,
                BigDecimal.ZERO,
                List.of(),
                List.of()
        );
    }
}