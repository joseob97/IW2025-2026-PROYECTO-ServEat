package com.serveat.view.empleado.repartidor;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PanelRepartidorViewTest {

    @BeforeEach
    void setup() {
        MockVaadin.setup();

        // Registrar rutas reales usadas por RouterLink
        RouteConfiguration.forSessionScope().setRoute(
                "empleado/repartidor",
                PanelRepartidorView.class,
                MainLayout.class
        );

        RouteConfiguration.forSessionScope().setRoute(
                "empleado/repartidor/pedidos-disponibles",
                PedidosDisponiblesView.class,
                MainLayout.class
        );

        RouteConfiguration.forSessionScope().setRoute(
                "empleado/repartidor/mis-repartos",
                MisRepartosView.class,
                MainLayout.class
        );
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void constructor_monta_lo_basico() {
        PanelRepartidorView view = new PanelRepartidorView();

        assertNotNull(findH2ByText(view, "Repartos"));
        assertNotNull(findSpanByText(view,
                "Gestiona pedidos a domicilio listos para repartir y tus entregas asignadas."
        ));

        List<RouterLink> links = findAllRouterLinks(view);
        assertEquals(2, links.size());
    }

    // Helpers mínimos

    private static H2 findH2ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H2 h2 && text.equals(h2.getText())) return h2;
        }
        return null;
    }

    private static Span findSpanByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Span s && text.equals(s.getText())) return s;
        }
        return null;
    }

    private static List<RouterLink> findAllRouterLinks(Component root) {
        List<RouterLink> out = new ArrayList<>();
        for (Component c : flatten(root)) {
            if (c instanceof RouterLink rl) out.add(rl);
        }
        return out;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        if (c == null) return out;
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }
}