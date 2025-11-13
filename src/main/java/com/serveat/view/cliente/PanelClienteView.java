package com.serveat.view.cliente;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("cliente/panel")
public class PanelClienteView extends VerticalLayout {

    public PanelClienteView() {
        add(new H2("Panel Cliente"));
        add(new Paragraph("Aquí el cliente verá su historial de pedidos, datos, ofertas, etc."));
    }
}
