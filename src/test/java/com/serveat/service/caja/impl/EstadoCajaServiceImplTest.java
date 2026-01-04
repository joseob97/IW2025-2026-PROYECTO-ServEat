package com.serveat.service.caja.impl;

import com.serveat.domain.caja.EstadoCaja;
import com.serveat.domain.caja.TipoEstadoCaja;
import com.serveat.repository.caja.EstadoCajaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstadoCajaServiceImplTest {

    @Mock
    private EstadoCajaRepository estadoCajaRepo;

    @InjectMocks
    private EstadoCajaServiceImpl service;

    @Test
    void isCajaAbierta_si_no_hay_registros_devuelve_true_por_defecto() {
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.empty());

        boolean res = service.isCajaAbierta();

        assertThat(res).isTrue();
        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verifyNoMoreInteractions(estadoCajaRepo);
    }

    @Test
    void isCajaAbierta_si_ultimo_estado_es_abierta_devuelve_true() {
        EstadoCaja ultimo = mock(EstadoCaja.class);
        when(ultimo.getTipo()).thenReturn(TipoEstadoCaja.ABIERTA);
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.of(ultimo));

        boolean res = service.isCajaAbierta();

        assertThat(res).isTrue();
        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verify(ultimo).getTipo();
        verifyNoMoreInteractions(estadoCajaRepo, ultimo);
    }

    @Test
    void isCajaAbierta_si_ultimo_estado_es_cerrada_devuelve_false() {
        EstadoCaja ultimo = mock(EstadoCaja.class);
        when(ultimo.getTipo()).thenReturn(TipoEstadoCaja.CERRADA);
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.of(ultimo));

        boolean res = service.isCajaAbierta();

        assertThat(res).isFalse();
        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verify(ultimo).getTipo();
        verifyNoMoreInteractions(estadoCajaRepo, ultimo);
    }

    @Test
    void abrirCaja_si_ultimo_estado_es_abierta_lanza_illegalState_y_no_guarda() {
        EstadoCaja ultimo = mock(EstadoCaja.class);
        when(ultimo.getTipo()).thenReturn(TipoEstadoCaja.ABIERTA);
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.of(ultimo));

        assertThatThrownBy(() -> service.abrirCaja("admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La caja ya está abierta.");

        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verify(ultimo).getTipo();
        verify(estadoCajaRepo, never()).save(any());
        verifyNoMoreInteractions(estadoCajaRepo, ultimo);
    }

    @Test
    void abrirCaja_si_no_hay_registros_guarda_estado_abierta_con_usuario() {
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.empty());
        ArgumentCaptor<EstadoCaja> captor = ArgumentCaptor.forClass(EstadoCaja.class);

        service.abrirCaja("SISTEMA_AUTO");

        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verify(estadoCajaRepo).save(captor.capture());
        verifyNoMoreInteractions(estadoCajaRepo);

        EstadoCaja guardado = captor.getValue();
        assertThat(guardado).isNotNull();
        assertThat(guardado.getTipo()).isEqualTo(TipoEstadoCaja.ABIERTA);
        assertThat(guardado.getUsuario()).isEqualTo("SISTEMA_AUTO");
        assertThat(guardado.getFechaHora()).isNotNull();
    }

    @Test
    void abrirCaja_si_ultimo_estado_es_cerrada_guarda_estado_abierta_con_usuario() {
        EstadoCaja ultimo = mock(EstadoCaja.class);
        when(ultimo.getTipo()).thenReturn(TipoEstadoCaja.CERRADA);
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.of(ultimo));

        ArgumentCaptor<EstadoCaja> captor = ArgumentCaptor.forClass(EstadoCaja.class);

        service.abrirCaja("admin");

        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verify(ultimo).getTipo();
        verify(estadoCajaRepo).save(captor.capture());
        verifyNoMoreInteractions(estadoCajaRepo, ultimo);

        EstadoCaja guardado = captor.getValue();
        assertThat(guardado).isNotNull();
        assertThat(guardado.getTipo()).isEqualTo(TipoEstadoCaja.ABIERTA);
        assertThat(guardado.getUsuario()).isEqualTo("admin");
        assertThat(guardado.getFechaHora()).isNotNull();
    }

    @Test
    void cerrarCaja_si_ultimo_estado_es_cerrada_lanza_illegalState_y_no_guarda() {
        EstadoCaja ultimo = mock(EstadoCaja.class);
        when(ultimo.getTipo()).thenReturn(TipoEstadoCaja.CERRADA);
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.of(ultimo));

        assertThatThrownBy(() -> service.cerrarCaja("admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La caja ya está cerrada.");

        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verify(ultimo).getTipo();
        verify(estadoCajaRepo, never()).save(any());
        verifyNoMoreInteractions(estadoCajaRepo, ultimo);
    }

    @Test
    void cerrarCaja_si_no_hay_registros_permite_cerrar_y_guarda_estado_cerrada() {
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.empty());
        ArgumentCaptor<EstadoCaja> captor = ArgumentCaptor.forClass(EstadoCaja.class);

        service.cerrarCaja("SISTEMA_AUTO");

        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verify(estadoCajaRepo).save(captor.capture());
        verifyNoMoreInteractions(estadoCajaRepo);

        EstadoCaja guardado = captor.getValue();
        assertThat(guardado).isNotNull();
        assertThat(guardado.getTipo()).isEqualTo(TipoEstadoCaja.CERRADA);
        assertThat(guardado.getUsuario()).isEqualTo("SISTEMA_AUTO");
        assertThat(guardado.getFechaHora()).isNotNull();
    }

    @Test
    void cerrarCaja_si_ultimo_estado_es_abierta_guarda_estado_cerrada() {
        EstadoCaja ultimo = mock(EstadoCaja.class);
        when(ultimo.getTipo()).thenReturn(TipoEstadoCaja.ABIERTA);
        when(estadoCajaRepo.findTopByOrderByFechaHoraDesc()).thenReturn(Optional.of(ultimo));

        ArgumentCaptor<EstadoCaja> captor = ArgumentCaptor.forClass(EstadoCaja.class);

        service.cerrarCaja("admin");

        verify(estadoCajaRepo).findTopByOrderByFechaHoraDesc();
        verify(ultimo).getTipo();
        verify(estadoCajaRepo).save(captor.capture());
        verifyNoMoreInteractions(estadoCajaRepo, ultimo);

        EstadoCaja guardado = captor.getValue();
        assertThat(guardado).isNotNull();
        assertThat(guardado.getTipo()).isEqualTo(TipoEstadoCaja.CERRADA);
        assertThat(guardado.getUsuario()).isEqualTo("admin");
        assertThat(guardado.getFechaHora()).isNotNull();
    }

    @Test
    void obtenerFechaUltimaApertura_si_no_hay_aperturas_devuelve_empty() {
        EstadoCaja e1 = mock(EstadoCaja.class);
        when(e1.getTipo()).thenReturn(TipoEstadoCaja.CERRADA);

        when(estadoCajaRepo.findAll()).thenReturn(List.of(e1));

        Optional<LocalDateTime> res = service.obtenerFechaUltimaApertura();

        assertThat(res).isEmpty();

        verify(estadoCajaRepo).findAll();
        verify(e1).getTipo();
        verifyNoMoreInteractions(estadoCajaRepo, e1);
    }

    @Test
    void obtenerFechaUltimaApertura_devuelve_la_mas_reciente_de_las_aperturas() {
        EstadoCaja a1 = mock(EstadoCaja.class);
        EstadoCaja a2 = mock(EstadoCaja.class);
        EstadoCaja c1 = mock(EstadoCaja.class);

        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 1, 3, 18, 30);

        when(a1.getTipo()).thenReturn(TipoEstadoCaja.ABIERTA);
        when(a2.getTipo()).thenReturn(TipoEstadoCaja.ABIERTA);
        when(c1.getTipo()).thenReturn(TipoEstadoCaja.CERRADA);

        when(a1.getFechaHora()).thenReturn(t1);
        when(a2.getFechaHora()).thenReturn(t2);

        when(estadoCajaRepo.findAll()).thenReturn(List.of(c1, a1, a2));

        Optional<LocalDateTime> res = service.obtenerFechaUltimaApertura();

        assertThat(res).contains(t2);

        verify(estadoCajaRepo).findAll();
        verify(a1).getTipo();
        verify(a2).getTipo();
        verify(c1).getTipo();
        verify(a1).getFechaHora();
        verify(a2).getFechaHora();
        verifyNoMoreInteractions(estadoCajaRepo, a1, a2, c1);
    }
}