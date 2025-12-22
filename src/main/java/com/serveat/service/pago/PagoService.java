package com.serveat.service.pago;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.Pedido;

import java.math.BigDecimal;

public interface PagoService {

    Pago iniciarPago(Pedido pedido, MetodoPago metodo);

    Pago confirmarPago(Long pagoId, String referencia);

    void marcarPagoFallido(Long pagoId, String motivo);

    Pago procesarPagoOnline(Pedido pedidoCreado,
                            MetodoPago metodo,
                            String cardNumber,
                            String cardHolder,
                            String cardExpiryMMYY,
                            String cardCvv,
                            String paypalEmail,
                            String paypalPassword,
                            BigDecimal efectivoPagaCon);
}