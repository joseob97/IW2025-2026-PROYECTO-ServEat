package com.serveat.view.compartida.panel;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PanelBaseViewTest {

    @BeforeEach
    void setup() {
        MockVaadin.setup();
        RouteConfiguration.forSessionScope().setRoute("dummy-test", DummyView.class);
    }

    @AfterEach
    void tearDown() {
        MockVaadin.tearDown();
    }

    @Test
    void card_link_crea_router_link() {
        Component c = PanelBaseView.Cards.cardLink("Titulo", "Descripcion", DummyView.class);
        assertNotNull(c);
        assertTrue(c instanceof RouterLink);
    }

    @Test
    void card_link_destacada_crea_router_link() {
        Component c = PanelBaseView.Cards.cardLinkDestacada("Titulo", "Descripcion", DummyView.class);
        assertNotNull(c);
        assertTrue(c instanceof RouterLink);
    }

    @Route("dummy-test")
    public static class DummyView extends VerticalLayout {
    }
}