package com.serveat.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(
        name = "empleados",
        indexes = {
                @Index(name = "idx_empleado_username", columnList = "username"),
                @Index(name = "idx_empleado_email", columnList = "email"),
                @Index(name = "idx_empleado_rol", columnList = "rol"),
                @Index(name = "idx_empleado_enabled", columnList = "enabled")
        }
)
public class Empleado extends UsuarioBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String rol; // CAMARERO, COCINERO, ADMIN, REPARTIDOR

    @Column(nullable = false)
    private Boolean enabled = true;

    public Empleado() {}

    public Empleado(
            String nombre,
            String username,
            String password,
            String telefono,
            String email,
            String direccion,
            String rol,
            boolean enabled
    ) {
        this.nombre = nombre;
        this.username = username;
        this.password = password;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.rol = rol;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getRol() {
        return rol;
    }
    public void setRol(String rol) {
        this.rol = rol;
    }

    public boolean isEnabled() {
        return enabled != null && enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}