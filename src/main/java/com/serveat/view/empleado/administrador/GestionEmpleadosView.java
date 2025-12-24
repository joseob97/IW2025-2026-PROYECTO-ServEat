package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.service.usuario.EmpleadoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "empleado/admin/gestion-empleados", layout = MainLayout.class)
@PageTitle("Gestión de empleados | ServEat")
@RolesAllowed("ROLE_ADMIN")
public class GestionEmpleadosView extends VerticalLayout {

    private final EmpleadoService empleadoService;

    private final Grid<Empleado> grid = new Grid<>(Empleado.class, false);
    private ListDataProvider<Empleado> dataProvider;

    private TextField buscador;
    private ComboBox<String> filtroEstado;
    private ComboBox<String> filtroRol;

    public GestionEmpleadosView(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Gestión de empleados");

        configurarGrid();
        cargarEmpleados();

        HorizontalLayout filtros = crearFiltros();

        add(titulo, filtros, grid);
    }

    /* =========================
       CONFIGURACIÓN GRID
       ========================= */
    private void configurarGrid() {

        grid.addColumn(Empleado::getNombre)
                .setHeader("Nombre")
                .setAutoWidth(true);

        grid.addColumn(Empleado::getUsername)
                .setHeader("Usuario")
                .setAutoWidth(true);

        grid.addColumn(Empleado::getEmail)
                .setHeader("Email")
                .setAutoWidth(true);

        grid.addColumn(Empleado::getTelefono)
                .setHeader("Teléfono")
                .setAutoWidth(true);

        grid.addColumn(Empleado::getRol)
                .setHeader("Rol")
                .setAutoWidth(true);

        grid.addColumn(emp -> emp.isEnabled() ? "Activo" : "Inactivo")
                .setHeader("Estado")
                .setAutoWidth(true);

        grid.addComponentColumn(this::crearAcciones)
                .setHeader("Acciones");

        grid.setSizeFull();
    }

    /* =========================
       ACCIONES
       ========================= */
    private HorizontalLayout crearAcciones(Empleado empleado) {

        Button editar = new Button("Editar");
        Button cambiarEstado = new Button();
        Button eliminar = new Button("Eliminar");

        // TEXTO Y COLOR SEGÚN ESTADO
        if (empleado.isEnabled()) {
            cambiarEstado.setText("Desactivar");
            cambiarEstado.getStyle().set("background", "#d9534f");
        } else {
            cambiarEstado.setText("Activar");
            cambiarEstado.getStyle().set("background", "#5cb85c");
        }
        cambiarEstado.getStyle().set("color", "white");

        editar.addClickListener(e ->
                UI.getCurrent().navigate(
                        "empleado/admin/gestion-empleados/editar-empleado/" + empleado.getId()
                )
        );

        cambiarEstado.addClickListener(e ->
                mostrarConfirmacion(
                        empleado.isEnabled() ? "Desactivar empleado" : "Activar empleado",
                        empleado.isEnabled()
                                ? "¿Seguro que quieres desactivar este empleado?"
                                : "¿Seguro que quieres activar este empleado?",
                        () -> {
                            if (empleado.isEnabled()) {
                                empleadoService.desactivar(empleado);
                            } else {
                                empleadoService.activar(empleado);
                            }
                            cargarEmpleados();
                        }
                )
        );

        eliminar.addClickListener(e ->
                mostrarConfirmacion(
                        "Eliminar empleado",
                        "Esta acción es irreversible. ¿Deseas continuar?",
                        () -> {
                            empleadoService.eliminar(empleado);
                            cargarEmpleados();
                        }
                )
        );

        return new HorizontalLayout(editar, cambiarEstado, eliminar);
    }

    /* =========================
       CARGA DE DATOS
       ========================= */
    private void cargarEmpleados() {
        dataProvider = new ListDataProvider<>(empleadoService.obtenerTodos());
        grid.setDataProvider(dataProvider);
    }

    /* =========================
       FILTROS
       ========================= */
    private HorizontalLayout crearFiltros() {

        buscador = new TextField();
        buscador.setPlaceholder("Buscar por nombre, usuario, email o teléfono");
        buscador.setClearButtonVisible(true);
        buscador.setWidth("320px");

        filtroEstado = new ComboBox<>();
        filtroEstado.setItems("Todos", "Activos", "Inactivos");
        filtroEstado.setValue("Todos");
        filtroEstado.setWidth("150px");

        filtroRol = new ComboBox<>();
        filtroRol.setItems("Todos", "ADMIN", "CAMARERO", "COCINERO", "REPARTIDOR");
        filtroRol.setValue("Todos");
        filtroRol.setWidth("170px");

        buscador.addValueChangeListener(e -> aplicarFiltros());
        filtroEstado.addValueChangeListener(e -> aplicarFiltros());
        filtroRol.addValueChangeListener(e -> aplicarFiltros());

        HorizontalLayout filtros = new HorizontalLayout(
                buscador, filtroEstado, filtroRol
        );
        filtros.setAlignItems(Alignment.BASELINE);
        filtros.setSpacing(true);

        return filtros;
    }

    private void aplicarFiltros() {

        String texto = buscador.getValue() == null
                ? ""
                : buscador.getValue().toLowerCase();

        String estado = filtroEstado.getValue();
        String rol = filtroRol.getValue();

        dataProvider.setFilter(empleado -> {

            boolean coincideTexto =
                    empleado.getNombre().toLowerCase().contains(texto)
                            || empleado.getUsername().toLowerCase().contains(texto)
                            || empleado.getEmail().toLowerCase().contains(texto)
                            || (empleado.getTelefono() != null
                            && empleado.getTelefono().contains(texto));

            boolean coincideEstado = switch (estado) {
                case "Activos" -> empleado.isEnabled();
                case "Inactivos" -> !empleado.isEnabled();
                default -> true;
            };

            boolean coincideRol = rol.equals("Todos")
                    || empleado.getRol().equalsIgnoreCase(rol);

            return coincideTexto && coincideEstado && coincideRol;
        });
    }

    /* =========================
       CONFIRMACIÓN
       ========================= */
    private void mostrarConfirmacion(String titulo, String mensaje, Runnable accionConfirmada) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(titulo);

        dialog.add(new Paragraph(mensaje));

        Button cancelar = new Button("Cancelar", e -> dialog.close());
        Button confirmar = new Button("Confirmar", e -> {
            accionConfirmada.run();
            dialog.close();
        });

        confirmar.getStyle().set("color", "red");

        dialog.getFooter().add(cancelar, confirmar);
        dialog.open();
    }
}
