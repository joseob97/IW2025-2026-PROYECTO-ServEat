package com.serveat.domain.seguridad;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "FEATURES_ACTIVAS")
public class FeatureActiva {

    @Id
    @GeneratedValue
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    private Feature feature;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "activada_en")
    private LocalDateTime activadaEn;

    protected FeatureActiva() {}

    public FeatureActiva(Feature feature) {
        this.feature = feature;
        this.activa = false;
    }

    public Feature getFeature() {
        return feature;
    }

    public void setFeature(Feature feature) {
        this.feature = feature;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public LocalDateTime getActivadaEn() {
        return activadaEn;
    }

    public void setActivadaEn(LocalDateTime activadaEn) {
        this.activadaEn = activadaEn;
    }
}