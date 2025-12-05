package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.service.usuario.EmpleadoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@Route(value = "empleado/admin/gestion-empleados", layout = MainLayout.class)
@PageTitle("Gestión de empleados | ServEat")
@Secured("ROLE_ADMIN")
public class GestionEmpleadosView extends VerticalLayout {

    private final EmpleadoService empleadoService;
    private final Grid<Empleado> grid = new Grid<>(Empleado.class, false);

    public GestionEmpleadosView(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Gestión de empleados");
        add(titulo);

        configurarGrid();
        cargarEmpleados();

        add(grid);
    }

    private void configurarGrid() {

        grid.addColumn(Empleado::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Empleado::getNombre).setHeader("Nombre").setAutoWidth(true);
        grid.addColumn(Empleado::getUsername).setHeader("Usuario").setAutoWidth(true);
        grid.addColumn(Empleado::getEmail).setHeader("Email").setAutoWidth(true);
        grid.addColumn(emp -> emp.isEnabled() ? "Activo" : "Desactivado")
                .setHeader("Estado")
                .setAutoWidth(true);

        // Acciones
        grid.addComponentColumn(this::crearAcciones)
                .setHeader("Acciones")
                .setAutoWidth(true);

        // Estilo visual adicional
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
    }

    private HorizontalLayout crearAcciones(Empleado empleado) {

        Button editar = new Button("Editar");
        editar.addClickListener(e -> {
            UI.getCurrent().navigate(
                    "empleado/admin/gestion-empleados/editar-empleado/" + empleado.getId()
            );
        });

        Button activarDesactivar = new Button(
                empleado.isEnabled() ? "Desactivar" : "Activar"
        );

        activarDesactivar.getStyle().set("background-color",
                empleado.isEnabled() ? "#d9534f" : "#5cb85c");
        activarDesactivar.getStyle().set("color", "white");

        activarDesactivar.addClickListener(e -> {

            empleado.setEnabled(!empleado.isEnabled());
            empleadoService.save(empleado);

            Notification.show(
                    empleado.isEnabled()
                            ? "Empleado activado"
                            : "Empleado desactivado",
                    3000,
                    Notification.Position.MIDDLE
            );

            cargarEmpleados();
        });

        Button eliminar = new Button("Eliminar");
        eliminar.getStyle().set("color", "red");

        eliminar.addClickListener(e -> {
            empleadoService.delete(empleado);
            Notification.show("Empleado eliminado", 3000, Notification.Position.MIDDLE);
            cargarEmpleados();
        });

        return new HorizontalLayout(editar, activarDesactivar, eliminar);
    }


    private void cargarEmpleados() {
        List<Empleado> empleados = empleadoService.findAll();
        grid.setItems(empleados);
    }
}
