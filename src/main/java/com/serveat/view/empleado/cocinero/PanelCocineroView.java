package com.serveat.view.empleado.cocinero;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/cocinero", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class PanelCocineroView extends VerticalLayout {

    public PanelCocineroView() {
        setSpacing(true);
        setPadding(true);

        H2 titulo = new H2("Panel Cocinero");

        // Menú de opciones para el cocinero

        add(titulo);}
}