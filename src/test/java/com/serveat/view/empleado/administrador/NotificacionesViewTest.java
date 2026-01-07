package com.serveat.view.empleado.administrador;

import com.serveat.domain.notificaciones.PushNotificacion;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.notificaciones.PushNotificacionService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificacionesViewTest {

    @BeforeEach
    void setupUi() {
        UI ui = new UI();
        UI.setCurrent(ui);
    }

    @Test
    void constructor_no_revienta_y_refresca_grid_en_constructor() {
        PushNotificacionService service = mock(PushNotificacionService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.NOTIFICACIONES)).thenReturn(true);
        when(service.listarNotificaciones()).thenReturn(Collections.emptyList());

        NotificacionesView view = new NotificacionesView(service, featureService);
        UI.getCurrent().add(view);

        assertNotNull(view);

        assertNotNull(findH2ByText(view, "Notificaciones"));
        assertNotNull(findFirstGrid(view));
    }

    @Test
    void con_notificacion_puede_crear_componentes_de_accion_ver_y_eliminar_y_eliminar_llama_servicio() {
        PushNotificacionService service = mock(PushNotificacionService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.NOTIFICACIONES)).thenReturn(true);

        PushNotificacion n = mock(PushNotificacion.class);
        when(n.getId()).thenReturn(1L);
        when(n.getTitulo()).thenReturn("T");
        when(n.getMensaje()).thenReturn("M");

        when(service.listarNotificaciones()).thenReturn(List.of(n));

        NotificacionesView view = new NotificacionesView(service, featureService);
        UI.getCurrent().add(view);

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

        dispararClickVaadin(eliminar);

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

    private static Button findButtonByText(Component root, String text) {
        if (root == null) return null;
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        if (c == null) return out;
        out.add(c);
        c.getChildren().forEach(ch -> out.addAll(flatten(ch)));
        return out;
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
            Field f = col.getClass().getDeclaredField("renderer");
            f.setAccessible(true);
            Object renderer = f.get(col);
            if (renderer == null) return null;

            Method m = renderer.getClass().getMethod("createComponent", Object.class);

            return (T item) -> {
                try {
                    return (Component) m.invoke(renderer, item);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Exception e) {
            return null;
        }
    }

    private static void dispararClickVaadin(Button button) {
        try {
            Object event = crearClickEvent(button);

            Object eventBus = obtenerEventBus(button);
            if (eventBus == null) {
                fail("No se pudo acceder al eventBus del componente");
                return;
            }

            Method fireEvent = null;
            for (Method m : eventBus.getClass().getMethods()) {
                if ("fireEvent".equals(m.getName()) && m.getParameterCount() == 1) {
                    fireEvent = m;
                    break;
                }
            }
            if (fireEvent == null) {
                fail("No se encontró método fireEvent en el eventBus");
                return;
            }

            fireEvent.invoke(eventBus, event);

        } catch (AssertionError ae) {
            throw ae;
        } catch (Exception e) {
            throw new RuntimeException("No se pudo disparar el click del botón en test", e);
        }
    }

    private static Object obtenerEventBus(Component component) throws Exception {
        Class<?> cls = component.getClass();
        while (cls != null) {
            try {
                Field f = cls.getDeclaredField("eventBus");
                f.setAccessible(true);
                return f.get(component);
            } catch (NoSuchFieldException ignore) {
                cls = cls.getSuperclass();
            }
        }
        return null;
    }

    private static Object crearClickEvent(Button button) throws Exception {
        Class<?> clickEventClass = Class.forName("com.vaadin.flow.component.ClickEvent");
        Constructor<?>[] ctors = clickEventClass.getConstructors();

        for (Constructor<?> ctor : ctors) {
            Class<?>[] p = ctor.getParameterTypes();
            if (p.length < 2) continue;
            if (!Component.class.isAssignableFrom(p[0])) continue;
            if (p[1] != boolean.class && p[1] != Boolean.class) continue;

            Object[] args = new Object[p.length];
            args[0] = button;
            args[1] = false;

            for (int i = 2; i < p.length; i++) {
                Class<?> t = p[i];
                if (t == int.class || t == Integer.class) args[i] = 0;
                else if (t == long.class || t == Long.class) args[i] = 0L;
                else if (t == double.class || t == Double.class) args[i] = 0d;
                else if (t == float.class || t == Float.class) args[i] = 0f;
                else if (t == boolean.class || t == Boolean.class) args[i] = false;
                else args[i] = null;
            }

            return ctor.newInstance(args);
        }

        fail("No se encontró un constructor compatible de ClickEvent para esta versión de Vaadin");
        return null;
    }
}