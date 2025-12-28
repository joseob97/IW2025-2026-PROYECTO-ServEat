package com.serveat.service.notificaciones;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.administrador.estadisticas.EstadisticasSnapshot;
import com.serveat.service.seguridad.FeatureService;
import org.springframework.stereotype.Service;

@Service
public class NotificacionGananciasService {

    private final FeatureService featureService;
    private final EmailNotificacionService emailService;
    private final PushNotificacionService pushService;

    public NotificacionGananciasService(
            FeatureService featureService,
            EmailNotificacionService emailService,
            PushNotificacionService pushService
    ) {
        this.featureService = featureService;
        this.emailService = emailService;
        this.pushService = pushService;
    }

    public void notificarGanancias(EstadisticasSnapshot snapshot) {

        if (!featureService.tieneFeature(Feature.NOTIFICACIONES)) {
            return;
        }

        String titulo = "Ganancias del mes";

        String mensaje = """
                Ganancias del mes en curso

                Periodo: %s → %s
                Facturación acumulada: %s €

                ServEat
                """.formatted(
                snapshot.getDesde(),
                snapshot.getHasta(),
                snapshot.getTotalFacturado()
        );

        // PUSH
        pushService.crearNotificacion(titulo, mensaje);

        // EMAIL
        emailService.enviarNotificacion(
                "admin@serveat.com",
                titulo,
                mensaje
        );
    }
}
