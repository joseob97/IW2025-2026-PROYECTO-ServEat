package com.serveat.service.estadisticas;

import java.math.BigDecimal;

public interface EstadisticasService {

    long totalPedidos();

    long pedidosConfirmados();
    long pedidosCancelados();

    long pagosConfirmados();

    BigDecimal totalFacturado();
}