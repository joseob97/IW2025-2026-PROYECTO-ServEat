package com.serveat.domain.menu;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Type;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "CATEGORIAS")
public class Categoria {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    private String nombre;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}