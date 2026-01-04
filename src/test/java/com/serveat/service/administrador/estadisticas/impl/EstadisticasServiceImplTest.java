package com.serveat.service.administrador.estadisticas.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.administrador.estadisticas.EstadisticasSnapshot;
import com.serveat.service.caja.EstadoCajaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstadisticasServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private EstadoCajaService estadoCajaService;

    @InjectMocks
    private EstadisticasServiceImpl service;

    @Test
    void snapshotRango_si_rango_invalido_lanza_illegalArgument() {
        LocalDate desde = LocalDate.of(2026, 1, 10);
        LocalDate hasta = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> service.snapshotRango(desde, hasta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha 'Desde' no puede ser posterior a 'Hasta'.");

        verifyNoInteractions(pedidoRepository, pagoRepository, estadoCajaService);
    }

    @Test
    void snapshotRango_si_no_hay_pedidos_devuelve_snapshot_vacio() {
        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of());

        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);

        EstadisticasSnapshot snap = service.snapshotRango(desde, hasta);

        assertThat(snap).isNotNull();
        assertThat(snap.getDesde()).isEqualTo(desde);
        assertThat(snap.getHasta()).isEqualTo(hasta);
        assertThat(snap.isHayDatos()).isFalse();
        assertThat(snap.getTotalPedidos()).isZero();
        assertThat(snap.getPedidosConfirmados()).isZero();
        assertThat(snap.getPedidosCancelados()).isZero();
        assertThat(snap.getPagosConfirmados()).isZero();
        assertThat(snap.getTotalFacturado()).isEqualByComparingTo("0.00");
        assertThat(snap.getTopUnidades()).isEmpty();
        assertThat(snap.getTopFacturacion()).isEmpty();

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
        verifyNoInteractions(pagoRepository);
    }

    @Test
    void snapshotRango_calcula_kpis_y_totalFacturado_de_pagos_confirmados_en_rango() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);

        Pedido p1 = pedido("P-1", EstadoPedido.EN_COCINA, LocalDateTime.of(2026, 1, 5, 10, 0));
        Pedido p2 = pedido("P-2", EstadoPedido.ANULADO, LocalDateTime.of(2026, 1, 6, 10, 0));
        Pedido pFuera = pedido("P-3", EstadoPedido.EN_COCINA, LocalDateTime.of(2025, 12, 31, 23, 59));

        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(p1, p2, pFuera));

        Pago pago1 = pagoConfirmado(p1, MetodoPago.TARJETA, new BigDecimal("10.00"));
        Pago pago2 = pagoConfirmado(p2, MetodoPago.EFECTIVO, new BigDecimal("2.50"));
        Pago pagoFuera = pagoConfirmado(pFuera, MetodoPago.PAYPAL, new BigDecimal("99.99"));

        when(pagoRepository.findByEstado(EstadoPago.CONFIRMADO)).thenReturn(List.of(pago1, pago2, pagoFuera));

        EstadisticasSnapshot snap = service.snapshotRango(desde, hasta);

        assertThat(snap).isNotNull();
        assertThat(snap.getDesde()).isEqualTo(desde);
        assertThat(snap.getHasta()).isEqualTo(hasta);
        assertThat(snap.isHayDatos()).isTrue();

        // En rango hay 2 pedidos (p1, p2)
        assertThat(snap.getTotalPedidos()).isEqualTo(2);

        // Confirmados se consideran EN_COCINA
        assertThat(snap.getPedidosConfirmados()).isEqualTo(1);

        // Cancelados se consideran ANULADO
        assertThat(snap.getPedidosCancelados()).isEqualTo(1);

        // Pagos confirmados en rango se filtran por fechaCreacion del pedido asociado
        assertThat(snap.getPagosConfirmados()).isEqualTo(2);

        // Total facturado suma importes de pagos confirmados en rango
        assertThat(snap.getTotalFacturado()).isEqualByComparingTo("12.50");

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
        verify(pagoRepository).findByEstado(EstadoPago.CONFIRMADO);
    }

    @Test
    void topProductosPorUnidades_devuelve_ranking_ordenado_y_limitado() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);

        Producto pizza = producto("Pizza", "PZ-1", new BigDecimal("12.00"));
        Producto burger = producto("Burger", "BG-1", new BigDecimal("9.00"));

        Pedido p1 = pedido("P-1", EstadoPedido.EN_COCINA, LocalDateTime.of(2026, 1, 10, 10, 0));
        Pedido p2 = pedido("P-2", EstadoPedido.EN_CURSO, LocalDateTime.of(2026, 1, 11, 10, 0));

        p1.setLineaPedidos(setOf(
                linea(p1, pizza, 2),
                linea(p1, burger, 1)
        ));

        p2.setLineaPedidos(setOf(
                linea(p2, pizza, 3)
        ));

        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(p1, p2));

        List<Map<String, Object>> res = service.topProductosPorUnidades(desde, hasta, 1);

        assertThat(res).hasSize(1);
        assertThat(res.get(0)).containsEntry("producto", "Pizza");
        assertThat(res.get(0)).containsEntry("unidades", 5L);

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
        verifyNoInteractions(pagoRepository);
    }

    @Test
    void topProductosPorUnidades_si_limit_invalido_lanza_illegalArgument() {
        assertThatThrownBy(() -> service.topProductosPorUnidades(LocalDate.now(), LocalDate.now(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Limit inválido");

        assertThatThrownBy(() -> service.topProductosPorUnidades(LocalDate.now(), LocalDate.now(), 201))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Limit demasiado alto (máx 200)");

        verifyNoInteractions(pedidoRepository, pagoRepository, estadoCajaService);
    }

    @Test
    void topProductosPorFacturacion_devuelve_ranking_ordenado_y_con_decimales() {
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 1, 31);

        Producto pizza = producto("Pizza", "PZ-1", new BigDecimal("12.00"));
        Producto burger = producto("Burger", "BG-1", new BigDecimal("9.00"));

        Pedido p1 = pedido("P-1", EstadoPedido.EN_COCINA, LocalDateTime.of(2026, 1, 10, 10, 0));
        p1.setLineaPedidos(setOf(
                linea(p1, pizza, 2),   // 24.00
                linea(p1, burger, 1)   // 9.00
        ));

        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(p1));

        List<Map<String, Object>> res = service.topProductosPorFacturacion(desde, hasta, 2);

        assertThat(res).hasSize(2);

        // Orden esperado: Pizza (24.00) > Burger (9.00)
        assertThat(res.get(0)).containsEntry("producto", "Pizza");
        assertThat((BigDecimal) res.get(0).get("total")).isEqualByComparingTo("24.00");

        assertThat(res.get(1)).containsEntry("producto", "Burger");
        assertThat((BigDecimal) res.get(1).get("total")).isEqualByComparingTo("9.00");

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
        verifyNoInteractions(pagoRepository);
    }

    @Test
    void serieMensualUnidades_inicializa_todos_los_meses_y_agrega_unidades() {
        int yearDesde = 2026;
        int yearHasta = 2026;

        Producto pizza = producto("Pizza", "PZ-1", new BigDecimal("12.00"));

        Pedido enero = pedido("P-1", EstadoPedido.EN_COCINA, LocalDateTime.of(2026, 1, 10, 10, 0));
        enero.setLineaPedidos(setOf(
                linea(enero, pizza, 2),
                linea(enero, pizza, 1)
        ));

        Pedido febrero = pedido("P-2", EstadoPedido.EN_COCINA, LocalDateTime.of(2026, 2, 5, 10, 0));
        febrero.setLineaPedidos(setOf(
                linea(febrero, pizza, 4)
        ));

        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(enero, febrero));

        Map<YearMonth, Long> serie = service.serieMensualUnidades(yearDesde, yearHasta);

        assertThat(serie).hasSize(12);
        assertThat(serie.get(YearMonth.of(2026, 1))).isEqualTo(3L);
        assertThat(serie.get(YearMonth.of(2026, 2))).isEqualTo(4L);

        // Meses sin pedidos deben existir y valer 0
        assertThat(serie.get(YearMonth.of(2026, 3))).isEqualTo(0L);

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
    }

    @Test
    void serieMensualFacturacion_inicializa_todos_los_meses_y_agrega_totales_por_lineas() {
        int yearDesde = 2026;
        int yearHasta = 2026;

        Producto pizza = producto("Pizza", "PZ-1", new BigDecimal("12.00"));
        Producto burger = producto("Burger", "BG-1", new BigDecimal("9.00"));

        Pedido enero = pedido("P-1", EstadoPedido.EN_COCINA, LocalDateTime.of(2026, 1, 10, 10, 0));
        enero.setLineaPedidos(setOf(
                linea(enero, pizza, 2),   // 24.00
                linea(enero, burger, 1)   // 9.00
        ));

        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(enero));

        Map<YearMonth, BigDecimal> serie = service.serieMensualFacturacion(yearDesde, yearHasta);

        assertThat(serie).hasSize(12);
        assertThat(serie.get(YearMonth.of(2026, 1))).isEqualByComparingTo("33.00");
        assertThat(serie.get(YearMonth.of(2026, 2))).isEqualByComparingTo("0.00");

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
    }

    @Test
    void serieMensualVista_tipo_unidades_marca_maximo_correctamente() {
        int yearDesde = 2026;
        int yearHasta = 2026;

        Producto pizza = producto("Pizza", "PZ-1", new BigDecimal("12.00"));

        Pedido enero = pedido("P-1", EstadoPedido.EN_COCINA, LocalDateTime.of(2026, 1, 10, 10, 0));
        enero.setLineaPedidos(setOf(linea(enero, pizza, 3)));

        Pedido febrero = pedido("P-2", EstadoPedido.EN_COCINA, LocalDateTime.of(2026, 2, 10, 10, 0));
        febrero.setLineaPedidos(setOf(linea(febrero, pizza, 5)));

        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(enero, febrero));

        List<Map<String, Object>> vista = service.serieMensualVista(yearDesde, yearHasta, "Unidades");

        assertThat(vista).hasSize(12);

        // Debe existir exactamente un max true (febrero con 5)
        long maxCount = vista.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("max")))
                .count();

        assertThat(maxCount).isEqualTo(1);

        Optional<Map<String, Object>> filaMax = vista.stream()
                .filter(r -> Boolean.TRUE.equals(r.get("max")))
                .findFirst();

        assertThat(filaMax).isPresent();
        assertThat((BigDecimal) filaMax.get().get("valor")).isEqualByComparingTo("5.00");

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
    }

    @Test
    void añosDisponibles_si_hay_pedidos_devuelve_años_unicos_ordenados() {
        Pedido p2024 = pedido("P-1", EstadoPedido.EN_CURSO, LocalDateTime.of(2024, 5, 1, 10, 0));
        Pedido p2026 = pedido("P-2", EstadoPedido.EN_CURSO, LocalDateTime.of(2026, 1, 1, 10, 0));
        Pedido p2025 = pedido("P-3", EstadoPedido.EN_CURSO, LocalDateTime.of(2025, 12, 31, 10, 0));

        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(p2026, p2025, p2024));

        List<Integer> years = service.añosDisponibles();

        assertThat(years).containsExactly(2024, 2025, 2026);

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
    }

    @Test
    void añosDisponibles_si_no_hay_pedidos_devuelve_año_actual() {
        when(pedidoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of());

        List<Integer> years = service.añosDisponibles();

        assertThat(years).containsExactly(LocalDate.now().getYear());

        verify(pedidoRepository).findAllByOrderByFechaCreacionDesc();
    }

    @Test
    void generarCierreCajaDiario_agrega_totales_por_metodo() {
        Pago p1 = pagoConMetodoYImporte(MetodoPago.PAYPAL, "10.00");
        Pago p2 = pagoConMetodoYImporte(MetodoPago.EFECTIVO, "5.50");
        Pago p3 = pagoConMetodoYImporte(MetodoPago.TARJETA, "2.00");

        when(pagoRepository.findByEstadoAndFechaConfirmacionBetween(
                eq(EstadoPago.CONFIRMADO),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(p1, p2, p3));

        Map<String, Object> res = service.generarCierreCajaDiario();

        assertThat(res.get("total")).isEqualTo(new BigDecimal("17.50"));
        assertThat(res.get("paypal")).isEqualTo(new BigDecimal("10.00"));
        assertThat(res.get("efectivo")).isEqualTo(new BigDecimal("5.50"));
        assertThat(res.get("tarjeta")).isEqualTo(new BigDecimal("2.00"));

        verify(pagoRepository).findByEstadoAndFechaConfirmacionBetween(
                eq(EstadoPago.CONFIRMADO),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verifyNoInteractions(estadoCajaService);
    }

    @Test
    void generarCierreCajaTurno_usa_ultima_apertura_y_agrega_totales_por_metodo() {
        LocalDateTime inicioTurno = LocalDateTime.of(2026, 1, 4, 8, 0);
        when(estadoCajaService.obtenerFechaUltimaApertura()).thenReturn(Optional.of(inicioTurno));

        Pago p1 = pagoConMetodoYImporte(MetodoPago.TARJETA, "10.00");
        Pago p2 = pagoConMetodoYImporte(MetodoPago.TARJETA, "1.00");
        Pago p3 = pagoConMetodoYImporte(MetodoPago.PAYPAL, "2.50");

        when(pagoRepository.findByEstadoAndFechaConfirmacionBetween(
                eq(EstadoPago.CONFIRMADO),
                eq(inicioTurno),
                any(LocalDateTime.class)
        )).thenReturn(List.of(p1, p2, p3));

        Map<String, Object> res = service.generarCierreCajaTurno();

        assertThat(res.get("total")).isEqualTo(new BigDecimal("13.50"));
        assertThat(res.get("paypal")).isEqualTo(new BigDecimal("2.50"));
        assertThat(res.get("efectivo")).isEqualTo(new BigDecimal("0.00"));
        assertThat(res.get("tarjeta")).isEqualTo(new BigDecimal("11.00"));

        verify(estadoCajaService).obtenerFechaUltimaApertura();
        verify(pagoRepository).findByEstadoAndFechaConfirmacionBetween(
                eq(EstadoPago.CONFIRMADO),
                eq(inicioTurno),
                any(LocalDateTime.class)
        );
    }

    // Helpers

    private static Pedido pedido(String codigo, EstadoPedido estado, LocalDateTime fechaCreacion) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setEstado(estado);
        p.setFechaCreacion(fechaCreacion);
        p.setLineaPedidos(new LinkedHashSet<>());
        return p;
    }

    private static Producto producto(String nombre, String codigo, BigDecimal precio) {
        Producto pr = new Producto();
        pr.setNombre(nombre);
        pr.setCodigo(codigo);
        pr.setPrecio(precio);
        return pr;
    }

    private static LineaPedido linea(Pedido pedido, Producto producto, int cantidad) {
        LineaPedido lp = new LineaPedido(pedido, producto, cantidad);
        lp.setCantidad(cantidad);
        return lp;
    }

    private static Set<LineaPedido> setOf(LineaPedido... lineas) {
        Set<LineaPedido> s = new LinkedHashSet<>();
        if (lineas != null) {
            Collections.addAll(s, lineas);
        }
        return s;
    }

    private static Pago pagoConfirmado(Pedido pedido, MetodoPago metodo, BigDecimal importe) {
        Pago pago = new Pago(pedido, metodo, importe);
        pago.confirmar("REF");
        return pago;
    }

    private static Pago pagoConMetodoYImporte(MetodoPago metodo, String importe) {
        Pedido dummyPedido = new Pedido();
        Pago p = new Pago(dummyPedido, metodo, new BigDecimal(importe));
        p.confirmar("REF");
        return p;
    }
}