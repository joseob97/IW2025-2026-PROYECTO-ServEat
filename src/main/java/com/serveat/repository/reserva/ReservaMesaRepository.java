package com.serveat.repository.reserva;

import com.serveat.domain.reserva.EstadoReservaMesa;
import com.serveat.domain.reserva.ReservaMesa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReservaMesaRepository extends JpaRepository<ReservaMesa, UUID> {
    Optional<ReservaMesa> findByNumeroMesaAndEstado(Integer numeroMesa, EstadoReservaMesa estado);
}