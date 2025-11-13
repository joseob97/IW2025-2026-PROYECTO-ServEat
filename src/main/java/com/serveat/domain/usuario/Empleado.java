package com.serveat.domain.usuario;

import com.serveat.domain.comun.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Empleado extends BaseEntity {
    private String nombre; // Para mostrar su nombre
    @Column(unique = true, nullable = false)
    private String username; // El usuario para iniciar sesión
    private String password; // La contraseña (se guardará encriptada)
    private String rol; // Ej: "CAMARERO", "COCINERO", "ADMINISTRADOR"

    public Empleado() {
        // Constructor vacío requerido por JPA
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }
}