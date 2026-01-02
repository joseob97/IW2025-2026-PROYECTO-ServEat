package com.serveat.service.pago.impl;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.service.pedido.PedidoCalculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    @Mock
    private PagoRepository pagoRepo;

    @Mock
    private PedidoCalculoService pedidoCalculoService;

    @InjectMocks
    private PagoServiceImpl service;

    private Pedido pedido;

    @BeforeEach
    void setUp() {
        pedido = new Pedido();
        pedido.setLineaPedidos(new LinkedHashSet<>());
        pedido.getLineaPedidos().add(mock(LineaPedido.class));
    }

    @Test
    void iniciarPago_pedido_null_lanza() {
        assertThatThrownBy(() -> service.iniciarPago(null, MetodoPago.TARJETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pedido inválido");

        verifyNoInteractions(pagoRepo);
    }

    @Test
    void iniciarPago_pedido_sin_lineas_lanza() {
        Pedido p = new Pedido();
        p.setLineaPedidos(new LinkedHashSet<>());

        assertThatThrownBy(() -> service.iniciarPago(p, MetodoPago.TARJETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El pedido no puede estar vacío");

        verifyNoInteractions(pagoRepo);
    }

    @Test
    void iniciarPago_metodo_null_lanza() {
        assertThatThrownBy(() -> service.iniciarPago(pedido, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Método de pago inválido");

        verifyNoInteractions(pagoRepo);
    }

    @Test
    void iniciarPago_total_null_o_no_positivo_lanza() {
        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(null);

        assertThatThrownBy(() -> service.iniciarPago(pedido, MetodoPago.TARJETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Importe inválido");

        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.iniciarPago(pedido, MetodoPago.TARJETA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Importe inválido");

        verifyNoInteractions(pagoRepo);
    }

    @Test
    void iniciarPago_ok_crea_y_guarda_pago_pendiente_con_importe_total() {
        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("12.50"));
        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);

        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago res = service.iniciarPago(pedido, MetodoPago.PAYPAL);

        verify(pagoRepo).save(captor.capture());
        Pago saved = captor.getValue();

        assertThat(res).isSameAs(saved);
        assertThat(saved.getPedido()).isSameAs(pedido);
        assertThat(saved.getMetodo()).isEqualTo(MetodoPago.PAYPAL);
        assertThat(saved.getImporte()).isEqualByComparingTo("12.50");
        assertThat(saved.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(saved.getFechaCreacion()).isNotNull();
    }

    @Test
    void confirmarPago_id_null_lanza() {
        assertThatThrownBy(() -> service.confirmarPago(null, "REF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pago inválido");

        verifyNoInteractions(pagoRepo);
    }

    @Test
    void confirmarPago_referencia_invalida_lanza() {
        assertThatThrownBy(() -> service.confirmarPago(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Referencia inválida");

        assertThatThrownBy(() -> service.confirmarPago(1L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Referencia inválida");

        verifyNoInteractions(pagoRepo);
    }

    @Test
    void confirmarPago_no_encontrado_lanza() {
        when(pagoRepo.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmarPago(10L, "REF-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pago no encontrado");

        verify(pagoRepo, never()).save(any());
    }

    @Test
    void confirmarPago_ok_confirma_y_guarda() {
        Pago pago = mock(Pago.class);
        when(pagoRepo.findById(7L)).thenReturn(Optional.of(pago));
        when(pagoRepo.save(pago)).thenReturn(pago);

        Pago res = service.confirmarPago(7L, "REF-OK");

        assertThat(res).isSameAs(pago);
        verify(pago).confirmar("REF-OK");
        verify(pagoRepo).save(pago);
    }

    @Test
    void marcarPagoFallido_id_null_lanza() {
        assertThatThrownBy(() -> service.marcarPagoFallido(null, "motivo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pago inválido");

        verifyNoInteractions(pagoRepo);
    }

    @Test
    void marcarPagoFallido_motivo_invalido_lanza() {
        assertThatThrownBy(() -> service.marcarPagoFallido(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Motivo inválido");

        assertThatThrownBy(() -> service.marcarPagoFallido(1L, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Motivo inválido");

        verifyNoInteractions(pagoRepo);
    }

    @Test
    void marcarPagoFallido_no_encontrado_lanza() {
        when(pagoRepo.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.marcarPagoFallido(3L, "x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pago no encontrado");

        verify(pagoRepo, never()).save(any());
    }

    @Test
    void marcarPagoFallido_ok_falla_y_guarda() {
        Pago pago = mock(Pago.class);
        when(pagoRepo.findById(5L)).thenReturn(Optional.of(pago));

        service.marcarPagoFallido(5L, "fallo");

        verify(pago).fallar("fallo");
        verify(pagoRepo).save(pago);
    }

    @Test
    void procesarPagoOnline_pedido_null_lanza() {
        assertThatThrownBy(() -> service.procesarPagoOnline(null, MetodoPago.TARJETA,
                "4111111111111111", "A", futureExpiry(), "123", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pedido inválido");
    }

    @Test
    void procesarPagoOnline_metodo_null_lanza() {
        assertThatThrownBy(() -> service.procesarPagoOnline(pedido, null,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selecciona un método de pago");
    }

    @Test
    void procesarPagoOnline_efectivo_ok_no_confirma_ni_falla_devuelve_pago_pendiente() {
        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("10.00"));
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Pago res = service.procesarPagoOnline(
                pedido,
                MetodoPago.EFECTIVO,
                null, null, null, null,
                null, null,
                new BigDecimal("10.00")
        );

        assertThat(res.getMetodo()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(res.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        verify(pagoRepo, times(1)).save(any(Pago.class));
        verify(pagoRepo, never()).findById(anyLong());
    }

    @Test
    void procesarPagoOnline_tarjeta_ok_confirma_y_guarda() {
        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("15.00"));

        Pago pagoPendiente = mock(Pago.class);
        when(pagoPendiente.getId()).thenReturn(100L);

        when(pagoRepo.save(any(Pago.class))).thenReturn(pagoPendiente);
        when(pagoRepo.findById(100L)).thenReturn(Optional.of(pagoPendiente));
        when(pagoRepo.save(pagoPendiente)).thenReturn(pagoPendiente);

        Pago res = service.procesarPagoOnline(
                pedido,
                MetodoPago.TARJETA,
                "4111 1111 1111 1111",
                "Juan Perez",
                futureExpiry(),
                "123",
                null, null,
                null
        );

        assertThat(res).isSameAs(pagoPendiente);
        verify(pagoPendiente).confirmar(argThat(ref -> ref != null && ref.startsWith("CARD-")));
        verify(pagoRepo, atLeastOnce()).save(any(Pago.class));
    }

    @Test
    void procesarPagoOnline_tarjeta_rechazo_proveedor_marca_fallido_y_relanzar() {
        when(pedidoCalculoService.calcularTotalPedido(pedido))
                .thenReturn(new BigDecimal("15.00"));

        Pago pagoPendiente = mock(Pago.class);
        when(pagoPendiente.getId()).thenReturn(200L);

        when(pagoRepo.save(any(Pago.class))).thenReturn(pagoPendiente);
        when(pagoRepo.findById(200L)).thenReturn(Optional.of(pagoPendiente));

        assertThatThrownBy(() -> service.procesarPagoOnline(
                pedido,
                MetodoPago.TARJETA,
                "4650 0353 6336 0000", // válida por Luhn y termina en 0000 → rechazo proveedor
                "Juan Perez",
                futureExpiry(),
                "123",
                null, null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El emisor ha rechazado la operación");

        verify(pagoPendiente).fallar("El emisor ha rechazado la operación");
        verify(pagoRepo).save(pagoPendiente);
    }

    @Test
    void procesarPagoOnline_paypal_ok_confirma_y_guarda() {
        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("9.99"));

        Pago pagoPendiente = mock(Pago.class);
        when(pagoPendiente.getId()).thenReturn(300L);

        when(pagoRepo.save(any(Pago.class))).thenReturn(pagoPendiente);
        when(pagoRepo.findById(300L)).thenReturn(Optional.of(pagoPendiente));
        when(pagoRepo.save(pagoPendiente)).thenReturn(pagoPendiente);

        Pago res = service.procesarPagoOnline(
                pedido,
                MetodoPago.PAYPAL,
                null, null, null, null,
                "ok@paypal.com",
                "secret1",
                null
        );

        assertThat(res).isSameAs(pagoPendiente);
        verify(pagoPendiente).confirmar(argThat(ref -> ref != null && ref.startsWith("PP-")));
        verify(pagoRepo, atLeastOnce()).save(any(Pago.class));
    }

    @Test
    void procesarPagoOnline_paypal_rechazo_proveedor_marca_fallido_y_relanzar() {
        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("9.99"));

        Pago pagoPendiente = mock(Pago.class);
        when(pagoPendiente.getId()).thenReturn(400L);

        when(pagoRepo.save(any(Pago.class))).thenReturn(pagoPendiente);
        when(pagoRepo.findById(400L)).thenReturn(Optional.of(pagoPendiente));

        assertThatThrownBy(() -> service.procesarPagoOnline(
                pedido,
                MetodoPago.PAYPAL,
                null, null, null, null,
                "fail@paypal.com",
                "secret1",
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PayPal no ha podido autorizar el pago");

        verify(pagoPendiente).fallar("PayPal no ha podido autorizar el pago");
        verify(pagoRepo).save(pagoPendiente);
    }

    @Test
    void procesarPagoOnline_tarjeta_datos_invalidos_no_confirma_ni_falla() {
        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("15.00"));
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.procesarPagoOnline(
                pedido,
                MetodoPago.TARJETA,
                "123",
                "Jo",
                "13/10",
                "12",
                null, null,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class);

        verify(pagoRepo, times(1)).save(any(Pago.class));
        verify(pagoRepo, never()).findById(anyLong());
        verify(pagoRepo, never()).save(argThat(p -> p instanceof Pago && ((Pago) p).getEstado() == EstadoPago.FALLIDO));
    }

    @Test
    void procesarPagoOnline_efectivo_pagaCon_menor_que_total_lanza() {
        when(pedidoCalculoService.calcularTotalPedido(pedido)).thenReturn(new BigDecimal("20.00"));
        when(pagoRepo.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.procesarPagoOnline(
                pedido,
                MetodoPago.EFECTIVO,
                null, null, null, null,
                null, null,
                new BigDecimal("10.00")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El efectivo indicado es menor que el total");
    }

    private static String futureExpiry() {
        YearMonth ym = YearMonth.now().plusMonths(6);
        String mm = String.format("%02d", ym.getMonthValue());
        String yy = String.format("%02d", ym.getYear() % 100);
        return mm + "/" + yy;
    }
}