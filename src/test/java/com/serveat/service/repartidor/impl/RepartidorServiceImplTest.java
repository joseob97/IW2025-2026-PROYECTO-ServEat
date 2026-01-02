package com.serveat.service.repartidor.impl;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.*;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.pago.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepartidorServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepo;

    @Mock
    private EmpleadoRepository empleadoRepo;

    @Mock
    private PagoService pagoService;

    @InjectMocks
    private RepartidorServiceImpl service;

    private Pedido pedido;
    private Empleado repartidor;

    @BeforeEach
    void setup() {
        repartidor = new Empleado();
        repartidor.setUsername("rep1");

        pedido = new Pedido();
        pedido.setCodigo("P1");
        pedido.setTipoPedido(TipoPedidoCliente.DOMICILIO);
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setEstadoCocina(EstadoCocina.LISTO);
        pedido.setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);
    }

    // ===========================
    // listarPedidosPendientes
    // ===========================

    @Test
    void listarPedidosPendientes_ok() {
        when(pedidoRepo.findByTipoPedidoAndEstadoRepartoAndEstadoCocina(
                TipoPedidoCliente.DOMICILIO,
                EstadoReparto.PENDIENTE_ASIGNACION,
                EstadoCocina.LISTO
        )).thenReturn(List.of(pedido));

        List<Pedido> res = service.listarPedidosPendientes();

        assertThat(res).containsExactly(pedido);
    }

    // ===========================
    // listarMisPedidos
    // ===========================

    @Test
    void listarMisPedidos_ok() {
        when(pedidoRepo.findByRepartidor_Username("rep1"))
                .thenReturn(List.of(pedido));

        List<Pedido> res = service.listarMisPedidos("rep1");

        assertThat(res).containsExactly(pedido);
    }

    @Test
    void listarMisPedidos_username_invalido() {
        assertThatThrownBy(() -> service.listarMisPedidos(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Repartidor inválido");
    }

    // ===========================
    // asignarmePedido
    // ===========================

    @Test
    void asignarmePedido_ok() {
        when(pedidoRepo.findWithDetalleByCodigo("P1"))
                .thenReturn(Optional.of(pedido));
        when(empleadoRepo.findByUsername("rep1"))
                .thenReturn(Optional.of(repartidor));
        when(pedidoRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Pedido res = service.asignarmePedido("P1", "rep1");

        assertThat(res.getEstadoReparto()).isEqualTo(EstadoReparto.ASIGNADO);
        assertThat(res.getRepartidor()).isSameAs(repartidor);
        assertThat(res.getFechaAsignacionReparto()).isNotNull();
    }

    @Test
    void asignarmePedido_no_domicilio() {
        pedido.setTipoPedido(TipoPedidoCliente.MESA);

        when(pedidoRepo.findWithDetalleByCodigo("P1"))
                .thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.asignarmePedido("P1", "rep1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este pedido no es a domicilio");
    }

    // ===========================
    // marcarEnReparto
    // ===========================

    @Test
    void marcarEnReparto_ok() {
        pedido.setEstadoReparto(EstadoReparto.ASIGNADO);
        pedido.setRepartidor(repartidor);

        when(pedidoRepo.findWithDetalleByCodigo("P1"))
                .thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Pedido res = service.marcarEnReparto("P1", "rep1");

        assertThat(res.getEstadoReparto()).isEqualTo(EstadoReparto.EN_REPARTO);
        assertThat(res.getFechaSalidaReparto()).isNotNull();
    }

    // ===========================
    // marcarEntregado
    // ===========================

    @Test
    void marcarEntregado_ok_con_pago_efectivo() {
        pedido.setEstadoReparto(EstadoReparto.EN_REPARTO);
        pedido.setRepartidor(repartidor);

        Pago pago = mock(Pago.class);
        when(pago.getMetodo()).thenReturn(MetodoPago.EFECTIVO);
        when(pago.getId()).thenReturn(1L);
        pedido.setPago(pago);

        when(pedidoRepo.findWithDetalleByCodigo("P1"))
                .thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Pedido res = service.marcarEntregado("P1", "rep1");

        assertThat(res.getEstadoReparto()).isEqualTo(EstadoReparto.ENTREGADO);
        assertThat(res.getFechaEntrega()).isNotNull();

        verify(pagoService).confirmarPago(1L, "Efectivo cobrado por repartidor");
    }

    // ===========================
    // marcarIncidencia
    // ===========================

    @Test
    void marcarIncidencia_ok_con_pago_efectivo() {
        pedido.setEstadoReparto(EstadoReparto.EN_REPARTO);
        pedido.setRepartidor(repartidor);

        Pago pago = mock(Pago.class);
        when(pago.getMetodo()).thenReturn(MetodoPago.EFECTIVO);
        when(pago.getId()).thenReturn(2L);
        pedido.setPago(pago);

        when(pedidoRepo.findWithDetalleByCodigo("P1"))
                .thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        Pedido res = service.marcarIncidencia("P1", "rep1", "Cliente ausente");

        assertThat(res.getEstadoReparto()).isEqualTo(EstadoReparto.INCIDENCIA);
        assertThat(res.getIncidenciaReparto()).isEqualTo("Cliente ausente");

        verify(pagoService)
                .marcarPagoFallido(2L, "Incidencia en entrega: Cliente ausente");
    }

    // ===========================
    // validarPedidoRepartidor (error)
    // ===========================

    @Test
    void marcarEnReparto_pedido_no_asignado() {
        pedido.setEstadoReparto(EstadoReparto.ASIGNADO);

        when(pedidoRepo.findWithDetalleByCodigo("P1"))
                .thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.marcarEnReparto("P1", "rep1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pedido no asignado a este repartidor");
    }
}