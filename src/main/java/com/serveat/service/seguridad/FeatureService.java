package com.serveat.service.seguridad;

import com.serveat.domain.seguridad.Feature;

import java.util.List;
import java.util.Set;

public interface FeatureService {

    boolean tieneFeature(Feature feature);

    void activarFeature(Feature feature);

    void desactivarFeature(Feature feature);

    Set<Feature> listarFeaturesActivos();

    List<Feature> listarTodas();

    boolean fueActivadaAlgunaVez(Feature feature);
}