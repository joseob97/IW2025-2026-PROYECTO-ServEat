package com.serveat.view.empleado.administrador;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/admin/usuarios", layout = MainLayout.class)
@PageTitle("Usuarios | Admin")
@Secured("ROLE_ADMIN")
public class GestionUsuariosView extends VerticalLayout {

    public GestionUsuariosView() {
        setPadding(true);
        setSpacing(true);

        add(new H2("Gestión de usuarios"),
                new Paragraph("Aquí podrás gestionar clientes/usuarios (sprint siguiente)."));
    }
}