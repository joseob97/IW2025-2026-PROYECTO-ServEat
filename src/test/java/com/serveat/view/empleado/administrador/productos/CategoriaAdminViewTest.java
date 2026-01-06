package com.serveat.view.empleado.administrador.productos;

import com.serveat.domain.menu.Categoria;
import com.serveat.service.menu.CategoriaService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CategoriaAdminViewTest {

    @BeforeEach
    void setupUi() {
        UI ui = new UI();
        UI.setCurrent(ui);
    }

    @Test
    void constructor_no_revienta_y_refresca_listado_inicial() {
        CategoriaService categoriaService = mock(CategoriaService.class);
        when(categoriaService.listarCategorias()).thenReturn(Collections.emptyList());

        CategoriaAdminView view = new CategoriaAdminView(categoriaService);
        UI.getCurrent().add(view);

        assertNotNull(view);

        verify(categoriaService, atLeastOnce()).listarCategorias();

        assertNotNull(findH2ByText(view, "Gestión de Categorías"));
        assertNotNull(findTextFieldByLabel(view, "Nombre de categoría"));
        assertNotNull(findButtonByText(view, "Crear"));

        Grid<?> grid = findFirstGrid(view);
        assertNotNull(grid);
        assertTrue(grid.getColumns().size() >= 1);
    }

    @Test
    void crear_categoria_con_nombre_en_blanco_no_llama_servicio() throws Exception {
        CategoriaService categoriaService = mock(CategoriaService.class);
        when(categoriaService.listarCategorias()).thenReturn(Collections.emptyList());

        CategoriaAdminView view = new CategoriaAdminView(categoriaService);
        UI.getCurrent().add(view);

        TextField nombre = findTextFieldByLabel(view, "Nombre de categoría");
        assertNotNull(nombre);

        nombre.setValue("   ");

        invokePrivate(view, "crearCategoria");

        verify(categoriaService, never()).crearCategoria(anyString());
        verify(categoriaService, atLeastOnce()).listarCategorias();
    }

    @Test
    void crear_categoria_con_nombre_valido_llama_servicio_limpia_campo_y_refresca() throws Exception {
        CategoriaService categoriaService = mock(CategoriaService.class);

        when(categoriaService.listarCategorias()).thenReturn(Collections.emptyList());

        Categoria c = mock(Categoria.class);
        when(c.getNombre()).thenReturn("Pizzas");
        when(categoriaService.listarCategorias()).thenReturn(List.of(c));

        CategoriaAdminView view = new CategoriaAdminView(categoriaService);
        UI.getCurrent().add(view);

        TextField nombre = findTextFieldByLabel(view, "Nombre de categoría");
        assertNotNull(nombre);

        nombre.setValue("Pizzas");

        invokePrivate(view, "crearCategoria");

        verify(categoriaService, times(1)).crearCategoria("Pizzas");
        verify(categoriaService, atLeast(2)).listarCategorias();

        assertEquals("", nombre.getValue());
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
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }

    private static void invokePrivate(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }
}