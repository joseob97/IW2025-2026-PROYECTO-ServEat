package com.serveat.service.administrador.estadisticas;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class EstadisticasScheduler {

    private final EstadisticasService estadisticasService;

    public EstadisticasScheduler(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;
    }

    /**
     * Refresco periódico: limpia cachés en background para que el siguiente acceso recalcule.
     */
    @Scheduled(cron = "0 */10 * * * *") // cada 10 minutos
    public void refrescoPeriodico() {
        estadisticasService.recalcularEstadisticasAsync();
    }

    /**
     * Precalcula un rango habitual (últimos 30 días) por la noche.
     */
    @Scheduled(cron = "0 5 2 * * *") // 02:05 cada día
    public void precalcularUltimos30Dias() {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(30);
        estadisticasService.snapshotRango(desde, hasta);
    }

    /**
     * Gancho de fin de mes: precalcula el rango del mes en curso.
     */
    @Scheduled(cron = "0 0 9 25 * *") // día 25 a las 09:00
    public void preavisoFinDeMes() {
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.withDayOfMonth(1);
        LocalDate hasta = hoy;
        estadisticasService.snapshotRango(desde, hasta);
    }
}