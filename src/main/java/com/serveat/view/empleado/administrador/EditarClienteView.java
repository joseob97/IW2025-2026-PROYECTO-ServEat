package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Cliente;
import com.serveat.service.usuario.ClienteService;
import com.serveat.service.usuario.exceptions.DuplicadoException;
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
import com.vaadin.flow.component.textfield.PasswordField;
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
    private final TextField nombre = new TextField("Nombre");
    private final TextField username = new TextField("Usuario");
    private final EmailField email = new EmailField("Email");
    private final PasswordField password = new PasswordField("Nueva contraseña");
    private final TextField telefono = new TextField("Teléfono");
    private final TextField direccion = new TextField("Dirección");
    private final Checkbox activo = new Checkbox("Cliente activo");

    public EditarClienteView(ClienteService clienteService) {
        this.clienteService = clienteService;

        setWidth("600px");
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Editar cliente");

        configurarCampos();
        configurarBinder();

        FormLayout formulario = new FormLayout(
                nombre,
                username,
                email,
                password,
                telefono,
                direccion,
                activo
        );

        Button guardar = new Button("Guardar", e -> guardar());
        Button cancelar = new Button("Cancelar",
                e -> getUI().ifPresent(ui ->
                        ui.navigate("empleado/admin/gestion-clientes"))
        );

        HorizontalLayout acciones = new HorizontalLayout(guardar, cancelar);

        add(titulo, formulario, acciones);
    }

    /* =========================
       CONFIGURACIÓN CAMPOS
       ========================= */
    private void configurarCampos() {

        email.setClearButtonVisible(true);
        email.setErrorMessage("Introduce un email válido");

        telefono.setAllowedCharPattern("[0-9]");
        telefono.setMaxLength(15);
        telefono.setHelperText("Solo números (9–15 dígitos)");

        password.setRevealButtonVisible(false);
        password.setPlaceholder("Déjala en blanco para no cambiarla");
    }

    /* =========================
       BINDER / VALIDACIONES
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
                .withValidator(
                        e -> e != null && e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                        "Formato de email no válido"
                )
                .bind(Cliente::getEmail, Cliente::setEmail);

        // 🔐 PASSWORD OPCIONAL
        binder.forField(password)
                .withValidator(
                        p -> p == null || p.isBlank() || p.length() >= 6,
                        "La contraseña debe tener al menos 6 caracteres"
                )
                .bind(
                        cliente -> "", // nunca mostramos la actual
                        (cliente, nuevaPassword) -> {
                            if (nuevaPassword != null && !nuevaPassword.isBlank()) {
                                cliente.setPassword(nuevaPassword);
                            }
                        }
                );

        binder.forField(telefono)
                .asRequired("El teléfono es obligatorio")
                .withValidator(
                        t -> t.matches("^[0-9]{9,15}$"),
                        "El teléfono debe tener entre 9 y 15 dígitos"
                )
                .bind(Cliente::getTelefono, Cliente::setTelefono);

        binder.forField(direccion)
                .asRequired("La dirección es obligatoria")
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
                        this::volverAlListado
                );
    }

    /* =========================
       GUARDAR
       ========================= */
    private void guardar() {

        if (!binder.validate().isOk()) {
            return;
        }

        try {
            clienteService.guardar(cliente);

            Notification.show(
                    "Cliente actualizado correctamente",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            volverAlListado();

        } catch (DuplicadoException e) {

            Notification.show(
                    e.getMessage(),
                    4000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void volverAlListado() {
        getUI().ifPresent(ui ->
                ui.navigate("empleado/admin/gestion-clientes")
        );
    }
}
