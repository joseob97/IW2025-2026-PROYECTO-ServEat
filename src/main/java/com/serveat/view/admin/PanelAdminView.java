package com.serveat.view.empleado;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("empleado/admin")
public class PanelAdminView extends VerticalLayout {

    public PanelAdminView() {
        add(new H2("Panel Administrador"));
        add(new Paragraph("Zona para administración general del sistema ServEat."));
    }
}
