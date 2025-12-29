package com.serveat.service.pago;

import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pago.AjustePagoDTO;

import java.math.BigDecimal;

public interface AjustePagoService {

    AjustePagoDTO calcularYCrearOActualizarAjuste(Pedido pedido,
                                                  Pago pagoOriginal,
                                                  BigDecimal totalAnterior,
                                                  BigDecimal totalNuevo);
    AjustePagoDTO obtenerDetallePorCodigo(String codigoAjuste);

    AjustePagoDTO completarAjuste(String codigoAjuste, String referencia);

    AjustePagoDTO cancelarAjuste(String codigoAjuste, String motivo);

    Pedido crearBorradorPedidoParaEdicion(Pedido pedidoOriginal);
}