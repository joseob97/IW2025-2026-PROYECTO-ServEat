package com.serveat.service.notificaciones;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.administrador.estadisticas.EstadisticasSnapshot;
import com.serveat.service.seguridad.FeatureService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class GananciasScheduler {

    private final FeatureService featureService;
    private final EstadisticasService estadisticasService;
    private final EmailNotificacionService notificacionService;

    public GananciasScheduler(
            FeatureService featureService,
            EstadisticasService estadisticasService,
            EmailNotificacionService notificacionService
    ) {
        this.featureService = featureService;
        this.estadisticasService = estadisticasService;
        this.notificacionService = notificacionService;
    }

    /**
     * Notifica las ganancias del mes en curso el día 25 a las 09:10.
     */
    @Scheduled(cron = "0 10 9 25 * *")
    public void notificarGananciasMes() {

        if (!featureService.tieneFeature(Feature.NOTIFICACIONES)) {
            return;
        }

        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.withDayOfMonth(1);
        LocalDate hasta = hoy;

        EstadisticasSnapshot snapshot =
                estadisticasService.snapshotRango(desde, hasta);

        String asunto = "Preaviso fin de mes - Ganancias";

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

        notificacionService.enviarNotificacion(
                "admin@serveat.com",
                asunto,
                mensaje
        );
    }
}

