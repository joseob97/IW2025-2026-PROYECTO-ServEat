package com.serveat.view.empleado.administrador.productos;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.service.menu.IngredienteService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionIngredientesViewTest {

    @Test
    void constructor_no_revienta_y_refresca_grid_con_filtro_inicial() {
        IngredienteService ingredienteService = mock(IngredienteService.class);
        when(ingredienteService.buscarPorNombre(any())).thenReturn(Collections.emptyList());

        GestionIngredientesView view = new GestionIngredientesView(ingredienteService);

        assertNotNull(view);

        verify(ingredienteService, atLeastOnce()).buscarPorNombre("");

        assertNotNull(findH2ByText(view, "Gestión de ingredientes"));
        assertNotNull(findTextFieldByLabel(view, "Buscar por nombre"));

        assertNotNull(findButtonByText(view, "➕ Nuevo"));
        assertNotNull(findButtonByText(view, "✏️ Editar"));
        assertNotNull(findButtonByText(view, "🗑 Eliminar"));

        assertNotNull(findFirstGrid(view));
    }

    @Test
    void refrescar_usa_valor_actual_del_filtro_y_llama_servicio() throws Exception {
        IngredienteService ingredienteService = mock(IngredienteService.class);
        when(ingredienteService.buscarPorNombre(any())).thenReturn(Collections.emptyList());

        GestionIngredientesView view = new GestionIngredientesView(ingredienteService);

        TextField filtro = findTextFieldByLabel(view, "Buscar por nombre");
        assertNotNull(filtro);

        filtro.setValue("tomate");

        invokePrivate(view, "refrescar");

        verify(ingredienteService, atLeastOnce()).buscarPorNombre("tomate");
    }

    @Test
    void abrir_dialogo_no_revienta() throws Exception {
        IngredienteService ingredienteService = mock(IngredienteService.class);
        when(ingredienteService.buscarPorNombre(any())).thenReturn(Collections.emptyList());

        GestionIngredientesView view = new GestionIngredientesView(ingredienteService);

        // No se puede contar Dialog con getChildren(), Dialog no cuelga del árbol del view
        // Se valida que la llamada no lanza excepción
        assertDoesNotThrow(() ->
                invokePrivate(view, "abrirDialogo", new Class<?>[]{Ingrediente.class}, new Object[]{null})
        );
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

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                return b;
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

    private static void invokePrivate(Object target, String methodName, Class<?>[] argTypes, Object[] args) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);
        m.invoke(target, args);
    }
}