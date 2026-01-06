package com.serveat.view.empleado.cocinero;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.cocina.CocineroService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.IntegerField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PedidosCocinaHoyViewTest {

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
    void constructor_monta_componentes_y_pageSize_10_y_boton_historico() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        Page<Pedido> empty = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(cocineroService.buscarPedidosCocinaHoy(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(empty);

        PedidosCocinaHoyView view = new PedidosCocinaHoyView(cocineroService, calculoService);
        UI.getCurrent().add(view);

        assertNotNull(findH3ByText(view, "Pedidos de hoy (Cocina)"));

        assertNotNull(findDatePickerByLabel(view, "Desde"));
        assertNotNull(findDatePickerByLabel(view, "Hasta"));
        assertNotNull(findComboBoxByLabel(view, "Estado cocina"));
        assertNotNull(findIntegerFieldByLabel(view, "Mesa"));

        assertNotNull(findButtonByText(view, "Buscar"));
        assertNotNull(findButtonByText(view, "Limpiar"));
        assertNotNull(findButtonByText(view, "Ver histórico"));

        ArgumentCaptor<Pageable> pageableCap = ArgumentCaptor.forClass(Pageable.class);
        verify(cocineroService, atLeastOnce())
                .buscarPedidosCocinaHoy(any(), any(), any(), any(), pageableCap.capture());

        Pageable used = pageableCap.getValue();
        assertNotNull(used);
        assertEquals(10, used.getPageSize());
    }

    @Test
    void onAttach_aplica_hoy_en_fechas_y_recarga() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        Page<Pedido> empty = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(cocineroService.buscarPedidosCocinaHoy(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(empty);

        LocalDate hoy = LocalDate.now();

        PedidosCocinaHoyView view = new PedidosCocinaHoyView(cocineroService, calculoService);
        UI.getCurrent().add(view);

        DatePicker dpDesde = findDatePickerByLabel(view, "Desde");
        DatePicker dpHasta = findDatePickerByLabel(view, "Hasta");

        assertEquals(hoy, dpDesde.getValue());
        assertEquals(hoy, dpHasta.getValue());

        verify(cocineroService, atLeastOnce())
                .buscarPedidosCocinaHoy(any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void limpiar_aplica_hoy_y_limpia_estado_y_mesa_y_recarga() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        Page<Pedido> empty = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(cocineroService.buscarPedidosCocinaHoy(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(empty);

        PedidosCocinaHoyView view = new PedidosCocinaHoyView(cocineroService, calculoService);
        UI.getCurrent().add(view);

        ComboBox<EstadoCocina> estado = findComboBoxByLabel(view, "Estado cocina");
        IntegerField mesa = findIntegerFieldByLabel(view, "Mesa");
        DatePicker dpDesde = findDatePickerByLabel(view, "Desde");
        DatePicker dpHasta = findDatePickerByLabel(view, "Hasta");

        estado.setValue(EstadoCocina.ACEPTADO);
        mesa.setValue(8);
        dpDesde.setValue(LocalDate.of(2026, 1, 1));
        dpHasta.setValue(LocalDate.of(2026, 1, 2));

        NotificationsKt.getNotifications().clear();
        int before = mockingDetails(cocineroService).getInvocations().size();

        findButtonByText(view, "Limpiar").click();

        assertNull(estado.getValue());
        assertNull(mesa.getValue());

        LocalDate hoy = LocalDate.now();
        assertEquals(hoy, dpDesde.getValue());
        assertEquals(hoy, dpHasta.getValue());

        int after = mockingDetails(cocineroService).getInvocations().size();
        assertTrue(after > before);
    }

    @Test
    void buscar_pasa_filtros_al_servicio_y_actualiza_infoPagina() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        List<Pedido> content = new ArrayList<>();
        Pedido p = new Pedido();
        p.setCodigo("P-1");
        p.setEstadoCocina(EstadoCocina.ACEPTADO);
        content.add(p);

        Page<Pedido> page = new PageImpl<>(content, PageRequest.of(0, 10), 1);
        when(cocineroService.buscarPedidosCocinaHoy(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        PedidosCocinaHoyView view = new PedidosCocinaHoyView(cocineroService, calculoService);
        UI.getCurrent().add(view);

        ComboBox<EstadoCocina> cbEstado = findComboBoxByLabel(view, "Estado cocina");
        IntegerField mesa = findIntegerFieldByLabel(view, "Mesa");

        cbEstado.setValue(EstadoCocina.ACEPTADO);
        mesa.setValue(7);

        findButtonByText(view, "Buscar").click();

        verify(cocineroService, atLeastOnce())
                .buscarPedidosCocinaHoy(any(), any(), eq(EstadoCocina.ACEPTADO), eq(7), any(Pageable.class));

        Span info = findSpanContainingText(view, "Mostrando");
        assertNotNull(info);
        assertTrue(info.getText().contains("Mostrando 1-1 de 1"));
    }

    @Test
    void si_el_servicio_lanza_excepcion_no_revienta_y_muestra_notificacion() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        when(cocineroService.buscarPedidosCocinaHoy(any(), any(), any(), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("boom"));

        PedidosCocinaHoyView view = new PedidosCocinaHoyView(cocineroService, calculoService);
        UI.getCurrent().add(view);

        assertNotNull(findFirstGrid(view));
        assertTrue(!NotificationsKt.getNotifications().isEmpty());
    }

    @Test
    void columnas_base_se_configuran_y_total_se_calcula() {
        CocineroService cocineroService = mock(CocineroService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        Pedido p = new Pedido();
        p.setCodigo("P-1");
        p.setEstadoCocina(EstadoCocina.ACEPTADO);

        Page<Pedido> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);
        when(cocineroService.buscarPedidosCocinaHoy(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        when(calculoService.calcularTotalPedido(p)).thenReturn(new BigDecimal("12.50"));

        PedidosCocinaHoyView view = new PedidosCocinaHoyView(cocineroService, calculoService);
        UI.getCurrent().add(view);

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);
        assertTrue(grid.getColumns().size() >= 5);
    }

    private static H3 findH3ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H3 h3 && text.equals(h3.getText())) return h3;
        }
        return null;
    }

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
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

    private static Span findSpanContainingText(Component root, String partial) {
        for (Component c : flatten(root)) {
            if (c instanceof Span s && s.getText() != null && s.getText().contains(partial)) return s;
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
}