package com.serveat.service.pago.impl;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pago.ajuste.AjustePago;
import com.serveat.domain.pago.ajuste.EstadoAjustePago;
import com.serveat.domain.pago.ajuste.TipoAjustePago;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.AjustePagoRepository;
import com.serveat.service.pago.AjustePagoService;
import com.serveat.service.pago.dto.AjustePagoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class AjustePagoServiceImpl implements AjustePagoService {

    private final AjustePagoRepository ajusteRepo;

    public AjustePagoServiceImpl(AjustePagoRepository ajusteRepo) {
        this.ajusteRepo = ajusteRepo;
    }

    @Override
    public AjustePagoDTO calcularYCrearOActualizarAjuste(Pedido pedido,
                                                         Pago pagoOriginal,
                                                         BigDecimal totalAnterior,
                                                         BigDecimal totalNuevo) {

        if (pedido == null) throw new IllegalArgumentException("Pedido inválido");

        BigDecimal ant = nz(totalAnterior);
        BigDecimal nue = nz(totalNuevo);
        BigDecimal diff = nue.subtract(ant);

        MetodoPago metodo = (pagoOriginal != null) ? pagoOriginal.getMetodo() : null;

        if (diff.signum() == 0) {
            cancelarPendienteSiExiste(pedido);
            return new AjustePagoDTO(
                    pedido.getCodigo(), ant, nue, BigDecimal.ZERO,
                    AjustePagoDTO.Accion.NINGUNA,
                    metodo,
                    "No hay diferencia de importe.",
                    null, null, null
            );
        }

        if (pagoOriginal == null) {
            cancelarPendienteSiExiste(pedido);
            return new AjustePagoDTO(
                    pedido.getCodigo(), ant, nue, diff.abs(),
                    AjustePagoDTO.Accion.NINGUNA,
                    null,
                    "El pedido no tiene pago asociado; no se genera ajuste.",
                    null, null, null
            );
        }

        if (pagoOriginal.getEstado() != EstadoPago.CONFIRMADO) {
            cancelarPendienteSiExiste(pedido);
            return new AjustePagoDTO(
                    pedido.getCodigo(), ant, nue, diff.abs(),
                    AjustePagoDTO.Accion.NINGUNA,
                    metodo,
                    "El pago no está confirmado; no se genera ajuste.",
                    null, null, null
            );
        }

        if (metodo == MetodoPago.EFECTIVO) {
            cancelarPendienteSiExiste(pedido);

            AjustePagoDTO.Accion accion = (diff.signum() > 0)
                    ? AjustePagoDTO.Accion.COBRAR_DIFERENCIA
                    : AjustePagoDTO.Accion.DEVOLVER_DIFERENCIA;

            String mensaje = (diff.signum() > 0)
                    ? "El total ha aumentado. Se debe cobrar la diferencia en efectivo."
                    : "El total ha disminuido. Se debe devolver la diferencia en efectivo.";

            return new AjustePagoDTO(
                    pedido.getCodigo(), ant, nue, diff.abs(),
                    accion, metodo, mensaje,
                    null, null, null
            );
        }

        AjustePago pendiente = ajusteRepo
                .findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                        pedido.getCodigo(), EstadoAjustePago.PENDIENTE
                )
                .orElse(null);

        TipoAjustePago tipo = (diff.signum() > 0) ? TipoAjustePago.COBRO : TipoAjustePago.DEVOLUCION;
        BigDecimal importe = diff.abs();

        AjustePago ajuste;
        if (pendiente != null) {
            ajuste = pendiente;

            if (ajuste.getEstado() != EstadoAjustePago.PENDIENTE) {
                throw new IllegalStateException("No se puede actualizar un ajuste que no está pendiente");
            }

            ajuste.setTipo(tipo);
            ajuste.setImporte(importe);
            ajusteRepo.save(ajuste);

        } else {
            String codigoAjuste = generarCodigo();

            ajuste = new AjustePago(
                    pedido,
                    codigoAjuste,
                    tipo,
                    importe
            );

            ajusteRepo.save(ajuste);
        }

        AjustePagoDTO.Accion accion = (diff.signum() > 0)
                ? AjustePagoDTO.Accion.COBRAR_DIFERENCIA
                : AjustePagoDTO.Accion.DEVOLVER_DIFERENCIA;

        String mensaje = (diff.signum() > 0)
                ? "El total ha aumentado. Se ha generado un ajuste de cobro."
                : "El total ha disminuido. Se ha generado un ajuste de devolución.";

        return new AjustePagoDTO(
                pedido.getCodigo(), ant, nue, importe,
                accion, metodo, mensaje,
                ajuste.getCodigo(), ajuste.getTipo(), ajuste.getEstado()
        );
    }

    private void cancelarPendienteSiExiste(Pedido pedido) {
        if (pedido == null || pedido.getCodigo() == null || pedido.getCodigo().isBlank()) return;

        AjustePago pendiente = ajusteRepo
                .findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                        pedido.getCodigo(), EstadoAjustePago.PENDIENTE
                )
                .orElse(null);

        if (pendiente == null) return;

        pendiente.setEstado(EstadoAjustePago.CANCELADO);
        ajusteRepo.save(pendiente);
    }

    private String generarCodigo() {
        return "AP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private BigDecimal nz(BigDecimal x) {
        return x == null ? BigDecimal.ZERO : x;
    }
}