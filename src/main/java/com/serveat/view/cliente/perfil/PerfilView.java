package com.serveat.view.cliente.perfil;

import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.usuario.ClienteService;
import com.serveat.service.usuario.EmpleadoService;
import com.serveat.view.layout.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

@Route(value = "perfil", layout = MainLayout.class)
@PageTitle("Mi perfil")
@RolesAllowed({
        "CLIENTE",
        "ADMIN",
        "CAMARERO",
        "COCINERO",
        "REPARTIDOR"
})
public class PerfilView extends VerticalLayout {

    private final ClienteService clienteService;
    private final EmpleadoService empleadoService;
    private final EmpleadoRepository empleadoRepository;

    private Cliente cliente;
    private Empleado empleado;

    private boolean modoEdicion = false;

    private TextField nombre;
    private TextField username;
    private EmailField email;
    private TextField telefono;
    private TextField direccion;
    private PasswordField password;

    private Button btnEditar;
    private Button btnGuardar;
    private Button btnCancelar;
    private Button btnDesactivar;
    private Button btnEliminar;

    public PerfilView(
            ClienteService clienteService,
            EmpleadoService empleadoService,
            EmpleadoRepository empleadoRepository
    ) {
        this.clienteService = clienteService;
        this.empleadoService = empleadoService;
        this.empleadoRepository = empleadoRepository;

        setMaxWidth("600px");
        setPadding(true);
        setSpacing(true);
        getStyle().set("margin", "0 auto");

        add(new H2("Mi perfil"));

        cargarUsuario();
        crearFormulario();
        actualizarModo();
    }

    /* =========================
       CARGAR USUARIO
       ========================= */
    private void cargarUsuario() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String usernameAuth = auth.getName();

