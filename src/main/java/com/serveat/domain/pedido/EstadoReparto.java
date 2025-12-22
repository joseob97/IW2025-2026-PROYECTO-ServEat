package com.serveat.domain.pedido;

public enum EstadoReparto {
    NO_APLICA,        // Pedidos recoger / mesa
    PENDIENTE_ASIGNACION,
    ASIGNADO,
    EN_REPARTO,
    ENTREGADO,
    INCIDENCIA
}