package com.serveat.view.empleado.repartidor;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.serveat.domain.pedido.Pedido;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepartidorPedidosBaseViewTest {

    private TestView view;

    @BeforeEach
    void setup() {
        MockVaadin.setup();
        view = new TestView();
        UI.getCurrent().add(view);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void se_inicializa_y_carga_datos_al_adjuntarse() {
        assertTrue(view.buscarLlamado);
        assertNotNull(findGrid(view));
        assertNotNull(findTitulo(view, "Vista Test Repartidor"));
    }

    @Test
    void boton_buscar_recarga_pagina() {
        view.buscarLlamado = false;

        findButton(view, "Buscar").click();

        assertTrue(view.buscarLlamado);
    }

    @Test
    void boton_limpiar_limpia_fechas_y_recarga() {
        DatePicker desde = findDatePicker(view, "Desde");
        DatePicker hasta = findDatePicker(view, "Hasta");

        desde.setValue(LocalDate.of(2025, 1, 1));
        hasta.setValue(LocalDate.of(2025, 1, 2));

        view.buscarLlamado = false;

        findButton(view, "Limpiar").click();

        assertNull(desde.getValue());
        assertNull(hasta.getValue());
        assertTrue(view.buscarLlamado);
    }

    @Test
    void boton_refrescar_recarga_pagina() {
        view.buscarLlamado = false;

        findButton(view, "🔄 Refrescar").click();

        assertTrue(view.buscarLlamado);
    }

    // Vista para test

    static class TestView extends RepartidorPedidosBaseView {

        boolean buscarLlamado = false;

        TestView() {
            initBase();
        }

        @Override
        protected String tituloPantalla() {
            return "Vista Test Repartidor";
        }

        @Override
        protected String textoInfo() {
            return "Texto informativo";
        }

        @Override
        protected void configurarGrid() {
            grid.addColumn(Pedido::getCodigo).setHeader("Código");
        }

        @Override
        protected Page<Pedido> buscarPage(java.time.LocalDateTime d,
                                          java.time.LocalDateTime h,
                                          Pageable pageable) {
            buscarLlamado = true;
            return new PageImpl<>(List.of());
        }
    }

    // Helpers

    private static Button findButton(Component root, String text) {
        return flatten(root).stream()
                .filter(c -> c instanceof Button)
                .map(c -> (Button) c)
                .filter(b -> text.equals(b.getText()))
                .findFirst()
                .orElseThrow();
    }

    private static DatePicker findDatePicker(Component root, String label) {
        return flatten(root).stream()
                .filter(c -> c instanceof DatePicker)
                .map(c -> (DatePicker) c)
                .filter(dp -> label.equals(dp.getLabel()))
                .findFirst()
                .orElseThrow();
    }

    private static Grid<?> findGrid(Component root) {
        return flatten(root).stream()
                .filter(c -> c instanceof Grid)
                .map(c -> (Grid<?>) c)
                .findFirst()
                .orElse(null);
    }

    private static H3 findTitulo(Component root, String text) {
        return flatten(root).stream()
                .filter(c -> c instanceof H3)
                .map(c -> (H3) c)
                .filter(h -> text.equals(h.getText()))
                .findFirst()
                .orElse(null);
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        if (c == null) return out;
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }
}