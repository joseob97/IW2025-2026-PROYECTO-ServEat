package com.serveat.view.publico.inicio;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InicioViewTest {

    @BeforeEach
    void clearVaadinCurrent() {
        UI.setCurrent(null);
    }

    @Test
    void construye_componentes_basicos() {
        InicioView view = new InicioView();

        List<Component> children = view.getChildren().collect(Collectors.toList());

        assertEquals(3, children.size());
        assertTrue(children.get(0) instanceof H2);
        assertTrue(children.get(1) instanceof Paragraph);
        assertTrue(children.get(2) instanceof Button);

        assertEquals(VerticalLayout.Alignment.CENTER, view.getAlignItems());
        assertTrue(view.isPadding());
        assertTrue(view.isSpacing());

        Button verCarta = (Button) children.get(2);
        assertEquals("12px", verCarta.getStyle().get("margin-top"));
    }

    @Test
    void beforeEnter_sin_logout_no_toca_history() {
        InicioView view = new InicioView();

        UI ui = mock(UI.class);
        UI.setCurrent(ui);

        var page = mock(com.vaadin.flow.component.page.Page.class);
        when(ui.getPage()).thenReturn(page);

        var history = mock(com.vaadin.flow.component.page.History.class);
        when(page.getHistory()).thenReturn(history);

        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getLocation()).thenReturn(locationWithQuery(Map.of()));

        view.beforeEnter(event);

        verify(history, never()).replaceState(any(), anyString());
    }

    @Test
    void beforeEnter_con_logout_limpia_url() {
        InicioView view = new InicioView();

        UI ui = mock(UI.class);
        UI.setCurrent(ui);

        var page = mock(com.vaadin.flow.component.page.Page.class);
        when(ui.getPage()).thenReturn(page);

        var history = mock(com.vaadin.flow.component.page.History.class);
        when(page.getHistory()).thenReturn(history);

        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getLocation()).thenReturn(locationWithQuery(Map.of("logout", List.of("1"))));

        view.beforeEnter(event);

        verify(history).replaceState(isNull(), eq(""));
    }

    @Test
    void beforeEnter_con_logout_intenta_aplicar_variant_success() {
        InicioView view = new InicioView();

        UI ui = mock(UI.class);
        UI.setCurrent(ui);

        var page = mock(com.vaadin.flow.component.page.Page.class);
        when(ui.getPage()).thenReturn(page);

        var history = mock(com.vaadin.flow.component.page.History.class);
        when(page.getHistory()).thenReturn(history);

        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getLocation()).thenReturn(locationWithQuery(Map.of("logout", List.of("1"))));

        try (MockedStatic<com.vaadin.flow.component.notification.Notification> notifStatic =
                     mockStatic(com.vaadin.flow.component.notification.Notification.class)) {

            com.vaadin.flow.component.notification.Notification notification =
                    mock(com.vaadin.flow.component.notification.Notification.class);

            notifStatic.when(() ->
                    com.vaadin.flow.component.notification.Notification.show(anyString(), anyInt(), any())
            ).thenReturn(notification);

            view.beforeEnter(event);

            verify(notification).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            verify(history).replaceState(isNull(), eq(""));
        }
    }

    private static Location locationWithQuery(Map<String, List<String>> params) {
        QueryParameters qp = new QueryParameters(params);
        return new Location("", qp);
    }
}