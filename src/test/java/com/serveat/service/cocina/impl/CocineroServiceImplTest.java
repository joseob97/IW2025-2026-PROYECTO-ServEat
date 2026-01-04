package com.serveat.service.cocina.impl;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.repository.pedido.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CocineroServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepo;

    @InjectMocks
    private CocineroServiceImpl service;

    @Test
    void listarPendientes_llama_repo_con_estado_en_cocina_y_pendiente_aceptacion() {
        List<Pedido> expected = List.of(mock(Pedido.class), mock(Pedido.class));
        when(pedidoRepo.findByEstadoAndEstadoCocina(EstadoPedido.EN_COCINA, EstadoCocina.PENDIENTE_ACEPTACION))
                .thenReturn(expected);

        List<Pedido> res = service.listarPendientes();

        assertThat(res).isSameAs(expected);
        verify(pedidoRepo).findByEstadoAndEstadoCocina(EstadoPedido.EN_COCINA, EstadoCocina.PENDIENTE_ACEPTACION);
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void listarPendientesAceptacion_llama_repo_con_estado_en_curso_o_en_cocina_y_pendiente_aceptacion() {
        List<Pedido> expected = List.of(mock(Pedido.class));
        when(pedidoRepo.findByEstadoOrEstadoAndEstadoCocina(
                EstadoPedido.EN_CURSO,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        )).thenReturn(expected);

        List<Pedido> res = service.listarPendientesAceptacion();

        assertThat(res).isSameAs(expected);
        verify(pedidoRepo).findByEstadoOrEstadoAndEstadoCocina(
                EstadoPedido.EN_CURSO,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void listarPedidosEnCurso_filtra_en_memoria_por_estado_en_cocina_y_estado_cocina_aceptado_o_en_preparacion() {
        Pedido p1 = mock(Pedido.class);
        when(p1.getEstado()).thenReturn(EstadoPedido.EN_COCINA);
        when(p1.getEstadoCocina()).thenReturn(EstadoCocina.ACEPTADO);

        Pedido p2 = mock(Pedido.class);
        when(p2.getEstado()).thenReturn(EstadoPedido.EN_COCINA);
        when(p2.getEstadoCocina()).thenReturn(EstadoCocina.EN_PREPARACION);

        Pedido p3 = mock(Pedido.class);
        when(p3.getEstado()).thenReturn(EstadoPedido.EN_COCINA);
        when(p3.getEstadoCocina()).thenReturn(EstadoCocina.PENDIENTE_ACEPTACION);

        Pedido p4 = mock(Pedido.class);
        when(p4.getEstado()).thenReturn(EstadoPedido.EN_CURSO);

        when(pedidoRepo.findAll()).thenReturn(List.of(p1, p2, p3, p4));

        List<Pedido> res = service.listarPedidosEnCurso();

        assertThat(res).containsExactly(p1, p2);

        verify(pedidoRepo).findAll();
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void aceptarPedido_si_no_se_encuentra_lanza_illegalArgument_y_no_guarda() {
        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.aceptarPedido("P-1", "cocinero1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pedido no encontrado");

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedidoRepo, never()).save(any());
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void aceptarPedido_si_estado_cocina_no_es_pendiente_aceptacion_lanza_illegalArgument_y_no_guarda() {
        Pedido pedido = mock(Pedido.class);
        when(pedido.getEstadoCocina()).thenReturn(EstadoCocina.ACEPTADO);
        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.aceptarPedido("P-1", "cocinero1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pedido no aceptable");

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedido, never()).setEstado(any());
        verify(pedido, never()).setEstadoCocina(any());
        verify(pedidoRepo, never()).save(any());
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void aceptarPedido_actualiza_estado_y_estado_cocina_y_auditoria_y_guarda() {
        Pedido pedido = mock(Pedido.class);
        when(pedido.getEstadoCocina()).thenReturn(EstadoCocina.PENDIENTE_ACEPTACION);
        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(pedido)).thenReturn(pedido);

        Pedido res = service.aceptarPedido("P-1", "cocinero1");

        assertThat(res).isSameAs(pedido);

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedido).setEstado(EstadoPedido.EN_COCINA);
        verify(pedido).setEstadoCocina(EstadoCocina.ACEPTADO);
        verify(pedido).setModificadoPor("cocinero1");
        verify(pedido).setFechaUltimaModificacion(any(LocalDateTime.class));
        verify(pedidoRepo).save(pedido);
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void marcarEnPreparacion_si_estado_cocina_no_es_aceptado_lanza_illegalArgument_y_no_guarda() {
        Pedido pedido = mock(Pedido.class);
        when(pedido.getEstadoCocina()).thenReturn(EstadoCocina.PENDIENTE_ACEPTACION);
        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.marcarEnPreparacion("P-1", "cocinero1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El pedido no está aceptado");

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedidoRepo, never()).save(any());
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void marcarEnPreparacion_actualiza_estado_cocina_y_auditoria_y_guarda() {
        Pedido pedido = mock(Pedido.class);
        when(pedido.getEstadoCocina()).thenReturn(EstadoCocina.ACEPTADO);
        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(pedido)).thenReturn(pedido);

        Pedido res = service.marcarEnPreparacion("P-1", "cocinero1");

        assertThat(res).isSameAs(pedido);

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedido).setEstadoCocina(EstadoCocina.EN_PREPARACION);
        verify(pedido).setModificadoPor("cocinero1");
        verify(pedido).setFechaUltimaModificacion(any(LocalDateTime.class));
        verify(pedidoRepo).save(pedido);
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void marcarListo_si_estado_cocina_no_es_en_preparacion_lanza_illegalArgument_y_no_guarda() {
        Pedido pedido = mock(Pedido.class);
        when(pedido.getEstadoCocina()).thenReturn(EstadoCocina.ACEPTADO);
        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> service.marcarListo("P-1", "cocinero1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El pedido no está en preparación");

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedidoRepo, never()).save(any());
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void marcarListo_si_no_es_domicilio_no_toca_estado_reparto_y_guarda() {
        Pedido pedido = mock(Pedido.class);
        when(pedido.getEstadoCocina()).thenReturn(EstadoCocina.EN_PREPARACION);
        when(pedido.getTipoPedido()).thenReturn(TipoPedidoCliente.MESA);

        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(pedido)).thenReturn(pedido);

        Pedido res = service.marcarListo("P-1", "cocinero1");

        assertThat(res).isSameAs(pedido);

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedido).setEstadoCocina(EstadoCocina.LISTO);
        verify(pedido, never()).setEstadoReparto(any());
        verify(pedido).setModificadoPor("cocinero1");
        verify(pedido).setFechaUltimaModificacion(any(LocalDateTime.class));
        verify(pedidoRepo).save(pedido);
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void marcarListo_si_es_domicilio_setea_estado_reparto_pendiente_asignacion_y_guarda() {
        Pedido pedido = mock(Pedido.class);
        when(pedido.getEstadoCocina()).thenReturn(EstadoCocina.EN_PREPARACION);
        when(pedido.getTipoPedido()).thenReturn(TipoPedidoCliente.DOMICILIO);

        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(pedido)).thenReturn(pedido);

        Pedido res = service.marcarListo("P-1", "cocinero1");

        assertThat(res).isSameAs(pedido);

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedido).setEstadoCocina(EstadoCocina.LISTO);
        verify(pedido).setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);
        verify(pedido).setModificadoPor("cocinero1");
        verify(pedido).setFechaUltimaModificacion(any(LocalDateTime.class));
        verify(pedidoRepo).save(pedido);
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void cancelarDesdeCocina_setea_campos_de_cancelacion_y_guarda() {
        Pedido pedido = mock(Pedido.class);
        when(pedidoRepo.findWithDetalleByCodigo("P-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepo.save(pedido)).thenReturn(pedido);

        Pedido res = service.cancelarDesdeCocina("P-1", "Sin stock", "cocinero1");

        assertThat(res).isSameAs(pedido);

        verify(pedidoRepo).findWithDetalleByCodigo("P-1");
        verify(pedido).setEstadoCocina(EstadoCocina.CANCELADO);
        verify(pedido).setEstado(EstadoPedido.ANULADO);
        verify(pedido).setCanceladoPor("cocinero1");
        verify(pedido).setMotivoCancelacion("Sin stock");
        verify(pedido).setFechaCancelacion(any(LocalDateTime.class));
        verify(pedido).setModificadoPor("cocinero1");
        verify(pedido).setFechaUltimaModificacion(any(LocalDateTime.class));
        verify(pedidoRepo).save(pedido);
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void buscarPendientesAceptacion_delega_en_repo_buscar_pedidos_cocina_historico_con_estado_pendiente_aceptacion() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 2, 0, 0);
        Integer mesa = 7;
        Pageable pageable = mock(Pageable.class);

        @SuppressWarnings("unchecked")
        Page<Pedido> expected = (Page<Pedido>) mock(Page.class);

        when(pedidoRepo.buscarPedidosCocinaHistorico(desde, hasta, EstadoCocina.PENDIENTE_ACEPTACION, mesa, pageable))
                .thenReturn(expected);

        Page<Pedido> res = service.buscarPendientesAceptacion(desde, hasta, mesa, pageable);

        assertThat(res).isSameAs(expected);

        verify(pedidoRepo).buscarPedidosCocinaHistorico(desde, hasta, EstadoCocina.PENDIENTE_ACEPTACION, mesa, pageable);
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void buscarPedidosCocinaHoy_delega_en_repo() {
        LocalDateTime desde = LocalDateTime.of(2026, 1, 4, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 4, 23, 59);
        EstadoCocina estado = EstadoCocina.EN_PREPARACION;
        Integer mesa = null;
        Pageable pageable = mock(Pageable.class);

        @SuppressWarnings("unchecked")
        Page<Pedido> expected = (Page<Pedido>) mock(Page.class);

        when(pedidoRepo.buscarPedidosCocinaHoy(desde, hasta, estado, mesa, pageable)).thenReturn(expected);

        Page<Pedido> res = service.buscarPedidosCocinaHoy(desde, hasta, estado, mesa, pageable);

        assertThat(res).isSameAs(expected);

        verify(pedidoRepo).buscarPedidosCocinaHoy(desde, hasta, estado, mesa, pageable);
        verifyNoMoreInteractions(pedidoRepo);
    }

    @Test
    void buscarPedidosCocinaHistorico_delega_en_repo() {
        LocalDateTime desde = LocalDateTime.of(2025, 12, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2025, 12, 31, 23, 59);
        EstadoCocina estado = EstadoCocina.LISTO;
        Integer mesa = 3;
        Pageable pageable = mock(Pageable.class);

        @SuppressWarnings("unchecked")
        Page<Pedido> expected = (Page<Pedido>) mock(Page.class);

        when(pedidoRepo.buscarPedidosCocinaHistorico(desde, hasta, estado, mesa, pageable)).thenReturn(expected);

        Page<Pedido> res = service.buscarPedidosCocinaHistorico(desde, hasta, estado, mesa, pageable);

        assertThat(res).isSameAs(expected);

        verify(pedidoRepo).buscarPedidosCocinaHistorico(desde, hasta, estado, mesa, pageable);
        verifyNoMoreInteractions(pedidoRepo);
    }
}