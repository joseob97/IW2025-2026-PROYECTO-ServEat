package com.serveat.repository.seguridad;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureActiva;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureActivaRepository extends JpaRepository<FeatureActiva, Feature> {
}