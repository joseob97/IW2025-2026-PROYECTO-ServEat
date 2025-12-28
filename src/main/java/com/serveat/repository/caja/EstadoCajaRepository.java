package com.serveat.repository.caja;

import com.serveat.domain.caja.EstadoCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EstadoCajaRepository extends JpaRepository<EstadoCaja, Long> {

    Optional<EstadoCaja> findTopByOrderByFechaHoraDesc();
}
