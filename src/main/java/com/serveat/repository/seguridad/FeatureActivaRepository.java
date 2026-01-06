package com.serveat.repository.seguridad;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureActiva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeatureActivaRepository extends JpaRepository<FeatureActiva, UUID> {
    Optional<FeatureActiva> findByFeature(Feature feature);

    boolean existsByFeature(Feature feature);
    boolean existsByFeatureAndActivaTrue(Feature feature);

}