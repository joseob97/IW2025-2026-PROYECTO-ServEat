package com.serveat.service.estadisticas;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface EstadisticasService {

    long totalPedidos();

    long pedidosConfirmados();

    long pedidosCancelados();

    long pagosConfirmados();

    BigDecimal totalFacturado();

    // Devuelve top productos por unidades vendidas: [{producto=..., unidades=...}, ...]
    List<Map<String, Object>> topProductosPorUnidades(int limit);

    // Devuelve top productos por facturación: [{producto=..., total=...}, ...]
    List<Map<String, Object>> topProductosPorFacturacion(int limit);

    // Devuelve resumen de cocina: {PENDIENTE_ACEPTACION=..., EN_PREPARACION=..., LISTO=...}
    Map<String, Long> resumenEstadosCocina();
}