package com.serveat.view.publico.informacion;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Información | ServEat")
@Route(value = "info", layout = MainLayout.class)
public class InformacionSitioView extends VerticalLayout {

    public InformacionSitioView() {
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        add(
                new H2("Información del sitio"),
                new Paragraph("ServEat es una plataforma para pedir a domicilio, recoger en local o gestionar pedidos en sala."),
                new Paragraph("Esta web está en desarrollo para el proyecto de Ingeniería Web.")
        );
    }
}