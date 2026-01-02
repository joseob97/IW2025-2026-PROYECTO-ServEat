package com.serveat.service.reserva.impl;

import com.serveat.domain.reserva.EstadoReservaMesa;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.repository.reserva.ReservaMesaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaMesaServiceImplTest {

    @Mock
    private ReservaMesaRepository repo;

    @InjectMocks
    private ReservaMesaServiceImpl service;

    @Test
    void abrirMesa_si_ya_hay_abierta_devuelve_la_misma_y_no_guarda() {
        Integer numeroMesa = 5;
        ReservaMesa existente = new ReservaMesa(numeroMesa);

        when(repo.findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA))
                .thenReturn(Optional.of(existente));

        ReservaMesa res = service.abrirMesa(numeroMesa);

        assertThat(res).isSameAs(existente);
        verify(repo, never()).save(any());
    }

    @Test
    void abrirMesa_si_no_hay_abierta_crea_y_guarda_una_nueva() {
        Integer numeroMesa = 7;

        when(repo.findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA))
                .thenReturn(Optional.empty());

        when(repo.save(any(ReservaMesa.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaMesa res = service.abrirMesa(numeroMesa);

        assertThat(res).isNotNull();
        assertThat(res.getNumeroMesa()).isEqualTo(numeroMesa);

        ArgumentCaptor<ReservaMesa> captor = ArgumentCaptor.forClass(ReservaMesa.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getNumeroMesa()).isEqualTo(numeroMesa);
    }

    @Test
    void obtenerMesaAbierta_si_existe_la_devuelve() {
        Integer numeroMesa = 3;
        ReservaMesa existente = new ReservaMesa(numeroMesa);

        when(repo.findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA))
                .thenReturn(Optional.of(existente));

        ReservaMesa res = service.obtenerMesaAbierta(numeroMesa);

        assertThat(res).isSameAs(existente);
    }

    @Test
    void obtenerMesaAbierta_si_no_existe_lanza_excepcion_con_mensaje() {
        Integer numeroMesa = 99;

        when(repo.findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerMesaAbierta(numeroMesa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No hay una mesa abierta con ese número");
    }

    @Test
    void cerrarMesa_si_existe_abierta_la_cierra_y_guarda() {
        Integer numeroMesa = 10;
        ReservaMesa abierta = new ReservaMesa(numeroMesa);

        when(repo.findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA))
                .thenReturn(Optional.of(abierta));

        when(repo.save(any(ReservaMesa.class))).thenAnswer(inv -> inv.getArgument(0));

        ReservaMesa res = service.cerrarMesa(numeroMesa);

        assertThat(res).isSameAs(abierta);

        ArgumentCaptor<ReservaMesa> captor = ArgumentCaptor.forClass(ReservaMesa.class);
        verify(repo).save(captor.capture());

        ReservaMesa guardada = captor.getValue();
        assertThat(guardada).isSameAs(abierta);

        // Verificación indirecta del cierre: el método cerrar() debe haber asignado un "fin"
        assertThat(guardada.getFin()).isNotNull();
    }

    @Test
    void cerrarMesa_si_no_hay_abierta_propaga_excepcion_de_obtener() {
        Integer numeroMesa = 11;

        when(repo.findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cerrarMesa(numeroMesa))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No hay una mesa abierta con ese número");

        verify(repo, never()).save(any());
    }
}