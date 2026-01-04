package com.serveat.service.pedido.seguimiento.impl;

import com.serveat.domain.pedido.*;
import com.serveat.repository.pedido.PedidoSeguimientoRepository;
import com.serveat.service.pedido.seguimiento.PedidoSeguimientoDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoSeguimientoServiceImplTest {

    @Mock
    private PedidoSeguimientoRepository seguimientoRepo;

    @InjectMocks
    private PedidoSeguimientoServiceImpl service;

    @Test
    void buscarActivosCliente_si_username_invalido_lanza_illegalArgument() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.buscarActivosCliente(
                "  ", null, null, null, null, null, pageable
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario inválido");

        verifyNoInteractions(seguimientoRepo);
    }

    @Test
    void buscarActivosCliente_si_rango_invalido_lanza_illegalArgument() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.minusMinutes(1);

        assertThatThrownBy(() -> service.buscarActivosCliente(
                "cliente", desde, hasta, null, null, null, pageable
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rango de fechas inválido: 'desde' > 'hasta'");

        verifyNoInteractions(seguimientoRepo);
    }

    @Test
    void buscarActivosCliente_llama_repo_con_parametros_y_devuelve_page() {
        Pageable pageable = PageRequest.of(1, 5);
        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now();
        EstadoPedido ep = EstadoPedido.EN_COCINA;
        EstadoCocina ec = EstadoCocina.EN_PREPARACION;
        EstadoReparto er = EstadoReparto.ASIGNADO;

        Page<Pedido> expected = new PageImpl<>(java.util.List.of(new Pedido()), pageable, 1);

        when(seguimientoRepo.buscarActivosClienteFiltrados(
                "cliente", desde, hasta, ep, ec, er, pageable
        )).thenReturn(expected);

        Page<Pedido> res = service.buscarActivosCliente("cliente", desde, hasta, ep, ec, er, pageable);

        assertThat(res).isSameAs(expected);

        verify(seguimientoRepo).buscarActivosClienteFiltrados(
                "cliente", desde, hasta, ep, ec, er, pageable
        );
        verifyNoMoreInteractions(seguimientoRepo);
    }

    @Test
    void buscarAnterioresCliente_si_username_invalido_lanza_illegalArgument() {
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> service.buscarAnterioresCliente(
                null, null, null, null, null, null, pageable
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario inválido");

        verifyNoInteractions(seguimientoRepo);
    }

    @Test
    void buscarAnterioresCliente_si_rango_invalido_lanza_illegalArgument() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.now();
        LocalDateTime hasta = desde.minusSeconds(1);

        assertThatThrownBy(() -> service.buscarAnterioresCliente(
                "cliente", desde, hasta, null, null, null, pageable
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Rango de fechas inválido: 'desde' > 'hasta'");

        verifyNoInteractions(seguimientoRepo);
    }

    @Test
    void buscarAnterioresCliente_llama_repo_con_parametros_y_devuelve_page() {
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime desde = LocalDateTime.now().minusDays(30);
        LocalDateTime hasta = LocalDateTime.now().minusDays(1);

        Page<Pedido> expected = new PageImpl<>(java.util.List.of(new Pedido()), pageable, 1);

        when(seguimientoRepo.buscarAnterioresClienteFiltrados(
                eq("cliente"),
                eq(desde),
                eq(hasta),
                isNull(),
                isNull(),
                isNull(),
                eq(pageable)
        )).thenReturn(expected);

        Page<Pedido> res = service.buscarAnterioresCliente("cliente", desde, hasta, null, null, null, pageable);

        assertThat(res).isSameAs(expected);

        verify(seguimientoRepo).buscarAnterioresClienteFiltrados(
                eq("cliente"),
                eq(desde),
                eq(hasta),
                isNull(),
                isNull(),
                isNull(),
                eq(pageable)
        );
        verifyNoMoreInteractions(seguimientoRepo);
    }

    @Test
    void obtenerSeguimientoPedidoCliente_si_codigo_invalido_lanza_illegalArgument() {
        assertThatThrownBy(() -> service.obtenerSeguimientoPedidoCliente(" ", "cliente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Código inválido");

        verifyNoInteractions(seguimientoRepo);
    }

    @Test
    void obtenerSeguimientoPedidoCliente_si_username_invalido_lanza_illegalArgument() {
        assertThatThrownBy(() -> service.obtenerSeguimientoPedidoCliente("PED-1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Usuario inválido");

        verifyNoInteractions(seguimientoRepo);
    }

    @Test
    void obtenerSeguimientoPedidoCliente_si_no_existe_lanza_illegalArgument() {
        when(seguimientoRepo.findWithDetalleByCodigoAndCliente_Username("PED-1", "cliente"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerSeguimientoPedidoCliente("PED-1", "cliente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pedido no encontrado o no pertenece al cliente");

        verify(seguimientoRepo).findWithDetalleByCodigoAndCliente_Username("PED-1", "cliente");
        verifyNoMoreInteractions(seguimientoRepo);
    }

    @Test
    void obtenerSeguimientoPedidoCliente_si_anulado_devuelve_tiempo_guion_y_progreso_1_y_estilos_error() {
        Pedido p = new Pedido();
        p.setCodigo("PED-ANU");
        p.setEstado(EstadoPedido.ANULADO);
        p.setEstadoCocina(EstadoCocina.CANCELADO);
        p.setEstadoReparto(EstadoReparto.NO_APLICA);
        p.setTipoPedido(TipoPedidoCliente.MESA);
        p.setFechaCreacion(LocalDateTime.now().minusMinutes(30));

        when(seguimientoRepo.findWithDetalleByCodigoAndCliente_Username("PED-ANU", "cliente"))
                .thenReturn(Optional.of(p));

        PedidoSeguimientoDTO dto = service.obtenerSeguimientoPedidoCliente("PED-ANU", "cliente");

        assertThat(dto.getCodigoPedido()).isEqualTo("PED-ANU");
        assertThat(dto.getEstadoPedido()).isEqualTo("ANULADO");
        assertThat(dto.getEstadoCocina()).isEqualTo("CANCELADO");
        assertThat(dto.getEstadoReparto()).isEqualTo("NO_APLICA");
        assertThat(dto.getEtiquetaTiempoRestante()).isEqualTo("-");
        assertThat(dto.getProgreso()).isEqualTo(1.0);

        assertThat(dto.getEstiloPedido()).isEqualTo("ERROR");
        assertThat(dto.getEstiloCocina()).isEqualTo("ERROR");
        assertThat(dto.getEstiloReparto()).isEqualTo("NEUTRO");

        assertThat(dto.getMensaje()).isNull();

        verify(seguimientoRepo).findWithDetalleByCodigoAndCliente_Username("PED-ANU", "cliente");
        verifyNoMoreInteractions(seguimientoRepo);
    }

    @Test
    void obtenerSeguimientoPedidoCliente_si_tiene_incidencia_la_expone_como_mensaje() {
        Pedido p = new Pedido();
        p.setCodigo("PED-INC");
        p.setEstado(EstadoPedido.EN_COCINA);
        p.setEstadoCocina(EstadoCocina.EN_PREPARACION);
        p.setEstadoReparto(EstadoReparto.EN_REPARTO);
        p.setTipoPedido(TipoPedidoCliente.DOMICILIO);
        p.setFechaCreacion(LocalDateTime.now().minusMinutes(5));
        p.setIncidenciaReparto("Repartidor con retraso por tráfico");

        when(seguimientoRepo.findWithDetalleByCodigoAndCliente_Username("PED-INC", "cliente"))
                .thenReturn(Optional.of(p));

        PedidoSeguimientoDTO dto = service.obtenerSeguimientoPedidoCliente("PED-INC", "cliente");

        assertThat(dto.getMensaje()).isEqualTo("Repartidor con retraso por tráfico");
        assertThat(dto.getEstiloPedido()).isEqualTo("INFO");
        assertThat(dto.getEstiloCocina()).isEqualTo("INFO");
        assertThat(dto.getEstiloReparto()).isEqualTo("INFO");

        assertThat(dto.getEtiquetaTiempoRestante()).isNotBlank();
        assertThat(dto.getProgreso()).isNotNull();
        assertThat(dto.getProgreso()).isBetween(0.0, 1.0);

        verify(seguimientoRepo).findWithDetalleByCodigoAndCliente_Username("PED-INC", "cliente");
        verifyNoMoreInteractions(seguimientoRepo);
    }

    @Test
    void obtenerSeguimientoPedidoCliente_si_listo_no_domicilio_devuelve_indeterminada_por_total_estimado_0() {
        Pedido p = new Pedido();
        p.setCodigo("PED-LISTO");
        p.setEstado(EstadoPedido.EN_COCINA);
        p.setEstadoCocina(EstadoCocina.LISTO);
        p.setEstadoReparto(EstadoReparto.NO_APLICA);
        p.setTipoPedido(TipoPedidoCliente.RECOGER);
        p.setFechaCreacion(LocalDateTime.now().minusMinutes(20));

        when(seguimientoRepo.findWithDetalleByCodigoAndCliente_Username("PED-LISTO", "cliente"))
                .thenReturn(Optional.of(p));

        PedidoSeguimientoDTO dto = service.obtenerSeguimientoPedidoCliente("PED-LISTO", "cliente");

        assertThat(dto.getEtiquetaTiempoRestante()).isEqualTo("-");
        assertThat(dto.getProgreso()).isNull();

        assertThat(dto.getEstiloCocina()).isEqualTo("OK");
        assertThat(dto.getEstiloReparto()).isEqualTo("NEUTRO");

        verify(seguimientoRepo).findWithDetalleByCodigoAndCliente_Username("PED-LISTO", "cliente");
        verifyNoMoreInteractions(seguimientoRepo);
    }

    @Test
    void obtenerSeguimientoPedidoCliente_si_entregado_devuelve_indeterminada_por_total_estimado_0() {
        Pedido p = new Pedido();
        p.setCodigo("PED-ENT");
        p.setEstado(EstadoPedido.EN_COCINA);
        p.setEstadoCocina(EstadoCocina.LISTO);
        p.setEstadoReparto(EstadoReparto.ENTREGADO);
        p.setTipoPedido(TipoPedidoCliente.DOMICILIO);
        p.setFechaCreacion(LocalDateTime.now().minusMinutes(60));
        p.setFechaEntrega(LocalDateTime.now().minusMinutes(5));

        when(seguimientoRepo.findWithDetalleByCodigoAndCliente_Username("PED-ENT", "cliente"))
                .thenReturn(Optional.of(p));

        PedidoSeguimientoDTO dto = service.obtenerSeguimientoPedidoCliente("PED-ENT", "cliente");

        assertThat(dto.getEtiquetaTiempoRestante()).isEqualTo("-");
        assertThat(dto.getProgreso()).isNull();
        assertThat(dto.getEstiloReparto()).isEqualTo("OK");

        verify(seguimientoRepo).findWithDetalleByCodigoAndCliente_Username("PED-ENT", "cliente");
        verifyNoMoreInteractions(seguimientoRepo);
    }

    @Test
    void obtenerSeguimientoPedidoCliente_si_campos_estado_null_usa_guion_y_estilo_neutro_y_estimacion_indeterminada() {
        Pedido p = new Pedido();
        p.setCodigo("PED-NULL");
        p.setEstado(null);
        p.setEstadoCocina(null);
        p.setEstadoReparto(null);
        p.setTipoPedido(TipoPedidoCliente.DOMICILIO);
        p.setFechaCreacion(LocalDateTime.now().minusMinutes(1));

        when(seguimientoRepo.findWithDetalleByCodigoAndCliente_Username("PED-NULL", "cliente"))
                .thenReturn(Optional.of(p));

        PedidoSeguimientoDTO dto = service.obtenerSeguimientoPedidoCliente("PED-NULL", "cliente");

        assertThat(dto.getEstadoPedido()).isEqualTo("-");
        assertThat(dto.getEstadoCocina()).isEqualTo("-");
        assertThat(dto.getEstadoReparto()).isEqualTo("-");
        assertThat(dto.getEstiloPedido()).isEqualTo("NEUTRO");
        assertThat(dto.getEstiloCocina()).isEqualTo("NEUTRO");
        assertThat(dto.getEstiloReparto()).isEqualTo("NEUTRO");
        assertThat(dto.getEtiquetaTiempoRestante()).isNotBlank();
        assertThat(dto.getProgreso()).isNotNull();

        verify(seguimientoRepo).findWithDetalleByCodigoAndCliente_Username("PED-NULL", "cliente");
        verifyNoMoreInteractions(seguimientoRepo);
    }
}