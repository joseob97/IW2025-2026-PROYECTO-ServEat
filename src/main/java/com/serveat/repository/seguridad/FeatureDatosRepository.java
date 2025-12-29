package com.serveat.repository.seguridad;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureDatos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FeatureDatosRepository extends JpaRepository<FeatureDatos, UUID> {

    Optional<FeatureDatos> findByFeature(Feature feature);

    Optional<FeatureDatos> findByCodigoDesbloqueo(String codigoDesbloqueo);
}
