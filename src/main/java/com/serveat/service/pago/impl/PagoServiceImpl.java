package com.serveat.service.pago.impl;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.service.pago.PagoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class PagoServiceImpl implements PagoService {

    private final PagoRepository pagoRepo;

    public PagoServiceImpl(PagoRepository pagoRepo) {
        this.pagoRepo = pagoRepo;
    }

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
}