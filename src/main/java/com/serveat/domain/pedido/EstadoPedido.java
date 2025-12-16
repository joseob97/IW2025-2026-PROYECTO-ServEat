package com.serveat.domain.pedido;

public enum EstadoPedido {

    EN_CURSO,      // Camarero creando / editando
    EN_COCINA,     // Confirmado → cocina lo ve
    EN_PREPARACION,// Cocinero empieza a cocinar
    LISTO,         // Cocina termina
    SERVIDO,       // Camarero entrega
    CANCELADO
}