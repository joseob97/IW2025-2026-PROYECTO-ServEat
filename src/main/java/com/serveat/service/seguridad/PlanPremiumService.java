package com.serveat.service.seguridad;

import java.util.Set;

public interface PlanPremiumService {

    String obtenerCodigoPlanActual();

    Set<String> listarFeaturesActivos();

    boolean tieneFeature(String feature);

    void cambiarPlanActual(String codigoPlan);
}