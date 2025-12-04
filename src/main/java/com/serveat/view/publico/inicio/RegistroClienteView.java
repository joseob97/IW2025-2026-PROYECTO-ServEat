package com.serveat.view.publico.inicio;

import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.usuario.ClienteRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Route("registro")
@PageTitle("Registro de Cliente | ServEat")
public class RegistroClienteView extends VerticalLayout {

    public RegistroClienteView(ClienteRepository clienteRepository) {

        //CENTRAR FORMULARIO
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // DISEÑO DEL FORMULARIO
        H1 title = new H1("Crear cuenta de cliente");

        TextField nombre = new TextField("Nombre completo");
        nombre.setRequired(true);

        TextField email = new TextField("Email");
        email.setRequired(true);

        TextField username = new TextField("Usuario");
        username.setRequired(true);

        PasswordField password = new PasswordField("Contraseña");
        password.setRequired(true);

        Button btnRegistrar = new Button("Registrarse", event -> {

            // Validación básica
            if (nombre.isEmpty() || username.isEmpty() || password.isEmpty()) {
                Notification.show("Rellena todos los campos", 3000, Notification.Position.MIDDLE);
                return;
            }

            if (clienteRepository.findByUsername(username.getValue()).isPresent()) {
                Notification.show("El nombre de usuario ya existe", 3000, Notification.Position.MIDDLE);
                return;
            }

            if (clienteRepository.findByEmail(email.getValue()).isPresent()) {
                Notification.show("El email ya está registrado", 3000, Notification.Position.MIDDLE);
                return;
            }

            // Crear cliente
            Cliente nuevo = new Cliente();
            nuevo.setNombre(nombre.getValue());
            nuevo.setEmail(email.getValue());
            nuevo.setUsername(username.getValue());
            nuevo.setPassword(new BCryptPasswordEncoder().encode(password.getValue()));

            clienteRepository.save(nuevo);

            Notification.show("Cuenta creada correctamente", 3000, Notification.Position.MIDDLE);

            // Redirigir al login
            getUI().ifPresent(ui -> ui.navigate("login"));
        });

        add(title, nombre, email, username, password, btnRegistrar);
    }
}
