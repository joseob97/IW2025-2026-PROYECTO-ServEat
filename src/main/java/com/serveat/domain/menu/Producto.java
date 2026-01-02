package com.serveat.domain.menu;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "PRODUCTOS",
        indexes = {
                @Index(name = "idx_producto_nombre", columnList = "nombre"),
                @Index(name = "idx_producto_categoria", columnList = "categoria_id")
        }
)
public class Producto {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    private String nombre;
    private String descripcion;
    private BigDecimal precio;

    @Column(unique = true, nullable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoIngrediente> ingredientes = new ArrayList<>();

    // =======================
    // GETTERS Y SETTERS
    // =======================

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public List<ProductoIngrediente> getIngredientes() { return ingredientes; }

    public void setIngredientes(List<ProductoIngrediente> ingredientes) {
        this.ingredientes = (ingredientes == null) ? new ArrayList<>() : ingredientes;
    }
}
