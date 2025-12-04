package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.service.usuario.EmpleadoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@Route(value = "empleado/admin/empleados", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class GestionEmpleadosView extends VerticalLayout {

    private final EmpleadoService empleadoService;
    private final Grid<Empleado> grid;

    public GestionEmpleadosView(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;

        setSpacing(true);
        setPadding(true);

        H2 titulo = new H2("Gestión de empleados");

        // Botón NUEVO EMPLEADO arriba del grid
        Button nuevoEmpleadoBtn = new Button("Nuevo empleado");
        nuevoEmpleadoBtn.getStyle().set("font-size", "16px");
        nuevoEmpleadoBtn.getStyle().set("font-weight", "bold");
        nuevoEmpleadoBtn.getStyle().set("margin-bottom", "10px");
        nuevoEmpleadoBtn.addClickListener(e -> {
            UI.getCurrent().navigate(NuevoEmpleadoView.class);
        });

        HorizontalLayout botonLayout = new HorizontalLayout(nuevoEmpleadoBtn);
        botonLayout.setWidthFull();
        botonLayout.setJustifyContentMode(JustifyContentMode.END);

        grid = new Grid<>(Empleado.class, false);

        // Definimos columnas
        grid.addColumn(Empleado::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Empleado::getNombre).setHeader("Nombre").setAutoWidth(true);
        grid.addColumn(Empleado::getUsername).setHeader("Usuario").setAutoWidth(true);
        grid.addColumn(Empleado::getRol).setHeader("Rol").setAutoWidth(true);

        // Columna con botones de acción
        grid.addComponentColumn(this::crearAcciones)
                .setHeader("Acciones")
                .setAutoWidth(true);

        // Cargar datos
        cargarEmpleados();

        add(titulo,botonLayout, grid);
    }

    private HorizontalLayout crearAcciones(Empleado empleado) {
        Button editar = new Button("Editar", e -> editarEmpleado(empleado));
        Button eliminar = new Button("Eliminar", e -> eliminarEmpleado(empleado));

        return new HorizontalLayout(editar, eliminar);
    }

    private void cargarEmpleados() {
        List<Empleado> empleados = empleadoService.findAll();
        grid.setItems(empleados);
    }

    private void editarEmpleado(Empleado empleado) {
        // De momento solo mostramos un aviso. Más adelante haremos el formulario.
        Notification.show("Editar empleado: " + empleado.getNombre(), 3000, Notification.Position.MIDDLE);
    }

    private void eliminarEmpleado(Empleado empleado) {
        empleadoService.delete(empleado);
        Notification.show("Empleado eliminado: " + empleado.getNombre(), 3000, Notification.Position.MIDDLE);
        cargarEmpleados(); // refrescamos el grid
    }
}
