package com.serveat.view.empleado.camarero;

import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.pedido.TicketService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PedidosCamareroViewTest {

    @BeforeEach
    void setupUi() {
        UI.setCurrent(new UI());
    }

    @AfterEach
    void tearDownUi() {
        UI.setCurrent(null);
    }

    @Test
    void constructor_no_revienta_y_carga_pagina_inicial() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        TicketService ticketService = mock(TicketService.class);

        Page<?> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(pedidoService.buscarPedidosFiltrados(any(), any(), any(), any(), any(), any()))
                .thenReturn((Page) emptyPage);

        PedidosCamareroView view = new PedidosCamareroView(pedidoService, calculoService, ticketService);
        attachToUi(view);

        assertNotNull(view);

        verify(pedidoService, atLeastOnce()).buscarPedidosFiltrados(any(), any(), any(), any(), any(), any());

        assertNotNull(findH3ByText(view, "Pedidos (Camarero)"));

        assertNotNull(findDatePickerByLabel(view, "Desde"));
        assertNotNull(findDatePickerByLabel(view, "Hasta"));

        assertNotNull(findComboBoxByLabel(view, "Estado pedido"));
        assertNotNull(findComboBoxByLabel(view, "Estado cocina"));

        assertNotNull(findIntegerFieldByLabel(view, "Mesa"));

        assertNotNull(findButtonByText(view, "Buscar"));
        assertNotNull(findButtonByText(view, "Limpiar"));

        assertNotNull(findButtonByText(view, "◀ Anterior"));
        assertNotNull(findButtonByText(view, "Siguiente ▶"));

        assertNotNull(findFirstGrid(view));

        Span infoPagina = findSpanContainingText(view, "Mostrando");
        assertNotNull(infoPagina);
    }

    @Test
    void cargar_pagina_si_el_servicio_lanza_excepcion_no_revienta() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        TicketService ticketService = mock(TicketService.class);

        when(pedidoService.buscarPedidosFiltrados(any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        PedidosCamareroView view = new PedidosCamareroView(pedidoService, calculoService, ticketService);
        attachToUi(view);

        assertNotNull(view);

        verify(pedidoService, atLeastOnce()).buscarPedidosFiltrados(any(), any(), any(), any(), any(), any());
    }

    private static void attachToUi(Component view) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            UI.setCurrent(new UI());
            ui = UI.getCurrent();
        }
        ui.add(view);
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

    private static ComboBox<?> findComboBoxByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof ComboBox<?> cb && label.equals(cb.getLabel())) return cb;
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