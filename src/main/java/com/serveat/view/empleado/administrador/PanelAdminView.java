package com.serveat.view.empleado.administrador;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/admin", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class PanelAdminView extends VerticalLayout {

    public PanelAdminView() {
        setSpacing(true);
        setPadding(true);

        H2 titulo = new H2("Panel de administración");

        // Menú de opciones para el admin
        RouterLink gestionarEmpleadosLink =
                new RouterLink("Gestionar empleados", GestionEmpleadosView.class);

        // Más adelante podrás añadir:
        Button gestionarClientes = new Button("Gestionar clientes (próximamente)");
        Button gestionarProductos = new Button("Gestionar productos (próximamente)");
        Button gestionarPedidos = new Button("Gestionar pedidos (próximamente)");

        add(titulo, gestionarEmpleadosLink, gestionarClientes, gestionarProductos, gestionarPedidos);
    }
}