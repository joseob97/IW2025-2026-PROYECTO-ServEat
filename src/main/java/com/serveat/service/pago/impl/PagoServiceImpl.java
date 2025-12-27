package com.serveat.service.pago.impl;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.service.pago.PagoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepo;

    public PagoServiceImpl(PagoRepository pagoRepo) {
        this.pagoRepo = pagoRepo;
    }

    // Crea un pago en estado PENDIENTE asociado a un pedido y método.
    @Override
    public Pago iniciarPago(Pedido pedido, MetodoPago metodo) {

        if (pedido == null) {
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }
        if (metodo == null) {
            throw new IllegalArgumentException("Método de pago inválido");
        }

        BigDecimal total = pedido.calcularPrecioTotal();
        if (total == null || total.signum() <= 0) {
            throw new IllegalArgumentException("Importe inválido");
        }

        Pago pago = new Pago(pedido, metodo, total);
        return pagoRepo.save(pago);
    }

    // Confirma un pago pendiente y guarda referencia del proveedor.
    @Override
    public Pago confirmarPago(Long pagoId, String referencia) {

        if (pagoId == null) {
            throw new IllegalArgumentException("Pago inválido");
        }
        if (referencia == null || referencia.isBlank()) {
            throw new IllegalArgumentException("Referencia inválida");
        }

        Pago pago = pagoRepo.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        pago.confirmar(referencia);
        return pagoRepo.save(pago);
    }

    // Marca un pago como fallido indicando el motivo.
    @Override
    public void marcarPagoFallido(Long pagoId, String motivo) {

        if (pagoId == null) {
            throw new IllegalArgumentException("Pago inválido");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Motivo inválido");
        }

        Pago pago = pagoRepo.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        pago.fallar(motivo);
        pagoRepo.save(pago);
    }

    // Procesa y confirma (o falla) un pago online validando los datos del método.
    @Override
    public Pago procesarPagoOnline(Pedido pedidoCreado,
                                   MetodoPago metodo,
                                   String cardNumber,
                                   String cardHolder,
                                   String cardExpiryMMYY,
                                   String cardCvv,
                                   String paypalEmail,
                                   String paypalPassword,
                                   BigDecimal efectivoPagaCon) {

        if (pedidoCreado == null) {
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (metodo == null) {
            throw new IllegalArgumentException("Selecciona un método de pago");
        }

        Pago pago = iniciarPago(pedidoCreado, metodo);

        validarDatosPago(pedidoCreado, metodo, cardNumber, cardHolder, cardExpiryMMYY, cardCvv,
                paypalEmail, paypalPassword, efectivoPagaCon);

        if (metodo == MetodoPago.EFECTIVO) {

            return pago;
        }
        
        try {

            autorizarProveedorSimulado(metodo, cardNumber, paypalEmail);

            String referencia = generarReferencia(metodo);
            return confirmarPago(pago.getId(), referencia);

        } catch (Exception ex) {
            marcarPagoFallido(pago.getId(), ex.getMessage());
            throw ex;
        }
    }

    private void validarDatosPago(Pedido pedidoCreado,
                                  MetodoPago metodo,
                                  String cardNumber,
                                  String cardHolder,
                                  String cardExpiryMMYY,
                                  String cardCvv,
                                  String paypalEmail,
                                  String paypalPassword,
                                  BigDecimal efectivoPagaCon) {

        if (metodo == MetodoPago.TARJETA) {
            String digits = soloDigitos(cardNumber);
            if (digits.length() < 13 || digits.length() > 19) {
                throw new IllegalArgumentException("Número de tarjeta inválido (13-19 dígitos)");
            }
            if (!luhnValido(digits)) {
                throw new IllegalArgumentException("Número de tarjeta inválido");
            }

            String titular = cardHolder != null ? cardHolder.trim() : "";
            if (titular.length() < 3) {
                throw new IllegalArgumentException("Titular inválido");
            }

            String exp = cardExpiryMMYY != null ? cardExpiryMMYY.trim() : "";
            if (!exp.matches("^(0[1-9]|1[0-2])\\/\\d{2}$")) {
                throw new IllegalArgumentException("Caducidad inválida (MM/YY)");
            }
            if (!caducidadValida(exp)) {
                throw new IllegalArgumentException("La tarjeta está caducada");
            }

            String cvv = cardCvv != null ? cardCvv.trim() : "";
            if (!cvv.matches("^\\d{3,4}$")) {
                throw new IllegalArgumentException("CVV inválido (3-4 dígitos)");
            }
        }

        if (metodo == MetodoPago.PAYPAL) {
            String em = paypalEmail != null ? paypalEmail.trim() : "";
            if (em.isBlank() || !em.contains("@") || !em.contains(".")) {
                throw new IllegalArgumentException("Email PayPal inválido");
            }
            String pw = paypalPassword != null ? paypalPassword : "";
            if (pw.length() < 6) {
                throw new IllegalArgumentException("Contraseña PayPal inválida (mín. 6 caracteres)");
            }
        }

        if (metodo == MetodoPago.EFECTIVO) {
            if (efectivoPagaCon != null && efectivoPagaCon.signum() < 0) {
                throw new IllegalArgumentException("Importe de efectivo inválido");
            }
            if (efectivoPagaCon != null) {
                BigDecimal total = pedidoCreado.calcularPrecioTotal();
                if (efectivoPagaCon.compareTo(total) < 0) {
                    throw new IllegalArgumentException("El efectivo indicado es menor que el total");
                }
            }
        }
    }

    private void autorizarProveedorSimulado(MetodoPago metodo, String cardNumber, String paypalEmail) {
        if (metodo == MetodoPago.TARJETA) {
            String digits = soloDigitos(cardNumber);
            if (digits.endsWith("0000")) {
                throw new IllegalArgumentException("El emisor ha rechazado la operación");
            }
        }
        if (metodo == MetodoPago.PAYPAL) {
            String em = paypalEmail != null ? paypalEmail.toLowerCase() : "";
            if (em.contains("fail")) {
                throw new IllegalArgumentException("PayPal no ha podido autorizar el pago");
            }
        }
    }

    private String generarReferencia(MetodoPago metodoPago) {
        String pref = switch (metodoPago) {
            case TARJETA -> "CARD";
            case PAYPAL -> "PP";
            case EFECTIVO -> "CASH";
        };
        return pref + "-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }

    private String soloDigitos(String s) {
        if (s == null) return "";
        return s.replaceAll("\\D+", "");
    }

    private boolean luhnValido(String digits) {
        int sum = 0;
        boolean alt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alt) {
                n *= 2;
                if (n > 9) n -= 9;
            }
            sum += n;
            alt = !alt;
        }
        return sum % 10 == 0;
    }

    private boolean caducidadValida(String mmYY) {
        int mm = Integer.parseInt(mmYY.substring(0, 2));
        int yy = Integer.parseInt(mmYY.substring(3, 5));
        int year = 2000 + yy;
        YearMonth exp = YearMonth.of(year, mm);
        YearMonth now = YearMonth.now();
        return !exp.isBefore(now);
    }
}