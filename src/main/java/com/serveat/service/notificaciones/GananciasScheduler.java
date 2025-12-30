package com.serveat.service.notificaciones;

import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.administrador.estadisticas.EstadisticasSnapshot;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class GananciasScheduler {

    private final EstadisticasService estadisticasService;
    private final NotificacionGananciasService notificacionGananciasService;

    public GananciasScheduler(
            EstadisticasService estadisticasService,
            NotificacionGananciasService notificacionGananciasService
    ) {
        this.estadisticasService = estadisticasService;
        this.notificacionGananciasService = notificacionGananciasService;
    }

    @Scheduled(cron = "0 10 9 25 * *")
    public void notificarGananciasMes() {

        EstadisticasSnapshot snapshot =
                estadisticasService.snapshotRango(
                        LocalDate.now().withDayOfMonth(1),
                        LocalDate.now()
                );

        notificacionGananciasService.notificarGanancias(snapshot);
    }
}



