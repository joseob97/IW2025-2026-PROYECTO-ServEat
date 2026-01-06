package com.serveat.view.empleado.cocinero;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.NotificationsKt;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.dom.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DetalleComandaViewTest {

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
    void constructor_monta_componentes_principales() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        DetalleComandaView view = new DetalleComandaView(pedidoService, calculoService);
        UI.getCurrent().add(view);

        assertNotNull(findH3ByText(view, "Detalle de Comanda"));
        assertNotNull(findFirstGrid(view));
        assertNotNull(findSelectEstadoCocina(view));
        assertNotNull(findButtonByExactText(view, "✅ Confirmar cambio"));
    }

    @Test
    void set_parameter_con_id_invalido_muestra_notificacion_y_no_llama_servicio() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        DetalleComandaView view = new DetalleComandaView(pedidoService, calculoService);
        UI.getCurrent().add(view);

        NotificationsKt.getNotifications().clear();
        callSetParameterIgnoringNavigation(view, "no-es-uuid");

        assertTrue(containsNotification("ID de comanda inválido"));
        verify(pedidoService, never()).obtenerPedidoPorId(any());
    }

    @Test
    void set_parameter_con_pedido_null_muestra_notificacion() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        UUID id = UUID.randomUUID();
        when(pedidoService.obtenerPedidoPorId(id)).thenReturn(null);

        DetalleComandaView view = new DetalleComandaView(pedidoService, calculoService);
        UI.getCurrent().add(view);

        NotificationsKt.getNotifications().clear();
        callSetParameterIgnoringNavigation(view, id.toString());

        assertTrue(containsNotification("Comanda no encontrada"));
        verify(pedidoService, times(1)).obtenerPedidoPorId(id);
    }

    @Test
    void set_parameter_carga_pedido_y_refresca_chips_grid_y_select() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        UUID id = UUID.randomUUID();

        Pedido pedido = new Pedido();
        pedido.setCodigo("PED-001");
        pedido.setEstadoCocina(EstadoCocina.ACEPTADO);
        setField(pedido, "id", id);

        ReservaMesa mesa = new ReservaMesa(7);
        pedido.setReservaMesa(mesa);

        Producto prod = new Producto();
        prod.setNombre("Hamburguesa");
        prod.setPrecio(new BigDecimal("8.50"));
        prod.setCodigo("P-001");

        LineaPedido lp = new LineaPedido(pedido, prod, 2);
        LinkedHashSet<LineaPedido> lineas = new LinkedHashSet<>();
        lineas.add(lp);
        pedido.setLineaPedidos(lineas);

        when(pedidoService.obtenerPedidoPorId(id)).thenReturn(pedido);
        when(calculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("17.00"));
        when(calculoService.calcularPrecioLinea(any(LineaPedido.class))).thenReturn(new BigDecimal("17.00"));

        DetalleComandaView view = new DetalleComandaView(pedidoService, calculoService);
        UI.getCurrent().add(view);

        NotificationsKt.getNotifications().clear();
        callSetParameterIgnoringNavigation(view, id.toString());

        assertNotNull(findSpanContainingText(view, "Mesa: 7"));
        assertNotNull(findSpanContainingText(view, "Código: PED-001"));
        assertNotNull(findSpanContainingText(view, "Estado: ACEPTADO"));
        assertNotNull(findSpanContainingText(view, "Total: 17.00"));

        Select<EstadoCocina> select = findSelectEstadoCocina(view);
        assertNotNull(select);
        assertEquals(EstadoCocina.ACEPTADO, select.getValue());

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);
        assertEquals(1, grid.getDataProvider().size(new com.vaadin.flow.data.provider.Query<>()));
    }

    @Test
    void confirmar_cambio_con_mismo_estado_no_llama_servicio() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        UUID id = UUID.randomUUID();

        Pedido pedido = new Pedido();
        pedido.setCodigo("PED-002");
        pedido.setEstadoCocina(EstadoCocina.EN_PREPARACION);
        setField(pedido, "id", id);

        when(pedidoService.obtenerPedidoPorId(id)).thenReturn(pedido);
        when(calculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("10.00"));

        DetalleComandaView view = new DetalleComandaView(pedidoService, calculoService);
        UI.getCurrent().add(view);

        callSetParameterIgnoringNavigation(view, id.toString());
        NotificationsKt.getNotifications().clear();

        Select<EstadoCocina> select = findSelectEstadoCocina(view);
        assertNotNull(select);
        select.setValue(EstadoCocina.EN_PREPARACION);

        Button confirmar = findButtonByExactText(view, "✅ Confirmar cambio");
        assertNotNull(confirmar);
        confirmar.click();

        verify(pedidoService, never()).cambiarEstadoCocina(any(), any());
        assertTrue(containsNotification("El estado ya es el mismo"));
    }

    @Test
    void confirmar_cambio_con_estado_distinto_llama_servicio_y_refresca() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        UUID id = UUID.randomUUID();

        Pedido pedidoInicial = new Pedido();
        pedidoInicial.setCodigo("PED-003");
        pedidoInicial.setEstadoCocina(EstadoCocina.ACEPTADO);
        setField(pedidoInicial, "id", id);

        Pedido pedidoActualizado = new Pedido();
        pedidoActualizado.setCodigo("PED-003");
        pedidoActualizado.setEstadoCocina(EstadoCocina.LISTO);
        setField(pedidoActualizado, "id", id);

        when(pedidoService.obtenerPedidoPorId(id)).thenReturn(pedidoInicial);
        when(pedidoService.cambiarEstadoCocina(id, EstadoCocina.LISTO)).thenReturn(pedidoActualizado);

        when(calculoService.calcularTotalPedido(pedidoInicial)).thenReturn(new BigDecimal("10.00"));
        when(calculoService.calcularTotalPedido(pedidoActualizado)).thenReturn(new BigDecimal("10.00"));

        DetalleComandaView view = new DetalleComandaView(pedidoService, calculoService);
        UI.getCurrent().add(view);

        callSetParameterIgnoringNavigation(view, id.toString());
        NotificationsKt.getNotifications().clear();

        Select<EstadoCocina> select = findSelectEstadoCocina(view);
        assertNotNull(select);
        select.setValue(EstadoCocina.LISTO);

        Button confirmar = findButtonByExactText(view, "✅ Confirmar cambio");
        assertNotNull(confirmar);
        confirmar.click();

        verify(pedidoService, times(1)).cambiarEstadoCocina(id, EstadoCocina.LISTO);

        Select<EstadoCocina> selectAfter = findSelectEstadoCocina(view);
        assertEquals(EstadoCocina.LISTO, selectAfter.getValue());

        assertTrue(containsNotification("Estado actualizado"));
    }

    @Test
    void confirmar_cambio_si_el_servicio_lanza_excepcion_muestra_notificacion() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);

        UUID id = UUID.randomUUID();

        Pedido pedidoInicial = new Pedido();
        pedidoInicial.setCodigo("PED-004");
        pedidoInicial.setEstadoCocina(EstadoCocina.ACEPTADO);
        setField(pedidoInicial, "id", id);

        when(pedidoService.obtenerPedidoPorId(id)).thenReturn(pedidoInicial);
        when(calculoService.calcularTotalPedido(pedidoInicial)).thenReturn(new BigDecimal("10.00"));
        when(pedidoService.cambiarEstadoCocina(id, EstadoCocina.CANCELADO)).thenThrow(new RuntimeException("boom"));

        DetalleComandaView view = new DetalleComandaView(pedidoService, calculoService);
        UI.getCurrent().add(view);

        callSetParameterIgnoringNavigation(view, id.toString());
        NotificationsKt.getNotifications().clear();

        Select<EstadoCocina> select = findSelectEstadoCocina(view);
        assertNotNull(select);
        select.setValue(EstadoCocina.CANCELADO);

        Button confirmar = findButtonByExactText(view, "✅ Confirmar cambio");
        assertNotNull(confirmar);
        confirmar.click();

        assertTrue(containsNotification("Error: boom"));
    }

    private static void callSetParameterIgnoringNavigation(DetalleComandaView view, String param) {
        try {
            view.setParameter(null, param);
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if (msg == null || !msg.contains("No route found for the given navigation target")) {
                throw e;
            }
        }
    }

    private static H3 findH3ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H3 h3 && text.equals(h3.getText())) return h3;
        }
        return null;
    }

    private static Button findButtonByExactText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
    }

    private static Grid<?> findFirstGrid(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof Grid<?> g) return g;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Select<EstadoCocina> findSelectEstadoCocina(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof Select<?> s) {
                Object v = s.getValue();
                if (v == null || v instanceof EstadoCocina) return (Select<EstadoCocina>) s;
            }
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
        if (t != null && !t.isBlank()) out.append(t).append(' ');

        Iterator<Element> it = el.getChildren().iterator();
        while (it.hasNext()) collectText(it.next(), out);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            fail("No se pudo fijar el campo por reflexion: " + fieldName + ". " + e.getMessage());
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> cur = type;
        while (cur != null) {
            try {
                return cur.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}