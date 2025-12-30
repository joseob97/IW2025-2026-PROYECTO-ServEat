package com.serveat.repository.caja;

import com.serveat.domain.caja.CierreCaja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CierreCajaRepository extends JpaRepository<CierreCaja, Long> {

    boolean existsByFecha(LocalDate fecha);

    Optional<CierreCaja> findByFecha(LocalDate fecha);

    List<CierreCaja> findTop7ByOrderByFechaDesc();
}
