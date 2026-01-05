package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Cliente;
import com.serveat.service.usuario.ClienteService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditarClienteViewTest {

    @Test
    void constructor_no_revienta_y_monta_componentes_basicos() {
        ClienteService clienteService = mock(ClienteService.class);

        EditarClienteView view = new EditarClienteView(clienteService);

        assertNotNull(view);

        assertNotNull(findH2ByText(view, "Editar cliente"));

        assertNotNull(findTextFieldByLabel(view, "Nombre"));
        assertNotNull(findTextFieldByLabel(view, "Usuario"));
        assertNotNull(findEmailFieldByLabel(view, "Email"));
        assertNotNull(findPasswordFieldByLabel(view, "Nueva contraseña"));
        assertNotNull(findTextFieldByLabel(view, "Teléfono"));
        assertNotNull(findTextFieldByLabel(view, "Dirección"));
        assertNotNull(findCheckboxByLabel(view, "Cliente activo"));

        assertNotNull(findButtonByText(view, "Guardar"));
        assertNotNull(findButtonByText(view, "Cancelar"));
    }

    @Test
    void before_enter_con_id_carga_cliente_y_llama_servicio() {
        ClienteService clienteService = mock(ClienteService.class);

        Cliente cliente = mock(Cliente.class);
        when(clienteService.obtenerPorId(1L)).thenReturn(cliente);

        EditarClienteView view = new EditarClienteView(clienteService);

        view.beforeEnter(beforeEnterEventConId("1"));

        verify(clienteService, atLeastOnce()).obtenerPorId(1L);
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

    private static com.vaadin.flow.router.BeforeEnterEvent beforeEnterEventConId(String id) {
        com.vaadin.flow.router.BeforeEnterEvent event = mock(com.vaadin.flow.router.BeforeEnterEvent.class);

        com.vaadin.flow.router.RouteParameters params =
                new com.vaadin.flow.router.RouteParameters(
                        new com.vaadin.flow.router.RouteParam("id", id)
                );

        when(event.getRouteParameters()).thenReturn(params);

        return event;
    }
}