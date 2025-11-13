package com.serveat.view.empleado;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("empleado/cocinero")
public class PanelCocineroView extends VerticalLayout {

    public PanelCocineroView() {
        add(new H2("Panel Cocinero"));
        add(new Paragraph("Aquí el cocinero verá los pedidos en cocina, estados, tiempos, etc."));
    }
}
