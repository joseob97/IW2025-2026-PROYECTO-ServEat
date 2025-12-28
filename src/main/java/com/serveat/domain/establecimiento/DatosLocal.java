package com.serveat.domain.establecimiento;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "datos_local")
public class DatosLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ========= INFORMACIÓN ========= */

    @NotBlank(message = "El nombre del local es obligatorio")
    @Column(nullable = false)
    private String nombreLocal;

    @Size(max = 1000, message = "La descripción no puede superar los 1000 caracteres")
    @Column(length = 1000)
    private String descripcion;

    // 👉 NUEVO segundo bloque de información
    @Size(max = 1000, message = "La segunda descripción no puede superar los 1000 caracteres")
    @Column(length = 1000)
    private String descripcion2;

    /* ========= CONTACTO ========= */

    @Pattern(
            regexp = "^[0-9]{9,15}$",
            message = "El teléfono debe contener solo números (9 a 15 dígitos)"
    )
    private String telefono;

    @Email(message = "El email no tiene un formato válido")
    private String email;

    @Size(max = 255)
    private String direccion;

    private String horario;

    public DatosLocal() {
    }

    /* ========= GETTERS & SETTERS ========= */

    public Long getId() {
        return id;
    }

    public String getNombreLocal() {
        return nombreLocal;
    }

    public void setNombreLocal(String nombreLocal) {
        this.nombreLocal = nombreLocal;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion2() {
        return descripcion2;
    }

    public void setDescripcion2(String descripcion2) {
        this.descripcion2 = descripcion2;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }
}

