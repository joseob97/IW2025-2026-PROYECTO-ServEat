package com.serveat.view.empleado.administrador;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.serveat.domain.establecimiento.DatosLocal;
import com.serveat.service.establecimiento.DatosLocalService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionDatosLocalViewTest {

    @BeforeEach
    void setupVaadin() {
        MockVaadin.setup();
    }

    @AfterEach
    void tearDownVaadin() {
        MockVaadin.tearDown();
    }

    @Test
    void constructor_no_revienta_y_carga_datos_iniciales_desde_service() {
        DatosLocalService datosLocalService = mock(DatosLocalService.class);

        DatosLocal datos = new DatosLocal();
        when(datosLocalService.obtenerDatos()).thenReturn(datos);

        GestionDatosLocalView view = new GestionDatosLocalView(datosLocalService);
        UI.getCurrent().add(view);

        assertNotNull(findH2ByText(view, "Gestión de la información del local"));
        assertNotNull(findFormLayout(view));
        assertNotNull(findButtonByText(view, "Guardar cambios"));

        verify(datosLocalService, atLeastOnce()).obtenerDatos();
    }

    @Test
    void guardar_no_revienta_y_si_binder_escribe_llama_service_guardar() throws Exception {
        DatosLocalService datosLocalService = mock(DatosLocalService.class);

        DatosLocal datos = new DatosLocal();
        when(datosLocalService.obtenerDatos()).thenReturn(datos);

        GestionDatosLocalView view = new GestionDatosLocalView(datosLocalService);
        UI.getCurrent().add(view);

        setTextFieldValue(view, "Nombre del local", "Mi Local");
        setTextFieldValue(view, "Horario", "L-V 10:00-22:00");
        setTextFieldValue(view, "Teléfono", "600123123");
        setEmailFieldValue(view, "Email", "local@x.com");
        setTextFieldValue(view, "Dirección", "Calle Falsa 123");
        setTextAreaValue(view, "Descripción principal", "Desc");
        setTextAreaValue(view, "Descripción secundaria", "Desc2");

        invokePrivateNoArgs(view, "guardar");

        verify(datosLocalService, atLeastOnce()).guardar(any(DatosLocal.class));
    }

    // Helpers

    private static void setTextFieldValue(Component root, String label, String value) {
        TextField tf = findTextFieldByLabel(root, label);
        assertNotNull(tf, "No se encontró TextField con label: " + label);
        tf.setValue(value);
    }

    private static void setEmailFieldValue(Component root, String label, String value) {
        EmailField ef = findEmailFieldByLabel(root, label);
        assertNotNull(ef, "No se encontró EmailField con label: " + label);
        ef.setValue(value);
    }

    private static void setTextAreaValue(Component root, String label, String value) {
        TextArea ta = findTextAreaByLabel(root, label);
        assertNotNull(ta, "No se encontró TextArea con label: " + label);
        ta.setValue(value);
    }

    private static H2 findH2ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H2 h2 && text.equals(h2.getText())) return h2;
        }
        return null;
    }

    private static FormLayout findFormLayout(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof FormLayout fl) return fl;
        }
        return null;
    }

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
    }

    private static TextField findTextFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof TextField tf && label.equals(tf.getLabel())) return tf;
        }
        return null;
    }

    private static TextArea findTextAreaByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof TextArea ta && label.equals(ta.getLabel())) return ta;
        }
        return null;
    }

    private static EmailField findEmailFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof EmailField ef && label.equals(ef.getLabel())) return ef;
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

    private static void invokePrivateNoArgs(Object target, String methodName) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        try {
            m.invoke(target);
        } catch (Exception e) {
            // si por dentro lanza excepción, queremos ver la causa real en el test
            if (e.getCause() != null) throw (e.getCause() instanceof Exception ex) ? ex : e;
            throw e;
        }
    }
}