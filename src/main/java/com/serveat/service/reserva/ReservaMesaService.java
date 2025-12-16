package com.serveat.service.reserva;

import com.serveat.domain.reserva.ReservaMesa;

public interface ReservaMesaService {

    ReservaMesa abrirMesa(Integer numeroMesa);

    ReservaMesa obtenerMesaAbierta(Integer numeroMesa);

    ReservaMesa cerrarMesa(Integer numeroMesa);
}