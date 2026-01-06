package com.serveat.view.empleado.administrador.productos;

import com.serveat.domain.menu.Producto;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
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

class GestionProductosViewTest {

    @BeforeEach
    void setupUi() {
        UI ui = new UI();
        UI.setCurrent(ui);
    }

    @Test
    void constructor_no_revienta_y_refresca_grid_con_listado_inicial() {
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);

        when(productoService.listarProductos()).thenReturn(Collections.emptyList());

        GestionProductosView view = new GestionProductosView(productoService, categoriaService);
        UI.getCurrent().add(view);

        assertNotNull(view);

        verify(productoService, atLeastOnce()).listarProductos();

        assertNotNull(findH2ByText(view, "Gestión de productos"));
        assertNotNull(findTextFieldByLabel(view, "Buscar por nombre"));

        assertNotNull(findButtonByText(view, "➕ Nuevo"));
        assertNotNull(findButtonByText(view, "✏️ Editar"));
        assertNotNull(findButtonByText(view, "🗑 Eliminar"));

        assertNotNull(findFirstGrid(view));
    }

    @Test
    void refrescar_con_filtro_vacio_llama_listar_productos() throws Exception {
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);

        when(productoService.listarProductos()).thenReturn(Collections.emptyList());

        GestionProductosView view = new GestionProductosView(productoService, categoriaService);
        UI.getCurrent().add(view);

        TextField filtro = findTextFieldByLabel(view, "Buscar por nombre");
        assertNotNull(filtro);

        filtro.setValue("");

        invokePrivate(view, "refrescar");

        verify(productoService, atLeastOnce()).listarProductos();
        verify(productoService, never()).buscarPorNombreParcial(anyString());
    }

    @Test
    void refrescar_con_filtro_no_vacio_llama_buscar_por_nombre_parcial() throws Exception {
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);

        when(productoService.listarProductos()).thenReturn(Collections.emptyList());
        when(productoService.buscarPorNombreParcial("pizza")).thenReturn(Collections.emptyList());

        GestionProductosView view = new GestionProductosView(productoService, categoriaService);
        UI.getCurrent().add(view);

        TextField filtro = findTextFieldByLabel(view, "Buscar por nombre");
        assertNotNull(filtro);

        filtro.setValue("pizza");

        invokePrivate(view, "refrescar");

        verify(productoService, atLeastOnce()).buscarPorNombreParcial("pizza");
    }

    @Test
    void abrir_dialogo_nuevo_no_revienta_y_carga_listas_de_selector() throws Exception {
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);

        when(productoService.listarProductos()).thenReturn(Collections.emptyList());
        when(productoService.listarNombresIngredientes()).thenReturn(List.of("Tomate", "Queso"));
        when(categoriaService.listarCategorias()).thenReturn(Collections.emptyList());

        GestionProductosView view = new GestionProductosView(productoService, categoriaService);
        UI.getCurrent().add(view);

        invokePrivate(view, "abrirDialogo", new Class<?>[]{Producto.class}, new Object[]{null});

        verify(categoriaService, atLeastOnce()).listarCategorias();
        verify(productoService, atLeastOnce()).listarNombresIngredientes();

        Dialog dialog = findLastDialogInTree(UI.getCurrent());
        if (dialog != null) {
            MultiSelectComboBox<?> ingredientes = findMultiSelectComboBoxByLabel(dialog, "Ingredientes");
            assertNotNull(ingredientes);
        }
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

    private static MultiSelectComboBox<?> findMultiSelectComboBoxByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof MultiSelectComboBox<?> cb && label.equals(cb.getLabel())) return cb;
        }
        return null;
    }

    private static Dialog findLastDialogInTree(Component root) {
        Dialog last = null;
        for (Component c : flatten(root)) {
            if (c instanceof Dialog d) last = d;
        }
        return last;
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

    private static void invokePrivate(Object target, String methodName, Class<?>[] argTypes, Object[] args) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);
        m.invoke(target, args);
    }
}