package com.serveat.service.pago;

import com.serveat.domain.pago.ajuste.EstadoAjustePago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.ajuste.TipoAjustePago;

import java.math.BigDecimal;

public class AjustePagoDTO {

    public enum Accion {
        NINGUNA,
        COBRAR_DIFERENCIA,
        DEVOLVER_DIFERENCIA
    }

    private final String codigoPedido;

    private final BigDecimal totalAnterior;
    private final BigDecimal totalNuevo;
    private final BigDecimal diferenciaAbs;

    private final Accion accion;
    private final MetodoPago metodoPagoOriginal;

    private final String mensaje;

    private final String codigoAjuste;
    private final TipoAjustePago tipoAjuste;
    private final EstadoAjustePago estadoAjuste;

    public AjustePagoDTO(String codigoPedido,
                         BigDecimal totalAnterior,
                         BigDecimal totalNuevo,
                         BigDecimal diferenciaAbs,
                         Accion accion,
                         MetodoPago metodoPagoOriginal,
                         String mensaje,
                         String codigoAjuste,
                         TipoAjustePago tipoAjuste,
                         EstadoAjustePago estadoAjuste) {
        this.codigoPedido = codigoPedido;
        this.totalAnterior = totalAnterior;
        this.totalNuevo = totalNuevo;
        this.diferenciaAbs = diferenciaAbs;
        this.accion = accion;
        this.metodoPagoOriginal = metodoPagoOriginal;
        this.mensaje = mensaje;
        this.codigoAjuste = codigoAjuste;
        this.tipoAjuste = tipoAjuste;
        this.estadoAjuste = estadoAjuste;
    }

    public String getCodigoPedido() { return codigoPedido; }
    public BigDecimal getTotalAnterior() { return totalAnterior; }
    public BigDecimal getTotalNuevo() { return totalNuevo; }
    public BigDecimal getDiferenciaAbs() { return diferenciaAbs; }
    public Accion getAccion() { return accion; }
    public MetodoPago getMetodoPagoOriginal() { return metodoPagoOriginal; }
    public String getMensaje() { return mensaje; }
    public String getCodigoAjuste() { return codigoAjuste; }
    public TipoAjustePago getTipoAjuste() { return tipoAjuste; }
    public EstadoAjustePago getEstadoAjuste() { return estadoAjuste; }
}