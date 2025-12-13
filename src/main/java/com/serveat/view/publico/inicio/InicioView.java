package com.serveat.view.publico.inicio;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Inicio | ServEat")
@Route(value = "", layout = MainLayout.class)
public class InicioView extends VerticalLayout {

    public InicioView() {
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        H2 titulo = new H2("Bienvenido a ServEat");
        Paragraph subtitulo = new Paragraph("Pide a domicilio, recoge en local o gestiona tu pedido en tiempo real.");

        Button verCarta = new Button("Ver carta", e -> getUI().ifPresent(ui -> ui.navigate("carta")));
        verCarta.getStyle().set("margin-top", "12px");

        add(titulo, subtitulo, verCarta);
    }
}