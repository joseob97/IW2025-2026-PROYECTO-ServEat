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

    // Campos comunes
    private TextField nombre;
    private TextField username;
    private EmailField email;
    private TextField telefono;
    private TextField direccion;
    private PasswordField password;

    // Botones
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

        // SOLO CLIENTES → botones peligrosos
        if (cliente != null) {
            crearAccionesCliente();
        }
    }

    /* =========================
       BOTONES CLIENTE
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
                empleadoService.updatePassword(empleado, password.getValue());
            }

            empleadoService.save(empleado);
        }

        Notification.show("Perfil actualizado correctamente");
        modoEdicion = false;
        actualizarModo();
    }

    /* =========================
       CONFIRMACIONES CLIENTE
       ========================= */
    private void confirmarDesactivacion() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Desactivar cuenta");
        dialog.setText(
                "¿Seguro que deseas desactivar tu cuenta?\n\n" +
                        "Para volver a activarla deberás contactar con soporte."
        );
        dialog.setConfirmText("Desactivar");
        dialog.setCancelText("Cancelar");

        dialog.addConfirmListener(e -> {
            clienteService.desactivar(cliente);
            Notification.show("Cuenta desactivada");
            UI.getCurrent().getPage().setLocation("/logout");
        });

        dialog.open();
    }

    private void confirmarEliminacion() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Eliminar cuenta definitivamente");
        dialog.setText(
                "Esta acción es IRREVERSIBLE.\n\n" +
                        "Todos tus datos serán eliminados y no podrán recuperarse."
        );
        dialog.setConfirmText("Eliminar definitivamente");
        dialog.setCancelText("Cancelar");

        dialog.addConfirmListener(e -> {
            clienteService.eliminar(cliente);
            Notification.show("Cuenta eliminada");
            UI.getCurrent().getPage().setLocation("/logout");
        });

        dialog.open();
    }

    /* =========================
       VALIDACIONES
       ========================= */
    private boolean validarCampos() {

        if (nombre.isEmpty() || username.isEmpty() || email.isEmpty()) {
            Notification.show("No puede haber campos vacíos");
            return false;
        }

        String emailValue = email.getValue();
        if (emailValue == null || !emailValue.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            email.setInvalid(true);
            email.setErrorMessage("Email no válido");
            return false;
        }

        if (!telefono.getValue().matches("\\d*")) {
            telefono.setInvalid(true);
            telefono.setErrorMessage("El teléfono solo puede contener números");
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
}

