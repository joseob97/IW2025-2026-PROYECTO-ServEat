package com.serveat.view.publico.inicio;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("Inicio | ServEat")
@Route(value = "", layout = MainLayout.class)
public class InicioView extends VerticalLayout {

    public InicioView() {
        add(new H2("Bienvenido a ServEat"));
        setAlignItems(Alignment.CENTER);
    }
}