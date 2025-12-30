package com.serveat.service.caja.impl;

import com.serveat.domain.caja.CierreCaja;
import com.serveat.repository.caja.CierreCajaRepository;
import com.serveat.service.caja.CierreCajaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class CierreCajaServiceImpl implements CierreCajaService {

    private final CierreCajaRepository cierreCajaRepo;

    public CierreCajaServiceImpl(CierreCajaRepository cierreCajaRepo) {
        this.cierreCajaRepo = cierreCajaRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCajaCerrada(LocalDate fecha) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        return cierreCajaRepo.existsByFecha(fecha);
    }

    @Override
    public CierreCaja cerrarCaja(LocalDate fecha, BigDecimal total, BigDecimal efectivo, BigDecimal tarjeta, BigDecimal paypal) {
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha no puede ser nula");
        }
        
        if (cierreCajaRepo.existsByFecha(fecha)) {
            throw new IllegalStateException("La caja ya ha sido cerrada para la fecha: " + fecha);
        }

        CierreCaja cierre = new CierreCaja(fecha, total, efectivo, tarjeta, paypal);
        return cierreCajaRepo.save(cierre);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CierreCaja> obtenerHistorialSemanal() {
        return cierreCajaRepo.findTop7ByOrderByFechaDesc();
    }
}
