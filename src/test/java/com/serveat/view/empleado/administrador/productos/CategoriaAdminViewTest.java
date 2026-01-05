package com.serveat.view.empleado.administrador.productos;

import com.serveat.domain.menu.Categoria;
import com.serveat.service.menu.CategoriaService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoriaAdminViewTest {

    @Test
    void constructor_no_revienta_y_refresca_grid_en_constructor() {
        CategoriaService categoriaService = mock(CategoriaService.class);
        when(categoriaService.listarCategorias()).thenReturn(Collections.emptyList());

        CategoriaAdminView view = new CategoriaAdminView(categoriaService);

        assertNotNull(view);

        verify(categoriaService, atLeastOnce()).listarCategorias();

        assertNotNull(findH2ByText(view, "Gestión de Categorías"));
        assertNotNull(findTextFieldByLabel(view, "Nombre de categoría"));
        assertNotNull(findButtonByText(view, "Crear"));

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);
    }

    @Test
    void crear_categoria_con_nombre_valido_llama_servicio_limpia_campo_y_refresca() throws Exception {
        CategoriaService categoriaService = mock(CategoriaService.class);
        when(categoriaService.listarCategorias()).thenReturn(Collections.emptyList());

        CategoriaAdminView view = new CategoriaAdminView(categoriaService);

        TextField nombre = findTextFieldByLabel(view, "Nombre de categoría");
        assertNotNull(nombre);

        nombre.setValue("Bebidas");

        invokePrivate(view, "crearCategoria");

        verify(categoriaService, atLeastOnce()).crearCategoria("Bebidas");
        verify(categoriaService, atLeast(2)).listarCategorias();

        assertEquals("", nombre.getValue());
    }

    @Test
    void crear_categoria_con_nombre_en_blanco_no_llama_servicio() throws Exception {
        CategoriaService categoriaService = mock(CategoriaService.class);
        when(categoriaService.listarCategorias()).thenReturn(Collections.emptyList());

        CategoriaAdminView view = new CategoriaAdminView(categoriaService);

        TextField nombre = findTextFieldByLabel(view, "Nombre de categoría");
        assertNotNull(nombre);

        nombre.setValue("   ");

        invokePrivate(view, "crearCategoria");

        verify(categoriaService, never()).crearCategoria(anyString());
        verify(categoriaService, atLeastOnce()).listarCategorias();
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

    // Reflection solo para invocar crearCategoria

    private static void invokePrivate(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }
}