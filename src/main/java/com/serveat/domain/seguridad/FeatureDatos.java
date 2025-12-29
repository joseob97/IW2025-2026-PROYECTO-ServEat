package com.serveat.domain.seguridad;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "FEATURES_DATOS")
public class FeatureDatos {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private Feature feature;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "codigo_desbloqueo", nullable = false, length = 5)
    private String codigoDesbloqueo;

    protected FeatureDatos() {}

    public FeatureDatos(Feature feature, BigDecimal precio, String codigoDesbloqueo) {
        this.feature = feature;
        this.precio = precio;
        this.codigoDesbloqueo = codigoDesbloqueo;
    }

    public UUID getId() {
        return id;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public String getCodigoDesbloqueo() {
        return codigoDesbloqueo;
    }

    public void setCodigoDesbloqueo(String codigoDesbloqueo) {
        this.codigoDesbloqueo = codigoDesbloqueo;
    }
}
