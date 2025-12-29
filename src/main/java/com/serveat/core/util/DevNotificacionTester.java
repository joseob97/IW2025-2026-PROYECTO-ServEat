package com.serveat.core.util;

import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.administrador.estadisticas.EstadisticasSnapshot;
import com.serveat.service.notificaciones.NotificacionGananciasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Profile("dev")
@Component
public class DevNotificacionTester {

    private static final Logger log =
            LoggerFactory.getLogger(DevNotificacionTester.class);

    private final EstadisticasService estadisticasService;
    private final NotificacionGananciasService notificacionGananciasService;

    public DevNotificacionTester(
            EstadisticasService estadisticasService,
            NotificacionGananciasService notificacionGananciasService
    ) {
        this.estadisticasService = estadisticasService;
        this.notificacionGananciasService = notificacionGananciasService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void lanzarPruebaDev() {

        log.info(">>> [DEV] Lanzando prueba inmediata de notificaciones de ganancias <<<");

        EstadisticasSnapshot snapshot =
                estadisticasService.snapshotRango(
                        LocalDate.now().withDayOfMonth(1),
                        LocalDate.now()
                );

        notificacionGananciasService.notificarGanancias(snapshot);
    }
}

