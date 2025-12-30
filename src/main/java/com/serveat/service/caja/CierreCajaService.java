package com.serveat.service.caja;

import com.serveat.domain.caja.CierreCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface CierreCajaService {

    boolean isCajaCerrada(LocalDate fecha);

    CierreCaja cerrarCaja(LocalDate fecha, BigDecimal total, BigDecimal efectivo, BigDecimal tarjeta, BigDecimal paypal);

    List<CierreCaja> obtenerHistorialSemanal();
}
