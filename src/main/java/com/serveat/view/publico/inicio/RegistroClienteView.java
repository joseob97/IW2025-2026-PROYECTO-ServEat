package com.serveat.view.publico.inicio;

import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Route(value = "registro", layout = MainLayout.class)
@PageTitle("Registro de Cliente | ServEat")
public class RegistroClienteView extends VerticalLayout {

    private final Binder<Cliente> binder = new Binder<>(Cliente.class);

    public RegistroClienteView(ClienteRepository clienteRepository) {

        // =========================
        // CONFIGURACIÓN GENERAL
        // =========================
        setSizeFull();
        setPadding(true);
        setAlignItems(FlexComponent.Alignment.CENTER);

        // Contenedor centrado
        VerticalLayout contenedor = new VerticalLayout();
        contenedor.setWidth("420px");
        contenedor.setPadding(true);
        contenedor.setSpacing(true);
        contenedor.getStyle()
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.05)");

        H1 title = new H1("Crear cuenta de cliente");
        title.getStyle().set("margin-bottom", "1rem");

        // =========================
        // CAMPOS
        // =========================
        TextField nombre = new TextField("Nombre completo");
        EmailField email = new EmailField("Email");
        TextField username = new TextField("Usuario");
        PasswordField password = new PasswordField("Contraseña");
        TextField telefono = new TextField("Teléfono");
        TextField direccion = new TextField("Dirección");

        telefono.setAllowedCharPattern("[0-9]");
        telefono.setMaxLength(15);
        telefono.setHelperText("Solo números (9–15 dígitos)");

        email.setErrorMessage("Email no válido");

        // =========================
        // BINDER / VALIDACIONES
        // =========================
        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .bind(Cliente::getNombre, Cliente::setNombre);

        binder.forField(email)
                .asRequired("El email es obligatorio")
                .withValidator(
                        e -> e.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                        "Formato de email incorrecto"
                )
                .bind(Cliente::getEmail, Cliente::setEmail);

        binder.forField(username)
                .asRequired("El usuario es obligatorio")
                .bind(Cliente::getUsername, Cliente::setUsername);

        binder.forField(password)
                .asRequired("La contraseña es obligatoria")
                .bind(Cliente::getPassword, Cliente::setPassword);

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

        // =========================
        // FORMULARIO
        // =========================
        FormLayout form = new FormLayout(
                nombre, email, username, password, telefono, direccion
        );
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );

        // =========================
        // BOTÓN
        // =========================
        Button btnRegistrar = new Button("Registrarse");
        btnRegistrar.setWidthFull();
        btnRegistrar.getStyle()
                .set("background-color", "#0366d6")
                .set("color", "white");

        btnRegistrar.addClickListener(event -> {

            Cliente nuevo = new Cliente();

            if (!binder.writeBeanIfValid(nuevo)) {
                Notification.show(
                        "Corrige los errores del formulario",
                        3000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (clienteRepository.findByUsername(nuevo.getUsername()).isPresent()) {
                Notification.show("El nombre de usuario ya existe",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (clienteRepository.findByEmail(nuevo.getEmail()).isPresent()) {
                Notification.show("El email ya está registrado",
                                3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            nuevo.setPassword(new BCryptPasswordEncoder().encode(nuevo.getPassword()));
            clienteRepository.save(nuevo);

            Notification.show(
                    "Cuenta creada correctamente",
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> ui.navigate("login"));
        });

        contenedor.add(title, form, btnRegistrar);
        add(contenedor);
    }
}
