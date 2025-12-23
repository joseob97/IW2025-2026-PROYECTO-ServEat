package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Cliente;
import com.serveat.service.usuario.ClienteService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.*;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Editar cliente | ServEat")
@Route(value = "empleado/admin/gestion-clientes/editar/:id", layout = MainLayout.class)
@RolesAllowed("ROLE_ADMIN")
public class EditarClienteView extends VerticalLayout implements BeforeEnterObserver {

    private final ClienteService clienteService;

    private Cliente cliente;

    private final Binder<Cliente> binder = new Binder<>(Cliente.class);

    // Campos editables
    private TextField nombre = new TextField("Nombre");
    private TextField username = new TextField("Usuario");
    private EmailField email = new EmailField("Email");
    private TextField telefono = new TextField("Teléfono");
    private TextField direccion = new TextField("Dirección");
    private Checkbox activo = new Checkbox("Cliente activo");

    public EditarClienteView(ClienteService clienteService) {
        this.clienteService = clienteService;

        setWidth("600px");
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Editar cliente");

        FormLayout formulario = new FormLayout();

        email.setClearButtonVisible(true);
        telefono.setClearButtonVisible(true);

        formulario.add(
                nombre,
                username,
                email,
                telefono,
                direccion,
                activo
        );

        Button guardar = new Button("Guardar");
        Button cancelar = new Button("Cancelar");

        guardar.addClickListener(e -> guardarCambios());
        cancelar.addClickListener(e ->
                getUI().ifPresent(ui ->
                        ui.navigate("empleado/admin/gestion-clientes")
                )
        );

        HorizontalLayout acciones = new HorizontalLayout(guardar, cancelar);

        add(titulo, formulario, acciones);

        configurarBinder();
    }

    /* =========================
       BINDER
       ========================= */
    private void configurarBinder() {

        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .bind(Cliente::getNombre, Cliente::setNombre);

        binder.forField(username)
                .asRequired("El usuario es obligatorio")
                .bind(Cliente::getUsername, Cliente::setUsername);

        binder.forField(email)
                .asRequired("El email es obligatorio")
                .bind(Cliente::getEmail, Cliente::setEmail);

        binder.forField(telefono)
                .bind(Cliente::getTelefono, Cliente::setTelefono);

        binder.forField(direccion)
                .bind(Cliente::getDireccion, Cliente::setDireccion);

        binder.forField(activo)
                .bind(Cliente::isActivo, Cliente::setActivo);
    }

    /* =========================
       CARGA DEL CLIENTE
       ========================= */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        event.getRouteParameters()
                .get("id")
                .map(Long::valueOf)
                .ifPresentOrElse(
                        id -> {
                            cliente = clienteService.obtenerPorId(id);
                            binder.setBean(cliente);
                        },
                        () -> redirigirListado()
                );
    }

    /* =========================
       GUARDAR
       ========================= */
    private void guardarCambios() {

        if (binder.validate().isOk()) {
            clienteService.guardar(cliente);

            Notification.show(
                    "Cliente actualizado correctamente",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            redirigirListado();
        }
    }

    private void redirigirListado() {
        getUI().ifPresent(ui ->
                ui.navigate("empleado/admin/gestion-clientes")
        );
    }
}
