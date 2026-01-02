package com.serveat.service.seguridad.impl;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureActiva;
import com.serveat.repository.seguridad.FeatureActivaRepository;
import com.serveat.service.seguridad.FeatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class FeatureServiceImpl implements FeatureService {

    private static final Logger log =
            LoggerFactory.getLogger(FeatureServiceImpl.class);

    private final FeatureActivaRepository featureRepo;

    public FeatureServiceImpl(FeatureActivaRepository featureRepo) {
        this.featureRepo = featureRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneFeature(Feature feature) {
        boolean activa = featureRepo.findByFeature(feature)
                .map(FeatureActiva::isActiva)
                .orElse(false);

        log.debug("Consulta feature {} → activa={}", feature, activa);
        return activa;
    }

    @Override
    public void activarFeature(Feature feature) {
        FeatureActiva fa = featureRepo.findByFeature(feature)
                .orElseGet(() -> {
                    log.info("Inicializando feature {}", feature);
                    return new FeatureActiva(feature);
                });

        if (fa.isActiva()) {
            log.warn("Intento de activar feature ya activa: {}", feature);
            return;
        }

        fa.setActiva(true);
        fa.setActivadaEn(LocalDateTime.now());

        featureRepo.save(fa);

        log.info("Feature {} ACTIVADA correctamente", feature);
    }

    @Override
    public void desactivarFeature(Feature feature) {
        FeatureActiva fa = featureRepo.findByFeature(feature)
                .orElseThrow(() -> {
                    log.error("Intento de desactivar feature no inicializada: {}", feature);
                    return new IllegalStateException("Feature no inicializada");
                });

        if (!fa.isActiva()) {
            log.warn("Intento de desactivar feature ya inactiva: {}", feature);
            return;
        }

        fa.setActiva(false);
        fa.setActivadaEn(null);

        featureRepo.save(fa);

        log.info("Feature {} DESACTIVADA correctamente", feature);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Feature> listarFeaturesActivos() {
        Set<Feature> activos = new LinkedHashSet<>();

        for (FeatureActiva fa : featureRepo.findAll()) {
            if (fa.isActiva()) {
                activos.add(fa.getFeature());
            }
        }

        log.debug("Listado de features activas: {}", activos);
        return activos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feature> listarTodas() {
        List<Feature> todas = featureRepo.findAll()
                .stream()
                .map(FeatureActiva::getFeature)
                .toList();

        log.debug("Listado completo de features: {}", todas);
        return todas;
    }
}
