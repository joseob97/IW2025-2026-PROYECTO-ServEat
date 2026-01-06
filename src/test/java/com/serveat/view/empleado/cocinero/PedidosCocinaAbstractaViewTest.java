package com.serveat.view.empleado.cocinero;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.service.cocina.CocineroService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidosCocinaAbstractaViewTest {

    @BeforeEach
    void setupVaadin() {
        MockVaadin.setup();
        NotificationsKt.getNotifications().clear();
    }

    @AfterEach
    void tearDownVaadin() {
        MockVaadin.tearDown();
    }

    @Test
    void initView_sin_filtro_estado_oculta_combo_y_busca_con_estado_null() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        when(calculoService.calcularTotalPedido(any(Pedido.class))).thenReturn(new BigDecimal("10.00"));

        AtomicReference<EstadoCocina> estadoCapturado = new AtomicReference<>();

        TestView view = new TestView(
                cocineroService,
                calculoService,
                false,
                (pageable, d, h, estado, mesa) -> {
                    estadoCapturado.set(estado);
                    return emptyPage(pageable);
                }
        );

        view.initView("Titulo", null, "300px");
        UI.getCurrent().add(view);

        assertNull(estadoCapturado.get());
    }

    @Test
    void initView_con_filtro_estado_muestra_combo_y_busca_con_estado_seleccionado() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        when(calculoService.calcularTotalPedido(any(Pedido.class))).thenReturn(new BigDecimal("10.00"));

        AtomicReference<EstadoCocina> estadoCapturado = new AtomicReference<>();

        TestView view = new TestView(
                cocineroService,
                calculoService,
                true,
                (pageable, d, h, estado, mesa) -> {
                    estadoCapturado.set(estado);
                    return emptyPage(pageable);
                }
        );

        view.initView("Titulo", null, "300px");
        UI.getCurrent().add(view);

        ComboBox<EstadoCocina> combo = findComboBoxByLabel(view, "Estado cocina");
        assertNotNull(combo);
        assertTrue(combo.isVisible());

        combo.setValue(EstadoCocina.ACEPTADO);
        view.btnBuscar.click();

        assertEquals(EstadoCocina.ACEPTADO, estadoCapturado.get());
    }

    @Test
    void buscar_con_fechas_y_mesa_pasa_parametros_correctos() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        when(calculoService.calcularTotalPedido(any(Pedido.class))).thenReturn(new BigDecimal("10.00"));

        AtomicReference<LocalDate> desdeCapt = new AtomicReference<>();
        AtomicReference<LocalDate> hastaCapt = new AtomicReference<>();
        AtomicReference<Integer> mesaCapt = new AtomicReference<>();

        TestView view = new TestView(
                cocineroService,
                calculoService,
                true,
                (pageable, d, h, estado, mesa) -> {
                    desdeCapt.set(d == null ? null : d.toLocalDate());
                    hastaCapt.set(h == null ? null : h.toLocalDate());
                    mesaCapt.set(mesa);
                    return emptyPage(pageable);
                }
        );

        view.initView("Titulo", null, "300px");
        UI.getCurrent().add(view);

        DatePicker dpDesde = findDatePickerByLabel(view, "Desde");
        DatePicker dpHasta = findDatePickerByLabel(view, "Hasta");
        IntegerField mesa = findIntegerFieldByLabel(view, "Mesa");

        assertNotNull(dpDesde);
        assertNotNull(dpHasta);
        assertNotNull(mesa);

        dpDesde.setValue(LocalDate.of(2026, 1, 2));
        dpHasta.setValue(LocalDate.of(2026, 1, 5));
        mesa.setValue(7);

        view.btnBuscar.click();

        assertEquals(LocalDate.of(2026, 1, 2), desdeCapt.get());
        assertEquals(LocalDate.of(2026, 1, 5), hastaCapt.get());
        assertEquals(7, mesaCapt.get());
    }

    @Test
    void paginacion_habilita_y_desabilita_prev_next_y_actualiza_info() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        when(calculoService.calcularTotalPedido(any(Pedido.class))).thenReturn(new BigDecimal("10.00"));

        AtomicInteger llamadas = new AtomicInteger(0);

        TestView view = new TestView(
                cocineroService,
                calculoService,
                true,
                (pageable, d, h, estado, mesa) -> {
                    int call = llamadas.getAndIncrement();
                    if (call == 0) {
                        return pageWithTotal(pageable, 25, 10);
                    }
                    if (call == 1) {
                        return pageWithTotal(pageable, 25, 10);
                    }
                    return pageWithTotal(pageable, 25, 5);
                }
        );

        view.initView("Titulo", null, "300px");
        UI.getCurrent().add(view);

        assertFalse(view.prev.isEnabled());
        assertTrue(view.next.isEnabled());
        assertTrue(view.infoPagina.getText().contains("Mostrando 1-10 de 25"));

        view.next.click();
        assertTrue(view.prev.isEnabled());
        assertTrue(view.next.isEnabled());
        assertTrue(view.infoPagina.getText().contains("Mostrando 11-20 de 25"));

        view.next.click();
        assertTrue(view.prev.isEnabled());
        assertFalse(view.next.isEnabled());
        assertTrue(view.infoPagina.getText().contains("Mostrando 21-25 de 25"));
    }

    @Test
    void limpiar_llama_limpiarFiltros_y_recarga_pagina() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        when(calculoService.calcularTotalPedido(any(Pedido.class))).thenReturn(new BigDecimal("10.00"));

        AtomicInteger limpiarCalls = new AtomicInteger(0);
        AtomicInteger buscarCalls = new AtomicInteger(0);

        TestView view = new TestView(
                cocineroService,
                calculoService,
                true,
                (pageable, d, h, estado, mesa) -> {
                    buscarCalls.incrementAndGet();
                    return emptyPage(pageable);
                }
        ) {
            @Override
            protected void limpiarFiltros() {
                limpiarCalls.incrementAndGet();
                desde.clear();
                hasta.clear();
                filtroEstado.clear();
                filtroMesa.clear();
            }
        };

        view.initView("Titulo", null, "300px");
        UI.getCurrent().add(view);

        DatePicker dpDesde = findDatePickerByLabel(view, "Desde");
        DatePicker dpHasta = findDatePickerByLabel(view, "Hasta");
        ComboBox<EstadoCocina> cb = findComboBoxByLabel(view, "Estado cocina");
        IntegerField mesa = findIntegerFieldByLabel(view, "Mesa");

        dpDesde.setValue(LocalDate.of(2026, 1, 2));
        dpHasta.setValue(LocalDate.of(2026, 1, 3));
        cb.setValue(EstadoCocina.ACEPTADO);
        mesa.setValue(3);

        int before = buscarCalls.get();
        view.btnLimpiar.click();

        assertEquals(1, limpiarCalls.get());
        assertTrue(buscarCalls.get() > before);
        assertNull(dpDesde.getValue());
        assertNull(dpHasta.getValue());
        assertNull(cb.getValue());
        assertNull(mesa.getValue());
    }

    @Test
    void cargarPagina_si_buscar_lanza_excepcion_resetea_estado_y_muestra_notificacion() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        TestView view = new TestView(
                cocineroService,
                calculoService,
                true,
                (pageable, d, h, estado, mesa) -> {
                    throw new RuntimeException("boom");
                }
        );

        view.initView("Titulo", null, "300px");
        UI.getCurrent().add(view);

        assertEquals("", view.infoPagina.getText());
        assertFalse(view.prev.isEnabled());
        assertFalse(view.next.isEnabled());
        assertTrue(hasAnyNotification());
    }

    @Test
    void origenPedido_devuelve_texto_esperado() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        TestView view = new TestView(cocineroService, calculoService, true, (p, d, h, e, m) -> emptyPage(p));
        view.initView("Titulo", null, "300px");
        UI.getCurrent().add(view);

        Pedido p1 = new Pedido();
        p1.setTipoPedido(TipoPedidoCliente.DOMICILIO);
        assertEquals("Domicilio", view.exposeOrigen(p1));

        Pedido p2 = new Pedido();
        p2.setTipoPedido(TipoPedidoCliente.RECOGER);
        assertEquals("Recogida", view.exposeOrigen(p2));

        Pedido p3 = new Pedido();
        p3.setTipoPedido(TipoPedidoCliente.MESA);
        p3.setReservaMesa(new ReservaMesa(5));
        assertEquals("Mesa 5", view.exposeOrigen(p3));

        Pedido p4 = new Pedido();
        p4.setTipoPedido(TipoPedidoCliente.MESA);
        assertEquals("Mesa", view.exposeOrigen(p4));

        assertEquals("Cliente", view.exposeOrigen(null));
    }

    private static Page<Pedido> emptyPage(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    private static Page<Pedido> pageWithTotal(Pageable pageable, long total, int contentSize) {
        List<Pedido> content = new ArrayList<>();
        for (int i = 0; i < contentSize; i++) {
            Pedido p = new Pedido();
            p.setCodigo("P-" + UUID.randomUUID());
            p.setEstadoCocina(EstadoCocina.ACEPTADO);
            content.add(p);
        }
        return new PageImpl<>(content, pageable, total);
    }

    private static boolean hasAnyNotification() {
        return !NotificationsKt.getNotifications().isEmpty();
    }

    private static DatePicker findDatePickerByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof DatePicker dp && label.equals(dp.getLabel())) return dp;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<EstadoCocina> findComboBoxByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof ComboBox<?> cb && label.equals(cb.getLabel())) return (ComboBox<EstadoCocina>) cb;
        }
        return null;
    }

    private static IntegerField findIntegerFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof IntegerField f && label.equals(f.getLabel())) return f;
        }
        return null;
    }

    private static Grid<?> findFirstGrid(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof Grid<?> g) return g;
        }
        return null;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        if (c == null) return out;
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }

    @FunctionalInterface
    private interface SearchFn {
        Page<Pedido> buscar(Pageable pageable,
                            java.time.LocalDateTime desde,
                            java.time.LocalDateTime hasta,
                            EstadoCocina estado,
                            Integer mesa);
    }

    private static class TestView extends PedidosCocinaAbstractaView {

        private final boolean useEstado;
        private final SearchFn fn;

        TestView(CocineroService cocineroService,
                 PedidoCalculoService pedidoCalculoService,
                 boolean useEstado,
                 SearchFn fn) {
            super(cocineroService, pedidoCalculoService);
            this.useEstado = useEstado;
            this.fn = fn;
        }

        @Override
        protected boolean usarFiltroEstado() {
            return useEstado;
        }

        @Override
        protected void limpiarFiltros() {
            desde.clear();
            hasta.clear();
            filtroEstado.clear();
            filtroMesa.clear();
        }

        @Override
        protected void configurarGridColumnas() {
            configurarColumnasCocinaBase(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"), "Fecha", "Ver");
            assertNotNull(findFirstGrid(this));
        }

        @Override
        protected Page<Pedido> buscar(Pageable pageable,
                                      java.time.LocalDateTime desde,
                                      java.time.LocalDateTime hasta,
                                      EstadoCocina estado,
                                      Integer mesa) {
            return fn.buscar(pageable, desde, hasta, estado, mesa);
        }

        String exposeOrigen(Pedido p) {
            return origenPedido(p);
        }
    }
}