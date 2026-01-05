package com.serveat.view.empleado.administrador;

import com.serveat.domain.menu.Menu;
import com.serveat.domain.seguridad.Feature;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.service.menu.MenuService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestionMenusViewTest {

    @Test
    void constructor_no_revienta_y_con_feature_desactivada_muestra_textos_y_no_consulta_repos_ni_service() {
        MenuService menuService = mock(MenuService.class);
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(false);

        GestionMenusView view = new GestionMenusView(menuService, productoRepository, featureService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.MENUS_OFERTAS);
        verifyNoInteractions(menuService);
        verifyNoInteractions(productoRepository);

        assertNotNull(findH2ByText(view, "Gestión de menús y ofertas"));
        assertNotNull(findParagraphContainingAny(view, "plan premium", "plan", "premium"));
        assertNotNull(findParagraphContainingAny(view, "no verán menús ni ofertas", "no verán", "menús ni ofertas"));

        assertNull(findButtonByText(view, "Crear menú"));
        assertNull(findH3ByText(view, "Menús creados"));
    }

    @Test
    void constructor_no_revienta_y_con_feature_activada_monta_formulario_y_carga_listado() {
        MenuService menuService = mock(MenuService.class);
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(true);
        when(productoRepository.findAll()).thenReturn(Collections.emptyList());
        when(menuService.obtenerMenusActivos()).thenReturn(Collections.emptyList());

        GestionMenusView view = new GestionMenusView(menuService, productoRepository, featureService);

        assertNotNull(view);

        verify(featureService, atLeastOnce()).tieneFeature(Feature.MENUS_OFERTAS);
        verify(productoRepository, atLeastOnce()).findAll();
        verify(menuService, atLeastOnce()).obtenerMenusActivos();

        assertNotNull(findH2ByText(view, "Gestión de menús y ofertas"));
        assertNotNull(findH3ByText(view, "Menús creados"));
        assertNotNull(findFormLayout(view));

        assertNotNull(findButtonByText(view, "Crear menú"));
        assertNotNull(findParagraphContainingAny(view, "Aún no hay menús creados", "no hay menús", "menús creados"));
    }

    @Test
    void cargar_listado_menus_con_datos_crea_boton_eliminar_por_menu() throws Exception {
        MenuService menuService = mock(MenuService.class);
        ProductoRepository productoRepository = mock(ProductoRepository.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(Feature.MENUS_OFERTAS)).thenReturn(true);
        when(productoRepository.findAll()).thenReturn(Collections.emptyList());

        Menu m = mock(Menu.class);
        when(m.getNombre()).thenReturn("Menu 1");
        when(m.getPrecioFijo()).thenReturn(new BigDecimal("9.99"));

        when(menuService.obtenerMenusActivos()).thenReturn(List.of(m));

        GestionMenusView view = new GestionMenusView(menuService, productoRepository, featureService);

        int eliminarAntes = countButtonsByText(view, "Eliminar");

        invokePrivate(view, "cargarListadoMenus");

        int eliminarDespues = countButtonsByText(view, "Eliminar");

        assertTrue(eliminarDespues >= 1);
        assertTrue(eliminarDespues >= eliminarAntes);

        verify(menuService, atLeast(2)).obtenerMenusActivos();
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

    private static H3 findH3ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H3 h3 && text.equals(h3.getText())) {
                return h3;
            }
        }
        return null;
    }

    private static Paragraph findParagraphContainingAny(Component root, String... partials) {
        for (Component c : flatten(root)) {
            if (c instanceof Paragraph p) {
                String t = p.getText();
                if (t == null) continue;
                for (String partial : partials) {
                    if (partial != null && !partial.isBlank() && t.toLowerCase().contains(partial.toLowerCase())) {
                        return p;
                    }
                }
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

    private static int countButtonsByText(Component root, String text) {
        int n = 0;
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                n++;
            }
        }
        return n;
    }

    private static FormLayout findFormLayout(Component root) {
        for (Component c : flatten(root)) {
            if (c instanceof FormLayout fl) {
                return fl;
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