package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.service.usuario.EmpleadoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionEmpleadosViewTest {

    @Test
    void constructor_no_revienta_y_carga_empleados_y_monta_componentes_basicos() {
        EmpleadoService empleadoService = mock(EmpleadoService.class);
        when(empleadoService.obtenerTodos()).thenReturn(List.of());

        GestionEmpleadosView view = new GestionEmpleadosView(empleadoService);

        assertNotNull(view);

        verify(empleadoService, atLeastOnce()).obtenerTodos();

        assertNotNull(findH2ByText(view, "Gestión de empleados"));
        assertNotNull(findFirstGrid(view));

        TextField buscador = findTextFieldByPlaceholder(view, "Buscar por nombre, usuario, email o teléfono");
        assertNotNull(buscador);

        ComboBox<String> filtroEstado = findComboBoxByItems(view, List.of("Todos", "Activos", "Inactivos"));
        ComboBox<String> filtroRol = findComboBoxByItems(view, List.of("Todos", "ADMIN", "CAMARERO", "COCINERO", "REPARTIDOR"));

        assertNotNull(filtroEstado);
        assertNotNull(filtroRol);
    }

    @Test
    void aplicar_filtros_no_revienta_con_empleado_minimo() throws Exception {
        EmpleadoService empleadoService = mock(EmpleadoService.class);

        Empleado e = mock(Empleado.class);
        when(e.getNombre()).thenReturn("Ana");
        when(e.getUsername()).thenReturn("ana");
        when(e.getEmail()).thenReturn("ana@x.com");
        when(e.getTelefono()).thenReturn(null);
        when(e.isEnabled()).thenReturn(true);
        when(e.getRol()).thenReturn("ADMIN");

        when(empleadoService.obtenerTodos()).thenReturn(List.of(e));

        GestionEmpleadosView view = new GestionEmpleadosView(empleadoService);

        TextField buscador = findTextFieldByPlaceholder(view, "Buscar por nombre, usuario, email o teléfono");
        assertNotNull(buscador);
        buscador.setValue("ana");

        ComboBox<String> filtroEstado = findComboBoxByItems(view, List.of("Todos", "Activos", "Inactivos"));
        ComboBox<String> filtroRol = findComboBoxByItems(view, List.of("Todos", "ADMIN", "CAMARERO", "COCINERO", "REPARTIDOR"));

        assertNotNull(filtroEstado);
        assertNotNull(filtroRol);

        filtroEstado.setValue("Todos");
        filtroRol.setValue("Todos");

        invokePrivate(view, "aplicarFiltros");

        assertNotNull(view);
    }

    // Helpers

    private static H2 findH2ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H2 h2 && text.equals(h2.getText())) {
                return h2;
            }
        }
        return null;
    }

    private static Grid<?> findFirstGrid(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof Grid<?> g) {
                return g;
            }
        }
        return null;
    }

    private static TextField findTextFieldByPlaceholder(Component root, String placeholder) {
        for (Component c : flatten(root)) {
            if (c instanceof TextField tf && placeholder.equals(tf.getPlaceholder())) {
                return tf;
            }
        }
        return null;
    }

    private static ComboBox<String> findComboBoxByItems(Component root, List<String> expectedItems) {
        for (Component c : flatten(root)) {
            if (c instanceof ComboBox<?> cb) {
                @SuppressWarnings("unchecked")
                ComboBox<String> combo = (ComboBox<String>) cb;

                List<String> items = combo.getListDataView().getItems().toList();
                if (items.containsAll(expectedItems)) {
                    return combo;
                }
            }
        }
        return null;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }

    // Reflection solo para invocar métodos private

    private static void invokePrivate(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }
}