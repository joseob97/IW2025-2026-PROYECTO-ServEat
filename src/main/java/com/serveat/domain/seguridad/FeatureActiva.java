package com.serveat.domain.seguridad;

import jakarta.persistence.*;

@Entity
@Table(name = "FEATURES_ACTIVAS")
public class FeatureActiva {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "feature", nullable = false, unique = true, length = 50)
    private Feature feature;

    @Column(name = "activa", nullable = false)
    private boolean activa = false;

    protected FeatureActiva() {}

    public FeatureActiva(Feature feature, boolean activa) {
        this.feature = feature;
        this.activa = activa;
    }

    public Feature getFeature() {
        return feature;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}