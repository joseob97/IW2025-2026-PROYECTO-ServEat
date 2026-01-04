package com.serveat.service.caja.impl;

import com.serveat.domain.caja.CierreCaja;
import com.serveat.repository.caja.CierreCajaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CierreCajaServiceImplTest {

    @Mock
    private CierreCajaRepository cierreCajaRepo;

    @InjectMocks
    private CierreCajaServiceImpl service;

    @Test
    void isCajaCerrada_si_fecha_es_nula_lanza_illegalArgument() {
        assertThatThrownBy(() -> service.isCajaCerrada(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha no puede ser nula");

        verifyNoInteractions(cierreCajaRepo);
    }

    @Test
    void isCajaCerrada_devuelve_true_si_repo_indica_que_existe_cierre() {
        LocalDate fecha = LocalDate.of(2026, 1, 4);
        when(cierreCajaRepo.existsByFecha(fecha)).thenReturn(true);

        boolean res = service.isCajaCerrada(fecha);

        assertThat(res).isTrue();
        verify(cierreCajaRepo).existsByFecha(fecha);
        verifyNoMoreInteractions(cierreCajaRepo);
    }

    @Test
    void isCajaCerrada_devuelve_false_si_repo_indica_que_no_existe_cierre() {
        LocalDate fecha = LocalDate.of(2026, 1, 4);
        when(cierreCajaRepo.existsByFecha(fecha)).thenReturn(false);

        boolean res = service.isCajaCerrada(fecha);

        assertThat(res).isFalse();
        verify(cierreCajaRepo).existsByFecha(fecha);
        verifyNoMoreInteractions(cierreCajaRepo);
    }

    @Test
    void cerrarCaja_si_fecha_es_nula_lanza_illegalArgument() {
        assertThatThrownBy(() -> service.cerrarCaja(null,
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("3.00"),
                new BigDecimal("2.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La fecha no puede ser nula");

        verifyNoInteractions(cierreCajaRepo);
    }

    @Test
    void cerrarCaja_si_ya_existe_cierre_para_fecha_lanza_illegalState_y_no_guarda() {
        LocalDate fecha = LocalDate.of(2026, 1, 4);
        when(cierreCajaRepo.existsByFecha(fecha)).thenReturn(true);

        assertThatThrownBy(() -> service.cerrarCaja(
                fecha,
                new BigDecimal("10.00"),
                new BigDecimal("5.00"),
                new BigDecimal("3.00"),
                new BigDecimal("2.00")
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La caja ya ha sido cerrada para la fecha: " + fecha);

        verify(cierreCajaRepo).existsByFecha(fecha);
        verify(cierreCajaRepo, never()).save(any());
        verifyNoMoreInteractions(cierreCajaRepo);
    }

    @Test
    void cerrarCaja_si_no_existe_cierre_crea_entidad_y_guarda_y_devuelve_guardada() {
        LocalDate fecha = LocalDate.of(2026, 1, 4);
        BigDecimal total = new BigDecimal("10.00");
        BigDecimal efectivo = new BigDecimal("5.00");
        BigDecimal tarjeta = new BigDecimal("3.00");
        BigDecimal paypal = new BigDecimal("2.00");

        when(cierreCajaRepo.existsByFecha(fecha)).thenReturn(false);

        ArgumentCaptor<CierreCaja> captor = ArgumentCaptor.forClass(CierreCaja.class);

        CierreCaja guardado = mock(CierreCaja.class);
        when(cierreCajaRepo.save(any(CierreCaja.class))).thenReturn(guardado);

        CierreCaja res = service.cerrarCaja(fecha, total, efectivo, tarjeta, paypal);

        assertThat(res).isSameAs(guardado);

        verify(cierreCajaRepo).existsByFecha(fecha);
        verify(cierreCajaRepo).save(captor.capture());
        verifyNoMoreInteractions(cierreCajaRepo);

        CierreCaja enviadoASave = captor.getValue();
        assertThat(enviadoASave).isNotNull();

        // Validación de estado del objeto creado antes del save
        assertThat(enviadoASave.getFecha()).isEqualTo(fecha);
        assertThat(enviadoASave.getTotalGeneral()).isEqualByComparingTo(total);
        assertThat(enviadoASave.getTotalEfectivo()).isEqualByComparingTo(efectivo);
        assertThat(enviadoASave.getTotalTarjeta()).isEqualByComparingTo(tarjeta);
        assertThat(enviadoASave.getTotalPaypal()).isEqualByComparingTo(paypal);

        // Se asigna en el constructor, no debe ser nulo
        assertThat(enviadoASave.getFechaHoraCierre()).isNotNull();
    }

    @Test
    void obtenerHistorialSemanal_devuelve_los_ultimos_7_cierres_en_orden() {
        CierreCaja c1 = mock(CierreCaja.class);
        CierreCaja c2 = mock(CierreCaja.class);
        List<CierreCaja> esperado = List.of(c1, c2);

        when(cierreCajaRepo.findTop7ByOrderByFechaDesc()).thenReturn(esperado);

        List<CierreCaja> res = service.obtenerHistorialSemanal();

        assertThat(res).isSameAs(esperado);

        verify(cierreCajaRepo).findTop7ByOrderByFechaDesc();
        verifyNoMoreInteractions(cierreCajaRepo);
    }
}