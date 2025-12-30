package com.serveat.service.caja;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.seguridad.FeatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Component
public class CajaScheduler {

    private static final Logger logger = LoggerFactory.getLogger(CajaScheduler.class);

    private final EstadoCajaService estadoCajaService;
    private final CierreCajaService cierreCajaService;
    private final EstadisticasService estadisticasService;
    private final FeatureService featureService;

    public CajaScheduler(EstadoCajaService estadoCajaService,
                         CierreCajaService cierreCajaService,
                         EstadisticasService estadisticasService,
                         FeatureService featureService) {
        this.estadoCajaService = estadoCajaService;
        this.cierreCajaService = cierreCajaService;
        this.estadisticasService = estadisticasService;
        this.featureService = featureService;
    }

    // Apertura automática a las 13:00 todos los días
    @Scheduled(cron = "0 0 13 * * *")
    public void abrirCajaAutomaticamente() {
        if (!featureService.tieneFeature(Feature.CIERRE_CAJA)) {
            return;
        }

        try {
            if (!estadoCajaService.isCajaAbierta()) {
                estadoCajaService.abrirCaja("SISTEMA_AUTO");
                logger.info("Caja abierta automáticamente a las 13:00.");
            } else {
                logger.info("Intento de apertura automática omitido: La caja ya estaba abierta.");
            }
        } catch (Exception e) {
            logger.error("Error en la apertura automática de caja", e);
        }
    }

    // Cierre automático a las 00:00 (medianoche) todos los días
    @Scheduled(cron = "0 0 0 * * *")
    public void cerrarCajaAutomaticamente() {
        if (!featureService.tieneFeature(Feature.CIERRE_CAJA)) {
            return;
        }

        try {
            if (estadoCajaService.isCajaAbierta()) {
                // 1. Calcular totales del turno
                Map<String, Object> resultados = estadisticasService.generarCierreCajaTurno();
                
                BigDecimal total = (BigDecimal) resultados.get("total");
                BigDecimal paypal = (BigDecimal) resultados.get("paypal");
                BigDecimal efectivo = (BigDecimal) resultados.get("efectivo");
                BigDecimal tarjeta = (BigDecimal) resultados.get("tarjeta");

                // 2. Guardar informe de cierre (usamos fecha de ayer porque son las 00:00 del día siguiente)
                // O usamos LocalDate.now() si consideramos que el cierre pertenece al día que empieza.
                // Generalmente, un cierre a las 00:00 cierra el día anterior.
                LocalDate fechaCierre = LocalDate.now().minusDays(1);
                
                try {
                    cierreCajaService.cerrarCaja(fechaCierre, total, efectivo, tarjeta, paypal);
                } catch (IllegalStateException e) {
                    logger.warn("Ya existía un informe de cierre para la fecha {}. Se omite el guardado del informe.", fechaCierre);
                }

                // 3. Cerrar caja (estado)
                estadoCajaService.cerrarCaja("SISTEMA_AUTO");
                logger.info("Caja cerrada automáticamente a las 00:00.");
            } else {
                logger.info("Intento de cierre automático omitido: La caja ya estaba cerrada.");
            }
        } catch (Exception e) {
            logger.error("Error en el cierre automático de caja", e);
        }
    }
}
