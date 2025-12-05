package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Route(value = "empleado/admin/gestion-empleado/nuevo-empleado", layout = com.serveat.view.layout.MainLayout.class)
@PageTitle("Nuevo empleado")
public class NuevoEmpleadoView extends VerticalLayout {

    public NuevoEmpleadoView(EmpleadoRepository empleadoRepository) {

        setAlignItems(Alignment.CENTER);

        H2 titulo = new H2("Dar de alta a un nuevo empleado");
        add(titulo);

        TextField nombre = new TextField("Nombre completo");
        nombre.setRequired(true);

        TextField username = new TextField("Usuario");
        username.setRequired(true);

        TextField email = new TextField("Email");
        email.setRequired(true);

        TextField telefono = new TextField("Teléfono");
        telefono.setRequired(true);

        // Validación teléfono: solo números
        telefono.addValueChangeListener(e -> {
            if (!e.getValue().matches("\\d*")) {
                telefono.setInvalid(true);
                telefono.setErrorMessage("El teléfono solo puede contener números.");
            } else {
                telefono.setInvalid(false);
            }
        });

        TextField direccion = new TextField("Dirección");
        direccion.setRequired(true);

        PasswordField password = new PasswordField("Contraseña");
        password.setRequired(true);

        ComboBox<String> rol = new ComboBox<>("Rol");
        rol.setItems("CAMARERO", "COCINERO", "REPARTIDOR", "ADMIN");
        rol.setRequired(true);

        Checkbox enabled = new Checkbox("Empleado activo");
        enabled.setValue(true); // Por defecto activado

        Button guardar = new Button("Crear empleado");
        guardar.getStyle().set("background-color", "#0366d6");
        guardar.getStyle().set("color", "white");

        guardar.addClickListener(ev -> {

            // Validación básica
            if (nombre.isEmpty() || username.isEmpty() || password.isEmpty() ||
                    email.isEmpty() || telefono.isEmpty() || direccion.isEmpty() || rol.isEmpty()) {

                Notification.show("Completa todos los campos obligatorios.");
                return;
            }

            // Validación email único
            if (empleadoRepository.findByEmail(email.getValue()).isPresent()) {
                Notification.show("Ese email ya está registrado.");
                return;
            }

            // Validación username único
            if (empleadoRepository.findByUsername(username.getValue()).isPresent()) {
                Notification.show("Ese nombre de usuario ya existe.");
                return;
            }

            // Validación teléfono
            if (telefono.isInvalid()) {
                Notification.show("Corrige el teléfono antes de continuar.");
                return;
            }

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            Empleado empleado = new Empleado();
            empleado.setNombre(nombre.getValue());
            empleado.setUsername(username.getValue());
            empleado.setPassword(encoder.encode(password.getValue()));
            empleado.setTelefono(telefono.getValue());
            empleado.setEmail(email.getValue());
            empleado.setDireccion(direccion.getValue());
            empleado.setRol(rol.getValue());
            empleado.setEnabled(enabled.getValue());

            empleadoRepository.save(empleado);

            Notification.show("Empleado creado correctamente.");
            UI.getCurrent().navigate(GestionEmpleadosView.class);
        });

        add(nombre, username, email, telefono, direccion, password, rol, enabled, guardar);
    }
}
