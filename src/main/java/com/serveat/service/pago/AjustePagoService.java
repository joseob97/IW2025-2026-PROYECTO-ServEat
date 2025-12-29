package com.serveat.service.pago;

import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pago.dto.AjustePagoDTO;

import java.math.BigDecimal;

public interface AjustePagoService {

    AjustePagoDTO calcularYCrearOActualizarAjuste(Pedido pedido,
                                                  Pago pagoOriginal,
                                                  BigDecimal totalAnterior,
                                                  BigDecimal totalNuevo);
}