package com.serveat.domain.menu;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "MENUS")
public class Menu {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    @ManyToMany
    @JoinTable(
            name = "MENU_PRODUCTOS",
            joinColumns = @JoinColumn(name = "menu_id"),
            inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private List<Producto> productos;

    @Column(name = "precio_fijo", nullable = false)
    private BigDecimal precioFijo;

    private boolean activo = true;

    // Getters y setters

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<Producto> getProductos() {
        return productos;
    }

    public void setProductos(List<Producto> productos) {
        this.productos = productos;
    }

    public BigDecimal getPrecioFijo() {
        return precioFijo;
    }

    public void setPrecioFijo(BigDecimal precioFijo) {
        this.precioFijo = precioFijo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
