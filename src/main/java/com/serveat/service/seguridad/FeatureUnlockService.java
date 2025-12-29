package com.serveat.service.seguridad;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureActiva;
import com.serveat.domain.seguridad.FeatureDatos;
import com.serveat.repository.seguridad.FeatureActivaRepository;
import com.serveat.repository.seguridad.FeatureDatosRepository;
import com.serveat.service.notificaciones.PushNotificacionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class FeatureUnlockService {

    private final FeatureDatosRepository featureDatosRepository;
    private final FeatureActivaRepository featureActivaRepository;
    private final PushNotificacionService pushNotificacionService;

    public FeatureUnlockService(FeatureDatosRepository featureDatosRepository,
                                FeatureActivaRepository featureActivaRepository,
                                PushNotificacionService pushNotificacionService) {
        this.featureDatosRepository = featureDatosRepository;
        this.featureActivaRepository = featureActivaRepository;
        this.pushNotificacionService = pushNotificacionService;
    }

    /**
     * Simula el pago de una feature y envía el código por notificación push.
     * Solo ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void simularPagoYEnviarCodigo(Feature feature) {

        FeatureDatos datos = featureDatosRepository.findByFeature(feature)
                .orElseThrow(() -> new IllegalStateException("No existen datos para la feature " + feature));

        if (featureActivaRepository.existsByFeature(feature)) {
            throw new IllegalStateException("La feature ya está activa");
        }

        // Simulación de pago correcta
        String mensaje = """
                Pago realizado correctamente.
                
                Código de desbloqueo para la feature %s:
                %s
                """.formatted(feature.name(), datos.getCodigoDesbloqueo());

        pushNotificacionService.enviarNotificacion(
                "Desbloqueo de funcionalidad",
                mensaje
        );
    }

    /**
     * Valida el código introducido y activa la feature si es correcto.
     * Solo ADMIN.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public void validarCodigoYActivar(Feature feature, String codigoIntroducido) {

        if (codigoIntroducido == null || codigoIntroducido.isBlank()) {
            throw new IllegalArgumentException("El código no puede estar vacío");
        }

        FeatureDatos datos = featureDatosRepository.findByFeature(feature)
                .orElseThrow(() -> new IllegalStateException("No existen datos para la feature " + feature));

        if (!datos.getCodigoDesbloqueo().equals(codigoIntroducido)) {
            throw new IllegalArgumentException("Código de desbloqueo incorrecto");
        }

        if (featureActivaRepository.existsByFeature(feature)) {
            throw new IllegalStateException("La feature ya está activa");
        }

        FeatureActiva activa = new FeatureActiva(feature);
        activa.setActiva(true);
        activa.setActivadaEn(LocalDateTime.now());

        featureActivaRepository.save(activa);
    }
}
