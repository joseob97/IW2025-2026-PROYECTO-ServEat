package com.serveat.service.pago.impl;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pago.ajuste.AjustePago;
import com.serveat.domain.pago.ajuste.EstadoAjustePago;
import com.serveat.domain.pago.ajuste.TipoAjustePago;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.AjustePagoRepository;
import com.serveat.service.pago.AjustePagoService;
import com.serveat.service.pago.AjustePagoDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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

    @Override
    @Transactional(readOnly = true)
    public AjustePagoDTO obtenerDetallePorCodigo(String codigoAjuste) {
        if (codigoAjuste == null || codigoAjuste.isBlank()) {
            throw new IllegalArgumentException("Código de ajuste inválido");
        }

        AjustePago a = ajusteRepo.findByCodigo(codigoAjuste)
                .orElseThrow(() -> new IllegalArgumentException("Ajuste no encontrado: " + codigoAjuste));

        String codigoPedido = (a.getPedido() != null) ? a.getPedido().getCodigo() : null;

        return new AjustePagoDTO(
                codigoPedido,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                a.getImporte(),
                AjustePagoDTO.Accion.NINGUNA,
                null,
                "",
                a.getCodigo(),
                a.getTipo(),
                a.getEstado()
        );
    }

    @Override
    public AjustePagoDTO completarAjuste(String codigoAjuste, String referencia) {
        if (codigoAjuste == null || codigoAjuste.isBlank()) {
            throw new IllegalArgumentException("Código de ajuste inválido");
        }

        AjustePago a = ajusteRepo.findByCodigo(codigoAjuste)
                .orElseThrow(() -> new IllegalArgumentException("Ajuste no encontrado: " + codigoAjuste));

        if (a.getEstado() != EstadoAjustePago.PENDIENTE) {
            throw new IllegalStateException("El ajuste no está pendiente");
        }

        a.setEstado(EstadoAjustePago.COMPLETADO);
        a.setFechaCompletado(LocalDateTime.now());
        a.setReferenciaProveedor((referencia == null || referencia.isBlank()) ? null : referencia.trim());

        ajusteRepo.save(a);

        String codigoPedido = (a.getPedido() != null) ? a.getPedido().getCodigo() : null;

        return new AjustePagoDTO(
                codigoPedido,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                a.getImporte(),
                AjustePagoDTO.Accion.NINGUNA,
                null,
                "Ajuste completado correctamente.",
                a.getCodigo(),
                a.getTipo(),
                a.getEstado()
        );
    }

    @Override
    public AjustePagoDTO cancelarAjuste(String codigoAjuste, String motivo) {

        if (codigoAjuste == null || codigoAjuste.isBlank()) {
            throw new IllegalArgumentException("Código de ajuste inválido");
        }

        AjustePago a = ajusteRepo.findByCodigo(codigoAjuste)
                .orElseThrow(() -> new IllegalArgumentException("Ajuste no encontrado"));

        if (a.getEstado() != EstadoAjustePago.PENDIENTE) {
            throw new IllegalStateException("Solo se puede cancelar un ajuste pendiente");
        }

        a.setEstado(EstadoAjustePago.CANCELADO);
        ajusteRepo.save(a);

        return new AjustePagoDTO(
                a.getPedido() != null ? a.getPedido().getCodigo() : null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                a.getImporte(),
                AjustePagoDTO.Accion.NINGUNA,
                null,
                "Ajuste cancelado correctamente.",
                a.getCodigo(),
                a.getTipo(),
                a.getEstado()
        );
    }

    @Override
    public Pedido crearBorradorPedidoParaEdicion(Pedido original) {
        if (original == null) return null;

        Pedido copia = new Pedido();

        copia.setCodigo(original.getCodigo());

        copia.setCliente(original.getCliente());
        copia.setTipoPedido(original.getTipoPedido());
        copia.setReservaMesa(original.getReservaMesa());
        copia.setDireccionEntrega(original.getDireccionEntrega());

        copia.setEstado(original.getEstado());
        copia.setEstadoCocina(original.getEstadoCocina());
        copia.setEstadoReparto(original.getEstadoReparto());
        copia.setFechaCreacion(original.getFechaCreacion());


        if (original.getLineaPedidos() != null) {
            for (LineaPedido lp : original.getLineaPedidos()) {
                if (lp == null) continue;

                LineaPedido lp2 = new LineaPedido(copia, lp.getProducto(), lp.getCantidad());
                lp2.setCodigo(lp.getCodigo());

                lp2.setPrecioUnitario(lp.getPrecioUnitario());

                if (lp.getIngredientes() != null && !lp.getIngredientes().isEmpty()) {
                    Set<LineaPedidoIngrediente> set = new HashSet<>();
                    for (LineaPedidoIngrediente li : lp.getIngredientes()) {
                        if (li == null) continue;

                        LineaPedidoIngrediente li2 = new LineaPedidoIngrediente(
                                lp2,
                                li.getIngrediente(),
                                li.isIncluido(),
                                li.getExtraCantidad(),
                                li.getPrecioExtra()
                        );
                        set.add(li2);
                    }
                    lp2.setIngredientes(set);
                }

                copia.getLineaPedidos().add(lp2);
            }
        }

        return copia;
    }
}