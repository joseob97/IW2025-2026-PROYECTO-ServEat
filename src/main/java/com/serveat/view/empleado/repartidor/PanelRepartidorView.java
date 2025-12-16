package com.serveat.view.empleado.repartidor;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/cocinero", layout = MainLayout.class)
@Secured("ROLE_REPARTIDOR")
public class PanelRepartidorView extends VerticalLayout {

    public PanelRepartidorView() {
        setSpacing(true);
        setPadding(true);

        H2 titulo = new H2("Panel Repartidor");

        // Menú de opciones para el repartidor

        add(titulo);}
}