package com.serveat.service.caja.impl;

import com.serveat.domain.caja.EstadoCaja;
import com.serveat.domain.caja.TipoEstadoCaja;
import com.serveat.repository.caja.EstadoCajaRepository;
import com.serveat.service.caja.EstadoCajaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class EstadoCajaServiceImpl implements EstadoCajaService {

    private final EstadoCajaRepository estadoCajaRepo;

    public EstadoCajaServiceImpl(EstadoCajaRepository estadoCajaRepo) {
        this.estadoCajaRepo = estadoCajaRepo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isCajaAbierta() {
        return estadoCajaRepo.findTopByOrderByFechaHoraDesc()
                .map(estado -> estado.getTipo() == TipoEstadoCaja.ABIERTA)
                .orElse(true); // CAMBIO: Por defecto ABIERTA si no hay registros
    }

    @Override
    public void abrirCaja(String usuario) {
        Optional<EstadoCaja> ultimoEstado = estadoCajaRepo.findTopByOrderByFechaHoraDesc();
        if (ultimoEstado.isPresent() && ultimoEstado.get().getTipo() == TipoEstadoCaja.ABIERTA) {
            throw new IllegalStateException("La caja ya está abierta.");
        }
        estadoCajaRepo.save(new EstadoCaja(TipoEstadoCaja.ABIERTA, usuario));
    }

    @Override
    public void cerrarCaja(String usuario) {
        Optional<EstadoCaja> ultimoEstado = estadoCajaRepo.findTopByOrderByFechaHoraDesc();
        // Si no hay registros, asumimos que estaba abierta por defecto, así que permitimos cerrar.
        if (ultimoEstado.isPresent() && ultimoEstado.get().getTipo() == TipoEstadoCaja.CERRADA) {
            throw new IllegalStateException("La caja ya está cerrada.");
        }
        estadoCajaRepo.save(new EstadoCaja(TipoEstadoCaja.CERRADA, usuario));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalDateTime> obtenerFechaUltimaApertura() {
        return estadoCajaRepo.findAll().stream()
                .filter(e -> e.getTipo() == TipoEstadoCaja.ABIERTA)
                .map(EstadoCaja::getFechaHora)
                .max(LocalDateTime::compareTo);
    }
}
