package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.service.usuario.EmpleadoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditarEmpleadoViewTest {

    @Test
    void constructor_no_revienta_y_monta_componentes_basicos() {
        EmpleadoService empleadoService = mock(EmpleadoService.class);

        EditarEmpleadoView view = new EditarEmpleadoView(empleadoService);

        assertNotNull(view);

        assertNotNull(findH2ByText(view, "Editar empleado"));

        assertNotNull(findTextFieldByLabel(view, "Nombre completo"));
        assertNotNull(findTextFieldByLabel(view, "Usuario"));
        assertNotNull(findEmailFieldByLabel(view, "Email"));
        assertNotNull(findTextFieldByLabel(view, "Dirección"));
        assertNotNull(findTextFieldByLabel(view, "Teléfono"));

        assertNotNull(findComboBoxByLabel(view, "Rol"));
        assertNotNull(findCheckboxByLabel(view, "Empleado activo"));
        assertNotNull(findPasswordFieldByLabel(view, "Nueva contraseña (opcional)"));

        assertNotNull(findButtonByText(view, "Guardar"));
        assertNotNull(findButtonByText(view, "Cancelar"));
    }

    @Test
    void before_enter_con_id_carga_empleado_y_llama_servicio() {
        EmpleadoService empleadoService = mock(EmpleadoService.class);

        Empleado empleado = mock(Empleado.class);
        when(empleadoService.obtenerPorId(5L)).thenReturn(empleado);

        EditarEmpleadoView view = new EditarEmpleadoView(empleadoService);

        view.beforeEnter(beforeEnterEventConId("5"));

        verify(empleadoService, atLeastOnce()).obtenerPorId(5L);
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

    private static TextField findTextFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof TextField tf && label.equals(tf.getLabel())) {
                return tf;
            }
        }
        return null;
    }

    private static EmailField findEmailFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof EmailField ef && label.equals(ef.getLabel())) {
                return ef;
            }
        }
        return null;
    }

    private static PasswordField findPasswordFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof PasswordField pf && label.equals(pf.getLabel())) {
                return pf;
            }
        }
        return null;
    }

    private static Checkbox findCheckboxByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof Checkbox cb && label.equals(cb.getLabel())) {
                return cb;
            }
        }
        return null;
    }

    private static ComboBox<?> findComboBoxByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof ComboBox<?> cb && label.equals(cb.getLabel())) {
                return cb;
            }
        }
        return null;
    }

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                return b;
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

    private static BeforeEnterEvent beforeEnterEventConId(String id) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        RouteParameters params = new RouteParameters(new RouteParam("id", id));
        when(event.getRouteParameters()).thenReturn(params);
        return event;
    }
}