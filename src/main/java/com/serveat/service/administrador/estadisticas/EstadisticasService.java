package com.serveat.service.administrador.estadisticas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface EstadisticasService {

    /* Devuelve un resumen agregado (KPIs) para el rango de fechas indicado. */
    EstadisticasSnapshot snapshotRango(LocalDate desde, LocalDate hasta);

    /* Obtiene el ranking de productos por unidades vendidas en el rango indicado (limitado). */
    List<Map<String, Object>> topProductosPorUnidades(LocalDate desde, LocalDate hasta, int limit);

    /* Obtiene el ranking de productos por facturación en el rango indicado (limitado). */
    List<Map<String, Object>> topProductosPorFacturacion(LocalDate desde, LocalDate hasta, int limit);

    /* Devuelve una serie mensual lista para representación: mes, valor y marca del máximo. */
    List<Map<String, Object>> serieMensualVista(int yearDesde, int yearHasta, String tipo);

    /* Calcula la serie mensual de facturación por YearMonth dentro del rango de años. */
    Map<YearMonth, BigDecimal> serieMensualFacturacion(int yearDesde, int yearHasta);

    /* Calcula la serie mensual de unidades por YearMonth dentro del rango de años. */
    Map<YearMonth, Long> serieMensualUnidades(int yearDesde, int yearHasta);

    /* Devuelve los años disponibles para selección en filtros de la interfaz. */
    List<Integer> añosDisponibles();

    /* Devuelve un mensaje estándar para escenarios sin resultados. */
    String mensajeSinResultados();

    /* Invalida cachés de estadísticas para forzar recálculo bajo demanda. */
    void recalcularEstadisticasAsync();

    /* Genera un resumen de caja para el día actual */
    Map<String, Object> generarCierreCajaDiario();

    /* Genera un resumen de caja para el turno actual (desde última apertura) */
    Map<String, Object> generarCierreCajaTurno();
}