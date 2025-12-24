package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.service.usuario.EmpleadoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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

@Route(value = "empleado/admin/gestion-empleados/editar-empleado/:id", layout = MainLayout.class)
@PageTitle("Editar empleado | ServEat")
@RolesAllowed("ROLE_ADMIN")
public class EditarEmpleadoView extends VerticalLayout implements BeforeEnterObserver {

    private final EmpleadoService empleadoService;
    private Empleado empleado;

    private final Binder<Empleado> binder = new Binder<>(Empleado.class);

    // Campos
    private final TextField nombre = new TextField("Nombre completo");
    private final TextField username = new TextField("Usuario");
    private final EmailField email = new EmailField("Email");
    private final TextField direccion = new TextField("Dirección");
    private final TextField telefono = new TextField("Teléfono");
    private final ComboBox<String> rol = new ComboBox<>("Rol");
    private final Checkbox enabled = new Checkbox("Empleado activo");
    private final PasswordField password = new PasswordField("Nueva contraseña (opcional)");

    public EditarEmpleadoView(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;

        setWidth("600px");
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Editar empleado");

        configurarCampos();
        configurarBinder();

        FormLayout form = new FormLayout(
                nombre,
                username,
                email,
                direccion,
                telefono,
                rol,
                enabled,
                password
        );

        Button guardar = new Button("Guardar", e -> guardar());
        Button cancelar = new Button("Cancelar",
                e -> getUI().ifPresent(ui ->
                        ui.navigate("empleado/admin/gestion-empleados"))
        );

        HorizontalLayout acciones = new HorizontalLayout(guardar, cancelar);

        add(titulo, form, acciones);
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

        rol.setItems("ADMIN", "CAMARERO", "COCINERO", "REPARTIDOR");
        rol.setRequired(true);

        password.setRevealButtonVisible(false);
    }

    /* =========================
       BINDER / VALIDACIONES
       ========================= */
    private void configurarBinder() {

        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .bind(Empleado::getNombre, Empleado::setNombre);

        binder.forField(username)
                .asRequired("El usuario es obligatorio")
                .bind(Empleado::getUsername, Empleado::setUsername);

        binder.forField(email)
                .asRequired("El email es obligatorio")
                .withValidator(
                        e -> e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                        "Formato de email no válido"
                )
                .bind(Empleado::getEmail, Empleado::setEmail);

        binder.forField(direccion)
                .asRequired("La dirección es obligatoria")
                .bind(Empleado::getDireccion, Empleado::setDireccion);

        binder.forField(telefono)
                .asRequired("El teléfono es obligatorio")
                .withValidator(
                        t -> t.matches("^[0-9]{9,15}$"),
                        "Debe tener entre 9 y 15 dígitos"
                )
                .bind(Empleado::getTelefono, Empleado::setTelefono);

        binder.forField(rol)
                .asRequired("El rol es obligatorio")
                .bind(Empleado::getRol, Empleado::setRol);

        binder.forField(enabled)
                .bind(Empleado::isEnabled, Empleado::setEnabled);
    }

    /* =========================
       CARGA EMPLEADO
       ========================= */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        event.getRouteParameters()
                .get("id")
                .map(Long::valueOf)
                .ifPresentOrElse(
                        id -> {
                            empleado = empleadoService.obtenerPorId(id);
                            binder.setBean(empleado);
                        },
                        () -> getUI().ifPresent(ui ->
                                ui.navigate("empleado/admin/gestion-empleados"))
                );
    }

    /* =========================
       GUARDAR
       ========================= */
    private void guardar() {

        if (!binder.validate().isOk()) {
            return;
        }

        // Password solo si se ha escrito
        if (!password.isEmpty()) {
            empleado.setPassword(password.getValue());
        }

        try {
            empleadoService.guardar(empleado);

            Notification.show(
                    "Empleado actualizado correctamente",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui ->
                    ui.navigate("empleado/admin/gestion-empleados"));

        } catch (RuntimeException ex) {
            Notification.show(
                    ex.getMessage(),
                    4000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
