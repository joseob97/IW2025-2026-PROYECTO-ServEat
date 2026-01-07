package com.serveat.view.empleado.administrador.productos;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.menu.IngredienteService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionIngredientesViewTest {

    @BeforeEach
    void setupUi() {
        UI ui = new UI();
        UI.setCurrent(ui);
    }

    @Test
    void constructor_no_revienta_y_refresca_grid_con_filtro_inicial() {
        IngredienteService ingredienteService = mock(IngredienteService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(true);
        when(ingredienteService.buscarPorNombre(any())).thenReturn(Collections.emptyList());

        GestionIngredientesView view =
                new GestionIngredientesView(ingredienteService, featureService);

        UI.getCurrent().add(view);

        assertNotNull(view);

        verify(featureService).tieneFeature(Feature.INGREDIENTES);
        verify(ingredienteService, atLeastOnce()).buscarPorNombre("");

        assertNotNull(findH2ByText(view, "Gestión de ingredientes"));
        assertNotNull(findTextFieldByLabel(view, "Buscar por nombre"));

        assertNotNull(findButtonByText(view, "➕ Nuevo"));
        assertNotNull(findButtonByText(view, "✏️ Editar"));
        assertNotNull(findButtonByText(view, "🗑 Eliminar"));

        assertNotNull(findFirstGrid(view));
    }

    @Test
    void abrir_dialogo_no_revienta() throws Exception {
        IngredienteService ingredienteService = mock(IngredienteService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(true);
        when(ingredienteService.buscarPorNombre(any())).thenReturn(Collections.emptyList());

        GestionIngredientesView view =
                new GestionIngredientesView(ingredienteService, featureService);

        UI.getCurrent().add(view);

        assertDoesNotThrow(() ->
                invokePrivate(
                        view,
                        "abrirDialogo",
                        new Class<?>[]{Ingrediente.class},
                        new Object[]{null}
                )
        );
    }


    private static H2 findH2ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H2 h2 && text.equals(h2.getText())) return h2;
        }
        return null;
    }

    private static TextField findTextFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof TextField tf && label.equals(tf.getLabel())) return tf;
        }
        return null;
    }

    private static Button findButtonByText(Component root, String text) {
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

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        if (c == null) return out;
        out.add(c);
        c.getChildren().forEach(ch -> out.addAll(flatten(ch)));
        return out;
    }

    private static void invokePrivate(
            Object target,
            String methodName,
            Class<?>[] argTypes,
            Object[] args
    ) throws Exception {

        var m = target.getClass().getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);

        try {
            m.invoke(target, args);
        } catch (java.lang.reflect.InvocationTargetException ite) {
            if (ite.getCause() != null) {
                throw new RuntimeException(ite.getCause());
            }
            throw ite;
        }
    }
}