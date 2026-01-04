package com.serveat.service.pago.impl;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pago.ajuste.AjustePago;
import com.serveat.domain.pago.ajuste.EstadoAjustePago;
import com.serveat.domain.pago.ajuste.TipoAjustePago;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.AjustePagoRepository;
import com.serveat.service.pago.AjustePagoDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AjustePagoServiceImplTest {

    @Mock
    private AjustePagoRepository ajusteRepo;

    @InjectMocks
    private AjustePagoServiceImpl service;

    @Test
    void calcularYCrearOActualizarAjuste_si_pedido_null_lanza_illegalArgument() {
        assertThatThrownBy(() -> service.calcularYCrearOActualizarAjuste(
                null, null, BigDecimal.ZERO, BigDecimal.ZERO
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pedido inválido");

        verifyNoInteractions(ajusteRepo);
    }

    @Test
    void calcularYCrearOActualizarAjuste_si_diff_cero_cancela_pendiente_si_existe_y_devuelve_ninguna() {
        Pedido pedido = pedidoConCodigo("PED-1");

        AjustePago pendiente = ajustePagoPendiente(pedido, "AP-OLD", TipoAjustePago.COBRO, new BigDecimal("2.00"));
        when(ajusteRepo.findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-1", EstadoAjustePago.PENDIENTE
        )).thenReturn(Optional.of(pendiente));

        ArgumentCaptor<AjustePago> captor = ArgumentCaptor.forClass(AjustePago.class);

        AjustePagoDTO res = service.calcularYCrearOActualizarAjuste(
                pedido,
                pagoConfirmado(MetodoPago.TARJETA),
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        );

        assertThat(res.getCodigoPedido()).isEqualTo("PED-1");
        assertThat(res.getDiferenciaAbs()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(res.getAccion()).isEqualTo(AjustePagoDTO.Accion.NINGUNA);
        assertThat(res.getMetodoPagoOriginal()).isEqualTo(MetodoPago.TARJETA);
        assertThat(res.getCodigoAjuste()).isNull();
        assertThat(res.getTipoAjuste()).isNull();
        assertThat(res.getEstadoAjuste()).isNull();

        verify(ajusteRepo).findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-1", EstadoAjustePago.PENDIENTE
        );
        verify(ajusteRepo).save(captor.capture());
        verifyNoMoreInteractions(ajusteRepo);

        AjustePago saved = captor.getValue();
        assertThat(saved).isSameAs(pendiente);
        assertThat(saved.getEstado()).isEqualTo(EstadoAjustePago.CANCELADO);
    }

    @Test
    void calcularYCrearOActualizarAjuste_si_diff_cero_y_no_hay_pendiente_no_guarda() {
        Pedido pedido = pedidoConCodigo("PED-1");

        when(ajusteRepo.findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-1", EstadoAjustePago.PENDIENTE
        )).thenReturn(Optional.empty());

        AjustePagoDTO res = service.calcularYCrearOActualizarAjuste(
                pedido,
                pagoConfirmado(MetodoPago.PAYPAL),
                new BigDecimal("10.00"),
                new BigDecimal("10.00")
        );

        assertThat(res.getAccion()).isEqualTo(AjustePagoDTO.Accion.NINGUNA);
        assertThat(res.getMetodoPagoOriginal()).isEqualTo(MetodoPago.PAYPAL);

        verify(ajusteRepo).findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-1", EstadoAjustePago.PENDIENTE
        );
        verifyNoMoreInteractions(ajusteRepo);
    }

    @Test
    void calcularYCrearOActualizarAjuste_si_no_hay_pago_asociado_no_genera_ajuste_y_cancela_pendiente() {
        Pedido pedido = pedidoConCodigo("PED-2");

        AjustePago pendiente = ajustePagoPendiente(pedido, "AP-OLD", TipoAjustePago.DEVOLUCION, new BigDecimal("1.00"));
        when(ajusteRepo.findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-2", EstadoAjustePago.PENDIENTE
        )).thenReturn(Optional.of(pendiente));

        ArgumentCaptor<AjustePago> captor = ArgumentCaptor.forClass(AjustePago.class);

        AjustePagoDTO res = service.calcularYCrearOActualizarAjuste(
                pedido,
                null,
                new BigDecimal("10.00"),
                new BigDecimal("12.00")
        );

        assertThat(res.getAccion()).isEqualTo(AjustePagoDTO.Accion.NINGUNA);
        assertThat(res.getMetodoPagoOriginal()).isNull();
        assertThat(res.getCodigoAjuste()).isNull();
        assertThat(res.getTipoAjuste()).isNull();
        assertThat(res.getEstadoAjuste()).isNull();
        assertThat(res.getDiferenciaAbs()).isEqualByComparingTo(new BigDecimal("2.00"));

        verify(ajusteRepo).findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-2", EstadoAjustePago.PENDIENTE
        );
        verify(ajusteRepo).save(captor.capture());
        verifyNoMoreInteractions(ajusteRepo);

        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoAjustePago.CANCELADO);
    }

    @Test
    void calcularYCrearOActualizarAjuste_si_pago_no_confirmado_no_genera_ajuste_y_cancela_pendiente() {
        Pedido pedido = pedidoConCodigo("PED-3");
        Pago pago = pagoNoConfirmado(MetodoPago.TARJETA);

        AjustePago pendiente = ajustePagoPendiente(pedido, "AP-OLD", TipoAjustePago.COBRO, new BigDecimal("3.00"));
        when(ajusteRepo.findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-3", EstadoAjustePago.PENDIENTE
        )).thenReturn(Optional.of(pendiente));

        ArgumentCaptor<AjustePago> captor = ArgumentCaptor.forClass(AjustePago.class);

        AjustePagoDTO res = service.calcularYCrearOActualizarAjuste(
                pedido,
                pago,
                new BigDecimal("10.00"),
                new BigDecimal("8.00")
        );

        assertThat(res.getAccion()).isEqualTo(AjustePagoDTO.Accion.NINGUNA);
        assertThat(res.getMetodoPagoOriginal()).isEqualTo(MetodoPago.TARJETA);
        assertThat(res.getCodigoAjuste()).isNull();
        assertThat(res.getTipoAjuste()).isNull();
        assertThat(res.getEstadoAjuste()).isNull();
        assertThat(res.getDiferenciaAbs()).isEqualByComparingTo(new BigDecimal("2.00"));

        verify(ajusteRepo).findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-3", EstadoAjustePago.PENDIENTE
        );
        verify(ajusteRepo).save(captor.capture());
        verifyNoMoreInteractions(ajusteRepo);

        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoAjustePago.CANCELADO);
    }

    @Test
    void calcularYCrearOActualizarAjuste_si_metodo_efectivo_no_crea_ni_actualiza_ajuste_y_cancela_pendiente() {
        Pedido pedido = pedidoConCodigo("PED-4");
        Pago pago = pagoConfirmado(MetodoPago.EFECTIVO);

        AjustePago pendiente = ajustePagoPendiente(pedido, "AP-OLD", TipoAjustePago.COBRO, new BigDecimal("1.00"));
        when(ajusteRepo.findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-4", EstadoAjustePago.PENDIENTE
        )).thenReturn(Optional.of(pendiente));

        ArgumentCaptor<AjustePago> captor = ArgumentCaptor.forClass(AjustePago.class);

        AjustePagoDTO res = service.calcularYCrearOActualizarAjuste(
                pedido,
                pago,
                new BigDecimal("10.00"),
                new BigDecimal("12.00")
        );

        assertThat(res.getAccion()).isEqualTo(AjustePagoDTO.Accion.COBRAR_DIFERENCIA);
        assertThat(res.getMetodoPagoOriginal()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(res.getCodigoAjuste()).isNull();
        assertThat(res.getTipoAjuste()).isNull();
        assertThat(res.getEstadoAjuste()).isNull();
        assertThat(res.getDiferenciaAbs()).isEqualByComparingTo(new BigDecimal("2.00"));

        verify(ajusteRepo).findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-4", EstadoAjustePago.PENDIENTE
        );
        verify(ajusteRepo).save(captor.capture());
        verifyNoMoreInteractions(ajusteRepo);

        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoAjustePago.CANCELADO);
    }

    @Test
    void calcularYCrearOActualizarAjuste_si_no_hay_pendiente_y_metodo_no_efectivo_crea_ajuste_y_guarda() {
        Pedido pedido = pedidoConCodigo("PED-5");
        Pago pago = pagoConfirmado(MetodoPago.TARJETA);

        when(ajusteRepo.findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-5", EstadoAjustePago.PENDIENTE
        )).thenReturn(Optional.empty());

        ArgumentCaptor<AjustePago> captor = ArgumentCaptor.forClass(AjustePago.class);
        when(ajusteRepo.save(any(AjustePago.class))).thenAnswer(inv -> inv.getArgument(0));

        AjustePagoDTO res = service.calcularYCrearOActualizarAjuste(
                pedido,
                pago,
                new BigDecimal("10.00"),
                new BigDecimal("12.50")
        );

        assertThat(res.getAccion()).isEqualTo(AjustePagoDTO.Accion.COBRAR_DIFERENCIA);
        assertThat(res.getMetodoPagoOriginal()).isEqualTo(MetodoPago.TARJETA);
        assertThat(res.getDiferenciaAbs()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(res.getCodigoAjuste()).isNotBlank();
        assertThat(res.getTipoAjuste()).isEqualTo(TipoAjustePago.COBRO);
        assertThat(res.getEstadoAjuste()).isEqualTo(EstadoAjustePago.PENDIENTE);

        verify(ajusteRepo).findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-5", EstadoAjustePago.PENDIENTE
        );
        verify(ajusteRepo).save(captor.capture());
        verifyNoMoreInteractions(ajusteRepo);

        AjustePago saved = captor.getValue();
        assertThat(saved.getPedido()).isSameAs(pedido);
        assertThat(saved.getTipo()).isEqualTo(TipoAjustePago.COBRO);
        assertThat(saved.getImporte()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(saved.getEstado()).isEqualTo(EstadoAjustePago.PENDIENTE);
        assertThat(saved.getCodigo()).startsWith("AP-");
    }

    @Test
    void calcularYCrearOActualizarAjuste_si_hay_pendiente_actualiza_tipo_importe_y_guarda() {
        Pedido pedido = pedidoConCodigo("PED-6");
        Pago pago = pagoConfirmado(MetodoPago.PAYPAL);

        AjustePago pendiente = ajustePagoPendiente(pedido, "AP-EXISTE", TipoAjustePago.COBRO, new BigDecimal("1.00"));

        when(ajusteRepo.findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-6", EstadoAjustePago.PENDIENTE
        )).thenReturn(Optional.of(pendiente));

        when(ajusteRepo.save(any(AjustePago.class))).thenAnswer(inv -> inv.getArgument(0));

        AjustePagoDTO res = service.calcularYCrearOActualizarAjuste(
                pedido,
                pago,
                new BigDecimal("10.00"),
                new BigDecimal("8.25")
        );

        assertThat(res.getAccion()).isEqualTo(AjustePagoDTO.Accion.DEVOLVER_DIFERENCIA);
        assertThat(res.getMetodoPagoOriginal()).isEqualTo(MetodoPago.PAYPAL);
        assertThat(res.getCodigoAjuste()).isEqualTo("AP-EXISTE");
        assertThat(res.getTipoAjuste()).isEqualTo(TipoAjustePago.DEVOLUCION);
        assertThat(res.getEstadoAjuste()).isEqualTo(EstadoAjustePago.PENDIENTE);
        assertThat(res.getDiferenciaAbs()).isEqualByComparingTo(new BigDecimal("1.75"));

        verify(ajusteRepo).findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "PED-6", EstadoAjustePago.PENDIENTE
        );
        verify(ajusteRepo).save(pendiente);
        verifyNoMoreInteractions(ajusteRepo);

        assertThat(pendiente.getTipo()).isEqualTo(TipoAjustePago.DEVOLUCION);
        assertThat(pendiente.getImporte()).isEqualByComparingTo(new BigDecimal("1.75"));
        assertThat(pendiente.getEstado()).isEqualTo(EstadoAjustePago.PENDIENTE);
    }

    @Test
    void obtenerDetallePorCodigo_si_codigo_invalido_lanza_illegalArgument() {
        assertThatThrownBy(() -> service.obtenerDetallePorCodigo(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Código de ajuste inválido");

        verifyNoInteractions(ajusteRepo);
    }

    @Test
    void obtenerDetallePorCodigo_si_no_existe_lanza_illegalArgument_con_codigo() {
        when(ajusteRepo.findByCodigo("AP-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerDetallePorCodigo("AP-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ajuste no encontrado: AP-1");

        verify(ajusteRepo).findByCodigo("AP-1");
        verifyNoMoreInteractions(ajusteRepo);
    }

    @Test
    void obtenerDetallePorCodigo_si_existe_devuelve_dto_con_estado_tipo_e_importe() {
        Pedido pedido = pedidoConCodigo("PED-7");

        AjustePago a = new AjustePago(pedido, "AP-DET", TipoAjustePago.COBRO, new BigDecimal("3.33"));
        a.setEstado(EstadoAjustePago.PENDIENTE);

        when(ajusteRepo.findByCodigo("AP-DET")).thenReturn(Optional.of(a));

        AjustePagoDTO res = service.obtenerDetallePorCodigo("AP-DET");

        assertThat(res.getCodigoPedido()).isEqualTo("PED-7");
        assertThat(res.getCodigoAjuste()).isEqualTo("AP-DET");
        assertThat(res.getTipoAjuste()).isEqualTo(TipoAjustePago.COBRO);
        assertThat(res.getEstadoAjuste()).isEqualTo(EstadoAjustePago.PENDIENTE);
        assertThat(res.getDiferenciaAbs()).isEqualByComparingTo(new BigDecimal("3.33"));

        verify(ajusteRepo).findByCodigo("AP-DET");
        verifyNoMoreInteractions(ajusteRepo);
    }

    @Test
    void completarAjuste_si_no_pendiente_lanza_illegalState() {
        AjustePago a = new AjustePago(pedidoConCodigo("PED-8"), "AP-X", TipoAjustePago.COBRO, new BigDecimal("1.00"));
        a.setEstado(EstadoAjustePago.COMPLETADO);

        when(ajusteRepo.findByCodigo("AP-X")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.completarAjuste("AP-X", "ref"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("El ajuste no está pendiente");

        verify(ajusteRepo).findByCodigo("AP-X");
        verifyNoMoreInteractions(ajusteRepo);
    }

    @Test
    void completarAjuste_si_pendiente_marca_completado_setea_fecha_y_ref_y_guarda() {
        AjustePago a = new AjustePago(pedidoConCodigo("PED-9"), "AP-C", TipoAjustePago.DEVOLUCION, new BigDecimal("4.00"));
        a.setEstado(EstadoAjustePago.PENDIENTE);

        when(ajusteRepo.findByCodigo("AP-C")).thenReturn(Optional.of(a));
        when(ajusteRepo.save(any(AjustePago.class))).thenAnswer(inv -> inv.getArgument(0));

        AjustePagoDTO res = service.completarAjuste("AP-C", "  REF-123  ");

        assertThat(res.getCodigoAjuste()).isEqualTo("AP-C");
        assertThat(res.getTipoAjuste()).isEqualTo(TipoAjustePago.DEVOLUCION);
        assertThat(res.getEstadoAjuste()).isEqualTo(EstadoAjustePago.COMPLETADO);
        assertThat(res.getMensaje()).isEqualTo("Ajuste completado correctamente.");

        verify(ajusteRepo).findByCodigo("AP-C");
        verify(ajusteRepo).save(a);
        verifyNoMoreInteractions(ajusteRepo);

        assertThat(a.getEstado()).isEqualTo(EstadoAjustePago.COMPLETADO);
        assertThat(a.getFechaCompletado()).isNotNull();
        assertThat(a.getReferenciaProveedor()).isEqualTo("REF-123");
    }

    @Test
    void completarAjuste_si_ref_vacia_setea_ref_a_null() {
        AjustePago a = new AjustePago(pedidoConCodigo("PED-10"), "AP-C2", TipoAjustePago.COBRO, new BigDecimal("2.00"));
        a.setEstado(EstadoAjustePago.PENDIENTE);

        when(ajusteRepo.findByCodigo("AP-C2")).thenReturn(Optional.of(a));
        when(ajusteRepo.save(any(AjustePago.class))).thenAnswer(inv -> inv.getArgument(0));

        service.completarAjuste("AP-C2", "  ");

        verify(ajusteRepo).findByCodigo("AP-C2");
        verify(ajusteRepo).save(a);
        verifyNoMoreInteractions(ajusteRepo);

        assertThat(a.getReferenciaProveedor()).isNull();
        assertThat(a.getEstado()).isEqualTo(EstadoAjustePago.COMPLETADO);
        assertThat(a.getFechaCompletado()).isNotNull();
    }

    @Test
    void cancelarAjuste_si_no_pendiente_lanza_illegalState() {
        AjustePago a = new AjustePago(pedidoConCodigo("PED-11"), "AP-Z", TipoAjustePago.COBRO, new BigDecimal("1.00"));
        a.setEstado(EstadoAjustePago.CANCELADO);

        when(ajusteRepo.findByCodigo("AP-Z")).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.cancelarAjuste("AP-Z", "motivo"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Solo se puede cancelar un ajuste pendiente");

        verify(ajusteRepo).findByCodigo("AP-Z");
        verifyNoMoreInteractions(ajusteRepo);
    }

    @Test
    void cancelarAjuste_si_pendiente_marca_cancelado_y_guarda() {
        Pedido pedido = pedidoConCodigo("PED-12");
        AjustePago a = new AjustePago(pedido, "AP-CAN", TipoAjustePago.COBRO, new BigDecimal("5.00"));
        a.setEstado(EstadoAjustePago.PENDIENTE);

        when(ajusteRepo.findByCodigo("AP-CAN")).thenReturn(Optional.of(a));
        when(ajusteRepo.save(any(AjustePago.class))).thenAnswer(inv -> inv.getArgument(0));

        AjustePagoDTO res = service.cancelarAjuste("AP-CAN", "motivo");

        assertThat(res.getCodigoPedido()).isEqualTo("PED-12");
        assertThat(res.getCodigoAjuste()).isEqualTo("AP-CAN");
        assertThat(res.getEstadoAjuste()).isEqualTo(EstadoAjustePago.CANCELADO);
        assertThat(res.getMensaje()).isEqualTo("Ajuste cancelado correctamente.");

        verify(ajusteRepo).findByCodigo("AP-CAN");
        verify(ajusteRepo).save(a);
        verifyNoMoreInteractions(ajusteRepo);

        assertThat(a.getEstado()).isEqualTo(EstadoAjustePago.CANCELADO);
    }

    @Test
    void crearBorradorPedidoParaEdicion_si_original_null_devuelve_null() {
        Pedido res = service.crearBorradorPedidoParaEdicion(null);
        assertThat(res).isNull();
        verifyNoInteractions(ajusteRepo);
    }

    @Test
    void crearBorradorPedidoParaEdicion_copia_campos_y_lineas_con_ingredientes_sin_reutilizar_instancias() {
        Pedido original = new Pedido();
        original.setCodigo("PED-13");
        original.setDireccionEntrega("Dir");
        original.setFechaCreacion(LocalDateTime.now());

        Producto prod = new Producto();
        prod.setNombre("Pizza");
        prod.setPrecio(new BigDecimal("10.00"));

        LineaPedido lp = new LineaPedido(original, prod, 2);
        lp.setCodigo("LP-1");
        lp.setPrecioUnitario(new BigDecimal("9.00"));

        Ingrediente ing = new Ingrediente();
        ing.setNombre("Queso");
        ing.setPrecioExtra(new BigDecimal("1.50"));

        LineaPedidoIngrediente li = new LineaPedidoIngrediente(
                lp, ing, true, 1, new BigDecimal("1.50")
        );
        lp.setIngredientes(Set.of(li));

        original.getLineaPedidos().add(lp);

        Pedido copia = service.crearBorradorPedidoParaEdicion(original);

        assertThat(copia).isNotNull();
        assertThat(copia.getCodigo()).isEqualTo("PED-13");
        assertThat(copia.getDireccionEntrega()).isEqualTo("Dir");
        assertThat(copia.getFechaCreacion()).isEqualTo(original.getFechaCreacion());

        assertThat(copia.getLineaPedidos()).hasSize(1);

        LineaPedido lp2 = copia.getLineaPedidos().iterator().next();
        assertThat(lp2).isNotSameAs(lp);
        assertThat(lp2.getCodigo()).isEqualTo("LP-1");
        assertThat(lp2.getCantidad()).isEqualTo(2);
        assertThat(lp2.getProducto()).isSameAs(prod);
        assertThat(lp2.getPrecioUnitario()).isEqualByComparingTo(new BigDecimal("9.00"));

        assertThat(lp2.getIngredientes()).hasSize(1);
        LineaPedidoIngrediente li2 = lp2.getIngredientes().iterator().next();
        assertThat(li2).isNotSameAs(li);
        assertThat(li2.getIngrediente()).isSameAs(ing);
        assertThat(li2.isIncluido()).isTrue();
        assertThat(li2.getExtraCantidad()).isEqualTo(1);
        assertThat(li2.getPrecioExtra()).isEqualByComparingTo(new BigDecimal("1.50"));

        verifyNoInteractions(ajusteRepo);
    }

    private static Pedido pedidoConCodigo(String codigo) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        return p;
    }

    private static Pago pagoConfirmado(MetodoPago metodo) {
        Pedido p = new Pedido();
        p.setCodigo("X");
        Pago pago = new Pago(p, metodo, new BigDecimal("10.00"));
        pago.confirmar("REF");
        return pago;
    }

    private static Pago pagoNoConfirmado(MetodoPago metodo) {
        Pedido p = new Pedido();
        p.setCodigo("X");
        Pago pago = new Pago(p, metodo, new BigDecimal("10.00"));
        pago.fallar("Fallo");
        return pago;
    }

    private static AjustePago ajustePagoPendiente(Pedido pedido, String codigo, TipoAjustePago tipo, BigDecimal importe) {
        AjustePago a = new AjustePago(pedido, codigo, tipo, importe);
        a.setEstado(EstadoAjustePago.PENDIENTE);
        return a;
    }
}