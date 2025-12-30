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

import java.math.BigDecimal;
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

    @PreAuthorize("hasRole('ADMIN')")
    public BigDecimal obtenerPrecioFeature(Feature feature) {
        return featureDatosRepository.findByFeature(feature)
                .orElseThrow(() -> new IllegalStateException(
                        "No existen datos para la feature " + feature))
                .getPrecio();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public boolean isFeaturePagada(Feature feature) {

        FeatureDatos datos = featureDatosRepository.findByFeature(feature)
                .orElseThrow(() -> new IllegalStateException(
                        "No existen datos para la feature " + feature));

        return datos.isPagada();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public String simularPagoYObtenerCodigo(Feature feature) {

        FeatureDatos datos = featureDatosRepository.findByFeature(feature)
                .orElseThrow(() -> new IllegalStateException(
                        "No existen datos para la feature " + feature));

        if (featureActivaRepository.existsByFeature(feature)) {
            throw new IllegalStateException("La feature ya está activa");
        }

        if (datos.isPagada()) {
            throw new IllegalStateException("La feature ya ha sido pagada");
        }

        // Marcar como pagada
        datos.setPagada(true);
        featureDatosRepository.save(datos);

        String codigo = datos.getCodigoDesbloqueo();

        // 👉 SOLO enviar push si NOTIFICACIONES está activa
        if (featureActivaRepository.existsByFeature(Feature.NOTIFICACIONES)) {

            String mensaje = """
                Pago realizado correctamente.
                
                Código de desbloqueo para %s:
                %s
                """.formatted(feature.name(), codigo);

            pushNotificacionService.enviarNotificacion(
                    "Desbloqueo de funcionalidad",
                    mensaje
            );
        }

        // 👉 SIEMPRE devolver el código (la vista decide qué hacer con él)
        return codigo;
    }


    @PreAuthorize("hasRole('ADMIN')")
    public void validarCodigoYActivar(Feature feature, String codigoIntroducido) {

        if (codigoIntroducido == null || codigoIntroducido.isBlank()) {
            throw new IllegalArgumentException("El código no puede estar vacío");
        }

        FeatureDatos datos = featureDatosRepository.findByFeature(feature)
                .orElseThrow(() -> new IllegalStateException(
                        "No existen datos para la feature " + feature));

        if (!datos.isPagada()) {
            throw new IllegalStateException("La feature aún no ha sido pagada");
        }

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
