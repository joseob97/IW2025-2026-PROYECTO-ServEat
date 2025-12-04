package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Route(value = "empleado/admin/nuevo-empleado", layout = com.serveat.view.layout.MainLayout.class)
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

        PasswordField password = new PasswordField("Contraseña");
        password.setRequired(true);

        ComboBox<String> rol = new ComboBox<>("Rol");
        rol.setItems("CAMARERO", "COCINERO", "REPARTIDOR", "ADMIN");
        rol.setRequired(true);

        Button guardar = new Button("Crear empleado");

        guardar.addClickListener(ev -> {

            if (nombre.isEmpty() || username.isEmpty() || password.isEmpty() || rol.isEmpty()) {
                Notification.show("Completa todos los campos.");
                return;
            }

            if (empleadoRepository.findByUsername(username.getValue()).isPresent()) {
                Notification.show("Ese nombre de usuario ya existe.");
                return;
            }

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

            Empleado empleado = new Empleado();
            empleado.setNombre(nombre.getValue());
            empleado.setUsername(username.getValue());
            empleado.setPassword(encoder.encode(password.getValue()));
            empleado.setRol(rol.getValue());

            empleadoRepository.save(empleado);

            Notification.show("Empleado creado correctamente.");
            UI.getCurrent().navigate(GestionEmpleadosView.class);
        });

        add(nombre, username, password, rol, guardar);
    }
}
