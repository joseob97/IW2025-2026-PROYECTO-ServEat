package com.serveat.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.serveat.view.layout.MainLayout;


@Route(value = "", layout = MainLayout.class)
public class IndexView extends VerticalLayout {

    public IndexView() {

        getElement().getClassList().add("index-view");


        H1 titulo = new H1("Bienvenido a ServEat 🍽️");


        Paragraph descripcion = new Paragraph(
                "Aplicación de gestión de pedidos y platos para restaurantes. "
        );


        Button botonCliente = new Button("Entrar como invitado",
                e -> getUI().ifPresent(ui -> ui.navigate("carta")));
        botonCliente.addThemeVariants(ButtonVariant.LUMO_PRIMARY);


        Button botonLogin = new Button("Iniciar Sesión",
                e -> getUI().ifPresent(ui -> ui.navigate("login")));
        botonLogin.addThemeVariants(ButtonVariant.LUMO_CONTRAST);


        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        add(titulo, descripcion, botonCliente, botonLogin);
    }
}
