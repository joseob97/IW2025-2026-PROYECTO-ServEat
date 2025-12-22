package com.serveat.service.pago;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.Pedido;

import java.math.BigDecimal;

public interface PagoService {

    // Crea un pago en estado PENDIENTE asociado a un pedido y método.
    Pago iniciarPago(Pedido pedido, MetodoPago metodo);

    // Confirma un pago pendiente y guarda referencia del proveedor.
    Pago confirmarPago(Long pagoId, String referencia);

    // Marca un pago como fallido indicando el motivo.
    void marcarPagoFallido(Long pagoId, String motivo);

    // Procesa y confirma (o falla) un pago online validando los datos del método.
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