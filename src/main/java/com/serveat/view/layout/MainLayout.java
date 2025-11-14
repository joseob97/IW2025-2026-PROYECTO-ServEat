package com.serveat.view.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.serveat.view.publico.inicio.InicioView;
import com.serveat.view.publico.carta.CartaView;

public class MainLayout extends AppLayout {

    public MainLayout() {
        createHeader();
    }

    private void createHeader() {
        H1 logo = new H1("ServEat");
        logo.getStyle().set("font-size", "24px");

        RouterLink linkInicio = new RouterLink("Inicio", InicioView.class);
        RouterLink linkCarta = new RouterLink("Carta", CartaView.class);

        Anchor logout = new Anchor("/logout", "Salir");

        HorizontalLayout header = new HorizontalLayout(
                logo, linkInicio, linkCarta, logout
        );

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.expand(logo);

        addToNavbar(header);
    }
}