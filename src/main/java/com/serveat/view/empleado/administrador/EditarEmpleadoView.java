package com.serveat.view.empleado.administrador;

import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.usuario.EmpleadoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import org.springframework.security.access.annotation.Secured;

import java.util.Optional;

@Route(value = "empleado/admin/gestion-empleados/editar-empleado/:id", layout = MainLayout.class)
@PageTitle("Editar empleado | ServEat")
@Secured("ROLE_ADMIN")
public class EditarEmpleadoView extends VerticalLayout implements BeforeEnterObserver {

    private final EmpleadoService empleadoService;
    private final EmpleadoRepository empleadoRepository;
    private Empleado empleado;

    // Campos del formulario
    TextField nombre = new TextField("Nombre completo");
    TextField username = new TextField("Usuario");
    TextField email = new TextField("Email");
    TextField direccion = new TextField("Dirección");
    TextField telefono = new TextField("Teléfono");
    ComboBox<String> rol = new ComboBox<>("Rol");   // ← CAMBIADO A COMBOBOX
    Checkbox enabled = new Checkbox("Empleado activo");
    PasswordField password = new PasswordField("Nueva contraseña (opcional)");

    Button guardar = new Button("Guardar cambios");
    Button cancelar = new Button("Cancelar");

    public EditarEmpleadoView(EmpleadoService empleadoService,
                              EmpleadoRepository empleadoRepository) {

        this.empleadoService = empleadoService;
        this.empleadoRepository = empleadoRepository;

        setSpacing(true);
        setPadding(true);
        setWidth("600px");

        H2 titulo = new H2("Editar empleado");
        add(titulo);

        // Validación del teléfono (solo números)
        telefono.addValueChangeListener(event -> {
            String value = event.getValue();
            if (!value.matches("\\d*")) {
                telefono.setInvalid(true);
                telefono.setErrorMessage("El teléfono solo puede contener números.");
            } else {
                telefono.setInvalid(false);
            }
        });

        // Configuración del ComboBox de roles
        rol.setItems("CAMARERO", "COCINERO", "REPARTIDOR", "ADMIN");
        rol.setPlaceholder("Selecciona un rol");
        rol.setRequired(true);

        // Formulario
        FormLayout form = new FormLayout();
        form.add(
                nombre,
                username,
                email,
                direccion,
                telefono,
                rol,
                enabled,
                password
        );

        // Botones
        guardar.getStyle().set("background-color", "#0366d6");
        guardar.getStyle().set("color", "white");

        cancelar.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate("empleado/admin/gestion-empleados"))
        );

        guardar.addClickListener(e -> guardarCambios());

        HorizontalLayout botones = new HorizontalLayout(guardar, cancelar);

        add(form, botones);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {

        Optional<String> idParam = event.getRouteParameters().get("id");

        if (idParam.isEmpty()) {
            Notification.show("No se proporcionó el ID del empleado.");
            event.forwardTo("empleado/admin/gestion-empleados");
            return;
        }

        Long id = Long.valueOf(idParam.get());

        Optional<Empleado> empleadoOpt = empleadoService.findById(id);
        if (empleadoOpt.isEmpty()) {
            Notification.show("Empleado no encontrado.");
            event.forwardTo("empleado/admin/gestion-empleados");
            return;
        }

        this.empleado = empleadoOpt.get();

        // Precargar datos en el formulario
        nombre.setValue(empleado.getNombre());
        username.setValue(empleado.getUsername());
        email.setValue(empleado.getEmail());
        direccion.setValue(empleado.getDireccion());
        telefono.setValue(empleado.getTelefono());
        rol.setValue(empleado.getRol());   // ← Ahora es ComboBox
        enabled.setValue(empleado.isEnabled());
    }

    private void guardarCambios() {

        // Validación de campos vacíos
        if (nombre.isEmpty() || username.isEmpty() || telefono.isEmpty()
                || email.isEmpty() || direccion.isEmpty() || rol.isEmpty()) {

            Notification.show("Los campos obligatorios no pueden estar vacíos.",
                    3000, Notification.Position.MIDDLE);
            return;
        }

        // Validación teléfono
        if (telefono.isInvalid()) {
            Notification.show("Corrige el teléfono antes de guardar.",
                    3000, Notification.Position.MIDDLE);
            return;
        }

        // Validar email único SOLO si pertenece a otro empleado
        empleadoRepository.findByEmail(email.getValue()).ifPresent(e -> {
            if (!e.getId().equals(empleado.getId())) {
                Notification.show("Ese email ya está registrado.");
                return;
            }
        });

        // Validar username único SOLO si pertenece a otro empleado
        empleadoRepository.findByUsername(username.getValue()).ifPresent(e -> {
            if (!e.getId().equals(empleado.getId())) {
                Notification.show("Ese nombre de usuario ya existe.");
                return;
            }
        });

        // Actualizar datos
        empleado.setNombre(nombre.getValue());
        empleado.setUsername(username.getValue());
        empleado.setEmail(email.getValue());
        empleado.setDireccion(direccion.getValue());
        empleado.setTelefono(telefono.getValue());
        empleado.setRol(rol.getValue());   // ← Obtención del valor del ComboBox
        empleado.setEnabled(enabled.getValue());

        // Si quiere cambiar contraseña
        if (!password.isEmpty()) {
            empleadoService.updatePassword(empleado, password.getValue());
        }

        empleadoService.save(empleado);

        Notification.show("Empleado actualizado correctamente.",
                3000, Notification.Position.MIDDLE);

        getUI().ifPresent(ui -> ui.navigate("empleado/admin/gestion-empleados"));
    }
}
