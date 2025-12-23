package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Cliente;
import com.serveat.service.usuario.ClienteService;
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

@PageTitle("Gestión de clientes | ServEat")
@Route(value = "empleado/admin/gestion-clientes", layout = MainLayout.class)
@RolesAllowed("ROLE_ADMIN")
public class GestionClientesView extends VerticalLayout {

    private final ClienteService clienteService;

    private final Grid<Cliente> grid = new Grid<>(Cliente.class, false);
    private ListDataProvider<Cliente> dataProvider;

    private TextField buscador;
    private ComboBox<String> filtroEstado;

    public GestionClientesView(ClienteService clienteService) {
        this.clienteService = clienteService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Gestión de clientes");

        configurarGrid();
        cargarClientes();

        HorizontalLayout filtros = crearFiltros();

        add(titulo, filtros, grid);
    }

    /* =========================
       CONFIGURACIÓN GRID
       ========================= */
    private void configurarGrid() {

        grid.addColumn(Cliente::getNombre)
                .setHeader("Nombre")
                .setAutoWidth(true);

        grid.addColumn(Cliente::getEmail)
                .setHeader("Email")
                .setAutoWidth(true);

        grid.addColumn(Cliente::getTelefono)
                .setHeader("Teléfono")
                .setAutoWidth(true);

        grid.addColumn(cliente -> cliente.isActivo() ? "Activo" : "Inactivo")
                .setHeader("Estado")
                .setAutoWidth(true);

        grid.addComponentColumn(this::crearAcciones)
                .setHeader("Acciones");

        grid.setSizeFull();
    }

    /* =========================
       ACCIONES
       ========================= */
    private HorizontalLayout crearAcciones(Cliente cliente) {

        Button editar = new Button("Editar");
        Button desactivar = new Button("Desactivar");
        Button eliminar = new Button("Eliminar");

        editar.addClickListener(e ->
                UI.getCurrent().navigate(
                        "empleado/admin/gestion-clientes/editar/" + cliente.getId()
                )
        );

        desactivar.addClickListener(e ->
                mostrarConfirmacion(
                        "Desactivar cliente",
                        "¿Seguro que quieres desactivar este cliente?",
                        () -> {
                            clienteService.desactivar(cliente);
                            cargarClientes();
                        }
                )
        );

        eliminar.addClickListener(e ->
                mostrarConfirmacion(
                        "Eliminar cliente",
                        "Esta acción es irreversible. ¿Deseas continuar?",
                        () -> {
                            clienteService.eliminar(cliente);
                            cargarClientes();
                        }
                )
        );

        return new HorizontalLayout(editar, desactivar, eliminar);
    }

    /* =========================
       CARGA DE DATOS
       ========================= */
    private void cargarClientes() {
        dataProvider = new ListDataProvider<>(clienteService.obtenerTodos());
        grid.setDataProvider(dataProvider);
    }

    /* =========================
       FILTROS
       ========================= */
    private HorizontalLayout crearFiltros() {

        buscador = new TextField();
        buscador.setPlaceholder("Buscar por nombre, email o teléfono");
        buscador.setClearButtonVisible(true);
        buscador.setWidth("300px");

        filtroEstado = new ComboBox<>();
        filtroEstado.setItems("Todos", "Activos", "Inactivos");
        filtroEstado.setValue("Todos");
        filtroEstado.setWidth("150px");

        buscador.addValueChangeListener(e -> aplicarFiltros());
        filtroEstado.addValueChangeListener(e -> aplicarFiltros());

        HorizontalLayout filtros = new HorizontalLayout(buscador, filtroEstado);
        filtros.setAlignItems(Alignment.BASELINE);
        filtros.setSpacing(true);

        return filtros;
    }

    private void aplicarFiltros() {

        String texto = buscador.getValue() == null
                ? ""
                : buscador.getValue().toLowerCase();

        String estado = filtroEstado.getValue();

        dataProvider.setFilter(cliente -> {

            boolean coincideTexto =
                    cliente.getNombre().toLowerCase().contains(texto)
                            || cliente.getEmail().toLowerCase().contains(texto)
                            || (cliente.getTelefono() != null
                            && cliente.getTelefono().toLowerCase().contains(texto));

            boolean coincideEstado = switch (estado) {
                case "Activos" -> cliente.isActivo();
                case "Inactivos" -> !cliente.isActivo();
                default -> true; // Todos
            };

            return coincideTexto && coincideEstado;
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
