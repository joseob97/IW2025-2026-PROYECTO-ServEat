package com.serveat.domain.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@MappedSuperclass
public abstract class UsuarioBase {

    // -----------------------
    // DATOS PERSONALES
    // -----------------------

    @NotBlank(message = "El nombre es obligatorio")
    @Column(nullable = false)
    protected String nombre;

    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Column(nullable = false, unique = true)
    protected String username;

    @NotBlank(message = "La contraseña es obligatoria")
    @Column(nullable = false)
    protected String password;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Column(nullable = false, unique = true)
    protected String email;

    // -----------------------
    // TELÉFONO
    // -----------------------

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^[0-9]+$", message = "El teléfono solo puede contener números")
    @Size(min = 9, max = 15, message = "El teléfono debe tener entre 9 y 15 dígitos")
    @Column(nullable = false, length = 15)
    protected String telefono;

    // -----------------------
    // DIRECCIÓN
    // -----------------------

    @NotBlank(message = "La dirección es obligatoria")
    @Column(nullable = false)
    protected String direccion;

    // -----------------------
    // GETTERS & SETTERS
    // -----------------------

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
}