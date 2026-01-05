package com.serveat.view.empleado.administrador;

import com.serveat.domain.notificaciones.PushNotificacion;
import com.serveat.service.notificaciones.PushNotificacionService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionesViewTest {

    @Test
    void constructor_no_revienta_y_refresca_grid_en_constructor() {
        PushNotificacionService service = mock(PushNotificacionService.class);
        when(service.listarNotificaciones()).thenReturn(Collections.emptyList());

        NotificacionesView view = new NotificacionesView(service);

        assertNotNull(view);
        verify(service, atLeastOnce()).listarNotificaciones();

        assertNotNull(findH2ByText(view, "Notificaciones"));

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);

        assertTrue(grid.getColumns().size() >= 4);
    }

    @Test
    void refrescar_llama_service_listar_notificaciones() throws Exception {
        PushNotificacionService service = mock(PushNotificacionService.class);
        when(service.listarNotificaciones()).thenReturn(Collections.emptyList());

        NotificacionesView view = new NotificacionesView(service);

        invokePrivate(view, "refrescar");

        verify(service, atLeast(2)).listarNotificaciones();
    }

    @Test
    void con_notificacion_puede_crear_componentes_de_accion_ver_y_eliminar() {
        PushNotificacionService service = mock(PushNotificacionService.class);

        PushNotificacion n = mock(PushNotificacion.class);
        when(n.getId()).thenReturn(1L);
        when(n.getTitulo()).thenReturn("T");
        when(n.getMensaje()).thenReturn("M");

        when(service.listarNotificaciones()).thenReturn(List.of(n));

        NotificacionesView view = new NotificacionesView(service);
        assertNotNull(view);

        @SuppressWarnings("unchecked")
        Grid<PushNotificacion> grid = (Grid<PushNotificacion>) findFirstGrid(view);
        assertNotNull(grid);

        Function<PushNotificacion, Component> renderer = findFirstComponentColumnRenderer(grid);
        assertNotNull(renderer);

        Component acciones = renderer.apply(n);
        assertNotNull(acciones);

        Button ver = findButtonByText(acciones, "Ver");
        Button eliminar = findButtonByText(acciones, "Eliminar");

        assertNotNull(ver);
        assertNotNull(eliminar);

        ver.click();
        eliminar.click();

        verify(service, atLeastOnce()).eliminarNotificacion(1L);
        verify(service, atLeast(2)).listarNotificaciones();
    }

    private static H2 findH2ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H2 h2 && text.equals(h2.getText())) return h2;
        }
        return null;
    }

    private static Grid<?> findFirstGrid(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof Grid<?> g) return g;
        }
        return null;
    }

    private static <T> Function<T, Component> findFirstComponentColumnRenderer(Grid<T> grid) {
        for (Grid.Column<T> col : grid.getColumns()) {
            Function<T, Component> fn = tryExtractComponentRenderer(col);
            if (fn != null) return fn;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> Function<T, Component> tryExtractComponentRenderer(Grid.Column<T> col) {
        try {
            var f = col.getClass().getDeclaredField("renderer");
            f.setAccessible(true);
            Object renderer = f.get(col);
            if (renderer == null) return null;

            var m = renderer.getClass().getMethod("createComponent", Object.class);

            return (T item) -> {
                try {
                    return (Component) m.invoke(renderer, item);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (NoSuchFieldException nsfe) {
            try {
                var f2 = col.getClass().getDeclaredField("columnRenderer");
                f2.setAccessible(true);
                Object renderer = f2.get(col);
                if (renderer == null) return null;

                var m = renderer.getClass().getMethod("createComponent", Object.class);

                return (T item) -> {
                    try {
                        return (Component) m.invoke(renderer, item);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            } catch (Exception ignore) {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }

    private static void invokePrivate(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }
}