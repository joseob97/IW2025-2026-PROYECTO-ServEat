package com.serveat.service.seguridad.impl;

import com.serveat.service.seguridad.PlanPremiumService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@Transactional
public class PlanPremiumServiceImpl implements PlanPremiumService {

    // DEMO: luego lo persistimos en BD (PlanPremiumRepository)
    private String planActual = "BASICO";

    @Override
    public String obtenerCodigoPlanActual() {
        return planActual;
    }

    @Override
    public Set<String> listarFeaturesActivos() {
        Set<String> f = new LinkedHashSet<>();
        f.add("PAGO_EFECTIVO"); // siempre

        if ("PRO".equals(planActual)) {
            f.add("PROMOCIONES");
            f.add("ESTADISTICAS");
            f.add("PAGO_TARJETA");
        }

        return f;
    }

    @Override
    public boolean tieneFeature(String feature) {
        return listarFeaturesActivos().contains(feature);
    }

    @Override
    public void cambiarPlanActual(String codigoPlan) {
        if (!"BASICO".equals(codigoPlan) && !"PRO".equals(codigoPlan)) {
            throw new IllegalArgumentException("Plan no válido: " + codigoPlan);
        }
        this.planActual = codigoPlan;
    }
}