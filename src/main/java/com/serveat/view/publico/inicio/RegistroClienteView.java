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
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Route(value = "registro", layout = MainLayout.class)
@PageTitle("Registro | ServEat")
public class RegistroClienteView extends VerticalLayout {

    private final Binder<Cliente> binder = new Binder<>(Cliente.class);

    public RegistroClienteView(ClienteRepository clienteRepository) {

        setSizeFull();
        setPadding(true);
        setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout contenedor = new VerticalLayout();
        contenedor.setWidth("420px");
        contenedor.setPadding(true);
        contenedor.setSpacing(true);
        contenedor.getStyle()
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "8px")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.05)");

        H1 title = new H1(getTranslation("registro.titulo"));
        title.getStyle().set("margin-bottom", "1rem");

        // =========================
        // CAMPOS
        // =========================
        TextField nombre = new TextField(getTranslation("registro.nombre"));
        EmailField email = new EmailField(getTranslation("registro.email"));
        TextField username = new TextField(getTranslation("registro.usuario"));
        PasswordField password = new PasswordField(getTranslation("registro.password"));
        TextField telefono = new TextField(getTranslation("registro.telefono"));
        TextField direccion = new TextField(getTranslation("registro.direccion"));

        telefono.setAllowedCharPattern("[0-9]");
        telefono.setMaxLength(15);
        telefono.setHelperText(getTranslation("registro.telefono.helper"));

        email.setErrorMessage(getTranslation("registro.email.error"));

        // =========================
        // BINDER / VALIDACIONES
        // =========================
        binder.forField(nombre)
                .asRequired(getTranslation("registro.error.nombre"))
                .bind(Cliente::getNombre, Cliente::setNombre);

        binder.forField(email)
                .asRequired(getTranslation("registro.error.email"))
                .withValidator(
                        new EmailValidator(getTranslation("registro.error.email.formato"))
                )
                .bind(Cliente::getEmail, Cliente::setEmail);

        binder.forField(username)
                .asRequired(getTranslation("registro.error.usuario"))
                .bind(Cliente::getUsername, Cliente::setUsername);

        binder.forField(password)
                .asRequired(getTranslation("registro.error.password"))
                .bind(Cliente::getPassword, Cliente::setPassword);

        binder.forField(telefono)
                .asRequired(getTranslation("registro.error.telefono"))
                .withValidator(
                        t -> t != null && t.matches("^[0-9]{9,15}$"),
                        getTranslation("registro.error.telefono.formato")
                )
                .bind(Cliente::getTelefono, Cliente::setTelefono);

        binder.forField(direccion)
                .asRequired(getTranslation("registro.error.direccion"))
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
        Button btnRegistrar = new Button(getTranslation("registro.boton"));
        btnRegistrar.setWidthFull();
        btnRegistrar.getStyle()
                .set("background-color", "#0366d6")
                .set("color", "white");

        btnRegistrar.addClickListener(event -> {

            Cliente nuevo = new Cliente();

            if (!binder.writeBeanIfValid(nuevo)) {
                Notification.show(
                        getTranslation("registro.error.formulario"),
                        3000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (clienteRepository.findByUsername(nuevo.getUsername()).isPresent()) {
                Notification.show(
                        getTranslation("registro.error.usuarioExiste"),
                        3000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            if (clienteRepository.findByEmail(nuevo.getEmail()).isPresent()) {
                Notification.show(
                        getTranslation("registro.error.emailExiste"),
                        3000,
                        Notification.Position.MIDDLE
                ).addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            nuevo.setPassword(
                    new BCryptPasswordEncoder().encode(nuevo.getPassword())
            );
            clienteRepository.save(nuevo);

            Notification.show(
                    getTranslation("registro.exito"),
                    3000,
                    Notification.Position.MIDDLE
            ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> ui.navigate("login"));
        });

        contenedor.add(title, form, btnRegistrar);
        add(contenedor);
    }
}
