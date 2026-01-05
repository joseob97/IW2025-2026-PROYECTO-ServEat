package com.serveat.view.empleado.administrador;

import com.serveat.domain.establecimiento.DatosLocal;
import com.serveat.service.establecimiento.DatosLocalService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionDatosLocalViewTest {

    @Test
    void constructor_no_revienta_y_carga_datos_iniciales_desde_service() {
        DatosLocalService datosLocalService = mock(DatosLocalService.class);

        DatosLocal datos = new DatosLocal();
        when(datosLocalService.obtenerDatos()).thenReturn(datos);

        GestionDatosLocalView view = new GestionDatosLocalView(datosLocalService);

        assertNotNull(view);

        verify(datosLocalService, atLeastOnce()).obtenerDatos();

        assertNotNull(findH2ByText(view, "Gestión de la información del local"));
        assertNotNull(findFormLayout(view));

        assertNotNull(findTextFieldByLabel(view, "Nombre del local"));
        assertNotNull(findTextAreaByLabel(view, "Descripción principal"));
        assertNotNull(findTextAreaByLabel(view, "Descripción secundaria"));
        assertNotNull(findTextFieldByLabel(view, "Horario"));
        assertNotNull(findTextFieldByLabel(view, "Teléfono"));
        assertNotNull(findEmailFieldByLabel(view, "Email"));
        assertNotNull(findTextFieldByLabel(view, "Dirección"));

        assertNotNull(findButtonByText(view, "Guardar cambios"));
    }

    @Test
    void guardar_llama_service_guardar_cuando_binder_writeBean_no_lanza_excepcion() throws Exception {
        DatosLocalService datosLocalService = mock(DatosLocalService.class);

        // Bean real para que binder.writeBean(datosLocal) no reviente por proxies/mock
        DatosLocal datos = new DatosLocal();
        when(datosLocalService.obtenerDatos()).thenReturn(datos);

        GestionDatosLocalView view = new GestionDatosLocalView(datosLocalService);

        setText(findTextFieldByLabel(view, "Nombre del local"), "Mi local");
        setText(findTextAreaByLabel(view, "Descripción principal"), "Desc 1");
        setText(findTextAreaByLabel(view, "Descripción secundaria"), "Desc 2");
        setText(findTextFieldByLabel(view, "Horario"), "10:00-22:00");
        setText(findTextFieldByLabel(view, "Teléfono"), "123456789");
        setText(findEmailFieldByLabel(view, "Email"), "a@b.com");
        setText(findTextFieldByLabel(view, "Dirección"), "Calle 1");

        // Invocamos el private guardar() directamente (ya con campos rellenos)
        invokePrivate(view, "guardar");

        verify(datosLocalService, atLeastOnce()).guardar(any(DatosLocal.class));
    }

    // Helpers

    private static void setText(TextField tf, String v) {
        assertNotNull(tf);
        tf.setValue(v);
    }

    private static void setText(TextArea ta, String v) {
        assertNotNull(ta);
        ta.setValue(v);
    }

    private static void setText(EmailField ef, String v) {
        assertNotNull(ef);
        ef.setValue(v);
    }

    private static H2 findH2ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H2 h2 && text.equals(h2.getText())) {
                return h2;
            }
        }
        return null;
    }

    private static FormLayout findFormLayout(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof FormLayout fl) {
                return fl;
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

    private static TextField findTextFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof TextField tf && label.equals(tf.getLabel())) {
                return tf;
            }
        }
        return null;
    }

    private static TextArea findTextAreaByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof TextArea ta && label.equals(ta.getLabel())) {
                return ta;
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