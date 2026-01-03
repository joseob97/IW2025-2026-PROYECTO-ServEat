package com.serveat.repository.reserva;

import com.serveat.domain.reserva.EstadoReservaMesa;
import com.serveat.domain.reserva.ReservaMesa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservaMesaRepositoryIT {

    @Autowired
    private ReservaMesaRepository repo;

    @Test
    void findByNumeroMesaAndEstado_cuandoExiste_devuelveEntidad() {
        ReservaMesa reserva = new ReservaMesa(5);
        repo.save(reserva);

        Optional<ReservaMesa> res = repo.findByNumeroMesaAndEstado(
                5,
                EstadoReservaMesa.ABIERTA
        );

        assertThat(res).isPresent();
        assertThat(res.get().getNumeroMesa()).isEqualTo(5);
    }

    @Test
    void findByNumeroMesaAndEstado_cuandoNoExistePorNumero_devuelveEmpty() {
        ReservaMesa reserva = new ReservaMesa(3);
        repo.save(reserva);

        Optional<ReservaMesa> res = repo.findByNumeroMesaAndEstado(
                4,
                EstadoReservaMesa.ABIERTA
        );

        assertThat(res).isEmpty();
    }

    @Test
    void findByNumeroMesaAndEstado_cuandoNoExistePorEstado_devuelveEmpty() {
        ReservaMesa reserva = new ReservaMesa(7);
        reserva.cerrar();
        repo.save(reserva);

        Optional<ReservaMesa> res = repo.findByNumeroMesaAndEstado(
                7,
                EstadoReservaMesa.ABIERTA
        );

        assertThat(res).isEmpty();
    }

    @Test
    void findByNumeroMesaAndEstado_cuandoMesaCerrada_devuelveEntidad() {
        ReservaMesa reserva = new ReservaMesa(9);
        reserva.cerrar();
        repo.save(reserva);

        Optional<ReservaMesa> res = repo.findByNumeroMesaAndEstado(
                9,
                EstadoReservaMesa.CERRADA
        );

        assertThat(res).isPresent();
        assertThat(res.get().getNumeroMesa()).isEqualTo(9);
    }
}