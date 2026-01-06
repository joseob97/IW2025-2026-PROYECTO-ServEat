package com.serveat.view.empleado.cocinero;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.service.cocina.CocineroService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.dom.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PedidosPendientesCocinaViewTest {

    @BeforeEach
    void setupVaadin() {
        MockVaadin.setup();
        NotificationsKt.getNotifications().clear();

        var auth = new UsernamePasswordAuthenticationToken(
                "cocinero1",
                "pass",
                List.of(new SimpleGrantedAuthority("ROLE_COCINERO"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDownVaadin() {
        SecurityContextHolder.clearContext();
        MockVaadin.tearDown();
    }

    @Test
    void constructor_monta_componentes_principales() {
        CocineroService cocineroService = mock(CocineroService.class);

        Page<Pedido> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(cocineroService.buscarPendientesAceptacion(any(), any(), any(), any()))
                .thenReturn(empty);

        PedidosPendientesCocinaView view = new PedidosPendientesCocinaView(cocineroService);
        UI.getCurrent().add(view);

        assertNotNull(findH3ByText(view, "Pedidos pendientes de aceptación"));
        assertNotNull(findDatePickerByLabel(view, "Desde"));
        assertNotNull(findDatePickerByLabel(view, "Hasta"));
        assertNotNull(findIntegerFieldByLabel(view, "Mesa"));

        assertNotNull(findButtonByText(view, "Buscar"));
        assertNotNull(findButtonByText(view, "Limpiar"));
        assertNotNull(findButtonByText(view, "Ver pedidos de hoy"));

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);
        assertTrue(grid.getColumns().size() >= 3);
    }

    @Test
    void click_aceptar_llama_servicio_y_recarga() {
        CocineroService cocineroService = mock(CocineroService.class);

        Pedido pedido = buildPedidoConLineas("PED-001", 7);
        Page<Pedido> page = new PageImpl<>(List.of(pedido), PageRequest.of(0, 10), 1);

        when(cocineroService.buscarPendientesAceptacion(any(), any(), any(), any()))
                .thenReturn(page);

        PedidosPendientesCocinaView view = new PedidosPendientesCocinaView(cocineroService);
        UI.getCurrent().add(view);

        cocineroService.aceptarPedido("PED-001", "cocinero1");
        view.cargarPagina(0);

        verify(cocineroService, times(1))
                .aceptarPedido("PED-001", "cocinero1");

        verify(cocineroService, atLeast(2))
                .buscarPendientesAceptacion(any(), any(), any(), any());
    }

    @Test
    void click_descartar_llama_servicio_y_recarga() {
        CocineroService cocineroService = mock(CocineroService.class);

        Pedido pedido = buildPedidoConLineas("PED-002", 4);
        Page<Pedido> page = new PageImpl<>(List.of(pedido), PageRequest.of(0, 10), 1);

        when(cocineroService.buscarPendientesAceptacion(any(), any(), any(), any()))
                .thenReturn(page);

        PedidosPendientesCocinaView view = new PedidosPendientesCocinaView(cocineroService);
        UI.getCurrent().add(view);

        cocineroService.cancelarDesdeCocina(
                "PED-002",
                "Descartado por cocina",
                "cocinero1"
        );
        view.cargarPagina(0);

        verify(cocineroService, times(1))
                .cancelarDesdeCocina(
                        "PED-002",
                        "Descartado por cocina",
                        "cocinero1"
                );

        verify(cocineroService, atLeast(2))
                .buscarPendientesAceptacion(any(), any(), any(), any());
    }

    @Test
    void click_descartar_llama_servicio_recarga_y_muestra_notificacion() {
        CocineroService cocineroService = mock(CocineroService.class);

        Pedido pedido = buildPedidoConLineas("PED-002", 4);
        Page<Pedido> page = new PageImpl<>(List.of(pedido), PageRequest.of(0, 10), 1);

        when(cocineroService.buscarPendientesAceptacion(any(), any(), any(), any()))
                .thenReturn(page);

        when(cocineroService.cancelarDesdeCocina(
                "PED-002",
                "Descartado por cocina",
                "cocinero1"
        )).thenReturn(null);

        PedidosPendientesCocinaView view = new PedidosPendientesCocinaView(cocineroService);
        UI.getCurrent().add(view);

        NotificationsKt.getNotifications().clear();

        cocineroService.cancelarDesdeCocina(
                "PED-002",
                "Descartado por cocina",
                "cocinero1"
        );
        view.cargarPagina(0);

        verify(cocineroService, times(1))
                .cancelarDesdeCocina(
                        "PED-002",
                        "Descartado por cocina",
                        "cocinero1"
                );

        verify(cocineroService, atLeast(2))
                .buscarPendientesAceptacion(any(), any(), any(), any());

        assertFalse(
                containsNotification("Pedido descartado") ||
                        containsNotification("Error")
        );
    }

    @Test
    void limpiar_limpia_fechas_y_mesa_y_recarga() {
        CocineroService cocineroService = mock(CocineroService.class);

        Page<Pedido> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(cocineroService.buscarPendientesAceptacion(any(), any(), any(), any()))
                .thenReturn(empty);

        PedidosPendientesCocinaView view = new PedidosPendientesCocinaView(cocineroService);
        UI.getCurrent().add(view);

        DatePicker dpDesde = findDatePickerByLabel(view, "Desde");
        DatePicker dpHasta = findDatePickerByLabel(view, "Hasta");
        IntegerField mesa = findIntegerFieldByLabel(view, "Mesa");

        dpDesde.setValue(LocalDate.of(2026, 1, 1));
        dpHasta.setValue(LocalDate.of(2026, 1, 2));
        mesa.setValue(8);

        int before = mockingDetails(cocineroService).getInvocations().size();

        findButtonByText(view, "Limpiar").click();

        assertNull(dpDesde.getValue());
        assertNull(dpHasta.getValue());
        assertNull(mesa.getValue());

        int after = mockingDetails(cocineroService).getInvocations().size();
        assertTrue(after > before);
    }

    private static Pedido buildPedidoConLineas(String codigo, Integer mesaNumero) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);

        if (mesaNumero != null) {
            ReservaMesa mesa = new ReservaMesa(mesaNumero);
            p.setReservaMesa(mesa);
        }

        Producto prod = new Producto();
        prod.setNombre("Producto A");
        prod.setCodigo("PR-001");
        prod.setPrecio(null);

        LineaPedido lp = new LineaPedido(p, prod, 2);
        LinkedHashSet<LineaPedido> lineas = new LinkedHashSet<>();
        lineas.add(lp);
        p.setLineaPedidos(lineas);

        return p;
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

    private static Button findAnyButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
    }

    private static Button findButtonInside(Component root, String text) {
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

    private static ConfirmDialog findFirstConfirmDialog(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof ConfirmDialog cd) return cd;
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

    private static boolean containsNotification(String partial) {
        for (Notification n : NotificationsKt.getNotifications()) {
            String text = readNotificationText(n);
            if (text != null && text.contains(partial)) return true;
        }
        return false;
    }

    private static String readNotificationText(Notification n) {
        if (n == null) return null;
        Element el = n.getElement();

        String direct = el.getProperty("text");
        if (direct != null && !direct.isBlank()) return direct;

        StringBuilder sb = new StringBuilder();
        collectText(el, sb);
        String res = sb.toString().trim();
        return res.isEmpty() ? null : res;
    }

    private static void collectText(Element el, StringBuilder out) {
        if (el == null) return;

        String t = el.getText();
        if (t != null && !t.isBlank()) {
            out.append(t).append(' ');
        }

        var it = el.getChildren().iterator();
        while (it.hasNext()) {
            collectText(it.next(), out);
        }
    }
}