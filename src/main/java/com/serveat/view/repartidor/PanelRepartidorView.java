package com.serveat.view.empleado;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("empleado/repartidor")
public class PanelRepartidorView extends VerticalLayout {

    public PanelRepartidorView() {
        add(new H2("Panel Repartidor"));
        add(new Paragraph("Aquí el repartidor verá sus entregas, rutas, estados, etc."));
    }
}
