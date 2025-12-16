package com.serveat.domain.pedido;

public enum EstadoPedido {

    EN_CURSO,        // Pedido creado, editable
    EN_PREPARACION,  // Cocina trabajando
    LISTO,           // Listo para servir
    SERVIDO,         // Entregado en mesa
    CANCELADO        // Cancelado por camarero
}