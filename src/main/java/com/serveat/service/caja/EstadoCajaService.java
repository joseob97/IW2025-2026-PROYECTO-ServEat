package com.serveat.service.caja;

import com.serveat.domain.caja.EstadoCaja;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EstadoCajaService {

    boolean isCajaAbierta();

    void abrirCaja(String usuario);

    void cerrarCaja(String usuario);

    Optional<LocalDateTime> obtenerFechaUltimaApertura();
}
