package com.serveat.domain.pedido;

public enum EstadoCocina {
    PENDIENTE_ACEPTACION,  // camarero confirmó, cocina aún no lo ha aceptado
    ACEPTADO,              // cocina lo acepta (ya no se puede cancelar/rectificar)
    EN_PREPARACION,
    LISTO,
    CANCELADO
}