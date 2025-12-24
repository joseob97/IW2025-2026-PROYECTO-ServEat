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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

@Route(value = "empleado/admin/gestion-empleados/nuevo-empleado", layout = MainLayout.class)
@PageTitle("Nuevo empleado | ServEat")
@RolesAllowed("ROLE_ADMIN")
public class NuevoEmpleadoView extends VerticalLayout {

    private final EmpleadoService empleadoService;
    private final Binder<Empleado> binder = new Binder<>(Empleado.class);

    // Campos
    private final TextField nombre = new TextField("Nombre completo");
    private final TextField username = new TextField("Usuario");
    private final EmailField email = new EmailField("Email");
    private final TextField direccion = new TextField("Dirección");
    private final TextField telefono = new TextField("Teléfono");
    private final PasswordField password = new PasswordField("Contraseña");
    private final ComboBox<String> rol = new ComboBox<>("Rol");
    private final Checkbox enabled = new Checkbox("Empleado activo");

    public NuevoEmpleadoView(EmpleadoService empleadoService) {
        this.empleadoService = empleadoService;

        setWidth("600px");
        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Dar de alta a un nuevo empleado");

        configurarCampos();
        configurarBinder();

        FormLayout form = new FormLayout(
                nombre,
                username,
                email,
                direccion,
                telefono,
                password,
                rol,
                enabled
        );

        Button guardar = new Button("Crear empleado", e -> guardar());
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

        enabled.setValue(true); // activo por defecto
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

        binder.forField(password)
                .asRequired("La contraseña es obligatoria")
                .bind(Empleado::getPassword, Empleado::setPassword);

        binder.forField(rol)
                .asRequired("El rol es obligatorio")
                .bind(Empleado::getRol, Empleado::setRol);

        binder.forField(enabled)
                .bind(Empleado::isEnabled, Empleado::setEnabled);
    }

    /* =========================
       GUARDAR
       ========================= */
    private void guardar() {

        Empleado nuevoEmpleado = new Empleado();

        if (!binder.writeBeanIfValid(nuevoEmpleado)) {
            return;
        }

        try {
            empleadoService.guardar(nuevoEmpleado);

            Notification.show(
                    "Empleado creado correctamente",
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
