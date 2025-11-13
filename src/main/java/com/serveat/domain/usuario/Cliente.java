package com.serveat.domain.usuario;

import com.serveat.domain.comun.BaseEntity;
import jakarta.persistence.Entity;

@Entity
public class Cliente extends BaseEntity {

    private String nombre;
    private String email;

    // ➕ Campos para login
    private String username;
    private String password;
    private String rol;

    public Cliente() {
    }

    public Cliente(String nombre, String email, String username, String password) {
        this.nombre = nombre;
        this.email = email;
        this.username = username;
        this.password = password;
        this.rol = "CLIENTE";
    }

    // Getters y setters

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
