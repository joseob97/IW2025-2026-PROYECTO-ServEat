package com.serveat.service.pago;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.Pedido;

public interface PagoService {

    Pago iniciarPago(Pedido pedido, MetodoPago metodo);

    Pago confirmarPago(Long pagoId, String referencia);

    void marcarPagoFallido(Long pagoId, String motivo);
}