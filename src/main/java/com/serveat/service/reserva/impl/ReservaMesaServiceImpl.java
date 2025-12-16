package com.serveat.service.reserva.impl;

import com.serveat.domain.reserva.EstadoReservaMesa;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.repository.reserva.ReservaMesaRepository;
import com.serveat.service.reserva.ReservaMesaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReservaMesaServiceImpl implements ReservaMesaService {

    private final ReservaMesaRepository repo;

    public ReservaMesaServiceImpl(ReservaMesaRepository repo) {
        this.repo = repo;
    }

    @Override
    public ReservaMesa abrirMesa(Integer numeroMesa) {
        return repo.findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA)
                .orElseGet(() -> repo.save(new ReservaMesa(numeroMesa)));
    }

    @Override
    public ReservaMesa obtenerMesaAbierta(Integer numeroMesa) {
        return repo.findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA)
                .orElseThrow(() ->
                        new IllegalArgumentException("No hay una mesa abierta con ese número")
                );
    }

    @Override
    public ReservaMesa cerrarMesa(Integer numeroMesa) {
        ReservaMesa rm = obtenerMesaAbierta(numeroMesa);
        rm.cerrar();
        return repo.save(rm);
    }
}