package com.serveat.view.empleado.camarero;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/camarero", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class PanelCamareroView extends VerticalLayout {

    public PanelCamareroView() {
        setSpacing(true);
        setPadding(true);

        H2 titulo = new H2("Panel Camarero");

        // Menú de opciones para el camarero

        add(titulo);
    }
}