        try {
            cliente = clienteService.obtenerPorUsername(usernameAuth);
        } catch (Exception e) {
            Optional<Empleado> empOpt = empleadoRepository.findByUsername(usernameAuth);
            empOpt.ifPresent(value -> empleado = value);
        }
    }

    /* =========================
       FORMULARIO
       ========================= */
    private void crearFormulario() {

        nombre = new TextField("Nombre");
        username = new TextField("Usuario");
        email = new EmailField("Email");
        telefono = new TextField("Teléfono");
        direccion = new TextField("Dirección");
        password = new PasswordField("Nueva contraseña");

        if (cliente != null) {
            nombre.setValue(cliente.getNombre());
            username.setValue(cliente.getUsername());
            email.setValue(cliente.getEmail());
            telefono.setValue(cliente.getTelefono() != null ? cliente.getTelefono() : "");
            direccion.setValue(cliente.getDireccion() != null ? cliente.getDireccion() : "");
        } else if (empleado != null) {
            nombre.setValue(empleado.getNombre());
            username.setValue(empleado.getUsername());
            email.setValue(empleado.getEmail());
            telefono.setValue(empleado.getTelefono() != null ? empleado.getTelefono() : "");
            direccion.setValue(empleado.getDireccion() != null ? empleado.getDireccion() : "");
        }

        FormLayout form = new FormLayout(
                nombre,
                username,
                email,
                telefono,
                direccion,
                password
        );

        btnEditar = new Button("✏️ Editar perfil", e -> {
            modoEdicion = true;
            actualizarModo();
        });

        btnGuardar = new Button("💾 Guardar cambios", e -> guardarCambios());

        btnCancelar = new Button("Cancelar", e -> {
            modoEdicion = false;
            restaurarValores();
            actualizarModo();
        });

        HorizontalLayout acciones = new HorizontalLayout(
                btnEditar, btnGuardar, btnCancelar
        );

        add(form, acciones);

        if (cliente != null) {
            crearAccionesCliente();
        }
    }

    /* =========================
       MODO EDICIÓN
       ========================= */
    private void actualizarModo() {

        boolean editable = modoEdicion;

        nombre.setReadOnly(!editable);
        username.setReadOnly(!editable);
        email.setReadOnly(!editable);
        telefono.setReadOnly(!editable);
        direccion.setReadOnly(!editable);

        password.setVisible(editable);

        btnEditar.setVisible(!editable);
        btnGuardar.setVisible(editable);
        btnCancelar.setVisible(editable);
    }

    /* =========================
       GUARDAR CAMBIOS
       ========================= */
    private void guardarCambios() {

        if (!validarCampos()) return;

        try {
            if (cliente != null) {

                cliente.setNombre(nombre.getValue().trim());
                cliente.setUsername(username.getValue().trim());
                cliente.setEmail(email.getValue().trim());
                cliente.setTelefono(telefono.getValue().trim());
                cliente.setDireccion(direccion.getValue().trim());

                if (!password.isEmpty()) {
                    cliente.setPassword(password.getValue());
                }

                clienteService.guardar(cliente);

            } else if (empleado != null) {

                empleado.setNombre(nombre.getValue().trim());
                empleado.setUsername(username.getValue().trim());
                empleado.setEmail(email.getValue().trim());
                empleado.setTelefono(telefono.getValue().trim());
                empleado.setDireccion(direccion.getValue().trim());

                if (!password.isEmpty()) {
                    empleado.setPassword(password.getValue());
                }

                empleadoService.guardar(empleado);
            }

            Notification.show("Perfil actualizado correctamente", 3000, Position.MIDDLE);
            modoEdicion = false;
            actualizarModo();

        } catch (Exception e) {
            Notification.show(e.getMessage(), 4000, Position.MIDDLE);
        }
    }

    /* =========================
       VALIDACIONES
       ========================= */
    private boolean validarCampos() {

        nombre.setInvalid(false);
        username.setInvalid(false);
        email.setInvalid(false);
        telefono.setInvalid(false);

        if (nombre.isEmpty()) {
            nombre.setErrorMessage("Campo obligatorio");
            nombre.setInvalid(true);
            return false;
        }

        if (username.isEmpty()) {
            username.setErrorMessage("Campo obligatorio");
            username.setInvalid(true);
            return false;
        }

        if (email.isEmpty() || !email.getValue().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            email.setErrorMessage("Email no válido");
            email.setInvalid(true);
            return false;
        }

        if (!telefono.getValue().matches("\\d*")) {
            telefono.setErrorMessage("Solo números");
            telefono.setInvalid(true);
            return false;
        }

        return true;
    }

    private void restaurarValores() {

        if (cliente != null) {
            nombre.setValue(cliente.getNombre());
            username.setValue(cliente.getUsername());
            email.setValue(cliente.getEmail());
            telefono.setValue(cliente.getTelefono() != null ? cliente.getTelefono() : "");
            direccion.setValue(cliente.getDireccion() != null ? cliente.getDireccion() : "");
        } else if (empleado != null) {
            nombre.setValue(empleado.getNombre());
            username.setValue(empleado.getUsername());
            email.setValue(empleado.getEmail());
            telefono.setValue(empleado.getTelefono() != null ? empleado.getTelefono() : "");
            direccion.setValue(empleado.getDireccion() != null ? empleado.getDireccion() : "");
        }

        password.clear();
    }

    /* =========================
       ACCIONES CLIENTE
       ========================= */
    private void crearAccionesCliente() {

        btnDesactivar = new Button("Desactivar cuenta");
        btnDesactivar.getStyle().set("color", "orange");

        btnEliminar = new Button("Eliminar cuenta");
        btnEliminar.getStyle().set("color", "red");

        btnDesactivar.addClickListener(e -> confirmarDesactivacion());
        btnEliminar.addClickListener(e -> confirmarEliminacion());

        HorizontalLayout accionesPeligro = new HorizontalLayout(
                btnDesactivar, btnEliminar
        );
        accionesPeligro.getStyle().set("margin-top", "30px");

        add(accionesPeligro);
    }

    private void confirmarDesactivacion() {
        ConfirmDialog dialog = new ConfirmDialog(
                "Desactivar cuenta",
                "Para volver a activarla deberás contactar con soporte.",
                "Desactivar",
                e -> {
                    clienteService.desactivar(cliente);
                    UI.getCurrent().getPage().setLocation("/logout");
                },
                "Cancelar",
                e -> {}
        );
        dialog.open();
    }

    private void confirmarEliminacion() {
        ConfirmDialog dialog = new ConfirmDialog(
                "Eliminar cuenta definitivamente",
                "Esta acción es irreversible.",
                "Eliminar",
                e -> {
                    clienteService.eliminar(cliente);
                    UI.getCurrent().getPage().setLocation("/logout");
                },
                "Cancelar",
                e -> {}
        );
        dialog.open();
    }
}
