package com.serveat.view.empleado;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("empleado/camarero")
public class PanelCamareroView extends VerticalLayout {

    public PanelCamareroView() {
        add(new H2("Panel Camarero"));
        add(new Paragraph("Aquí irán las funciones específicas del camarero (gestión de mesas, pedidos en sala, etc.)."));
    }
}
