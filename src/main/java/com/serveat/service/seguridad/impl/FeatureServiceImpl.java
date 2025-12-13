package com.serveat.service.seguridad.impl;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureActiva;
import com.serveat.repository.seguridad.FeatureActivaRepository;
import com.serveat.service.seguridad.FeatureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class FeatureServiceImpl implements FeatureService {

    private final FeatureActivaRepository featureRepo;

    public FeatureServiceImpl(FeatureActivaRepository featureRepo) {
        this.featureRepo = featureRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneFeature(Feature feature) {
        return featureRepo.findByFeature(feature)
                .map(FeatureActiva::isActiva)
                .orElse(false);
    }

    @Override
    public void activarFeature(Feature feature) {
        FeatureActiva fa = featureRepo.findByFeature(feature)
                .orElseGet(() -> new FeatureActiva(feature));

        fa.setActiva(true);
        fa.setActivadaEn(LocalDateTime.now());

        featureRepo.save(fa);
    }

    @Override
    public void desactivarFeature(Feature feature) {
        FeatureActiva fa = featureRepo.findByFeature(feature)
                .orElseThrow(() -> new IllegalStateException("Feature no inicializada"));

        fa.setActiva(false);
        fa.setActivadaEn(null);

        featureRepo.save(fa);
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
        return activos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Feature> listarTodas() {
        return featureRepo.findAll()
                .stream()
                .map(FeatureActiva::getFeature)
                .toList();
    }
}