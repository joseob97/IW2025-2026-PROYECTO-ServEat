package com.serveat.view.empleado.administrador.estadisticas;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.caja.CierreCajaService;
import com.serveat.service.caja.EstadoCajaService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CierreCajaViewTest {

    private EstadisticasService estadisticasService;
    private CierreCajaService cierreCajaService;
    private EstadoCajaService estadoCajaService;
    private FeatureService featureService;

    @BeforeEach
    void setUp() {
        estadisticasService = mock(EstadisticasService.class);
        cierreCajaService = mock(CierreCajaService.class);
        estadoCajaService = mock(EstadoCajaService.class);
        featureService = mock(FeatureService.class);

        SecurityContextHolder.clearContext();
    }

    @Test
    void constructor_caja_abierta_muestra_cerrar_y_oculta_abrir() throws Exception {
        when(estadoCajaService.isCajaAbierta()).thenReturn(true);
        when(featureService.tieneFeature(any())).thenReturn(false);

        CierreCajaView view = new CierreCajaView(
                estadisticasService,
                cierreCajaService,
                estadoCajaService,
                featureService
        );

        Button cerrar = getField(view, "cerrarCajaButton", Button.class);
        Button abrir = getField(view, "abrirCajaButton", Button.class);

        assertTrue(cerrar.isVisible());
        assertFalse(abrir.isVisible());
    }

    @Test
    void constructor_caja_cerrada_muestra_abrir_y_oculta_cerrar() throws Exception {
        when(estadoCajaService.isCajaAbierta()).thenReturn(false);
        when(featureService.tieneFeature(any())).thenReturn(false);

        CierreCajaView view = new CierreCajaView(
                estadisticasService,
                cierreCajaService,
                estadoCajaService,
                featureService
        );

        Button cerrar = getField(view, "cerrarCajaButton", Button.class);
        Button abrir = getField(view, "abrirCajaButton", Button.class);

        assertFalse(cerrar.isVisible());
        assertTrue(abrir.isVisible());
    }

    @Test
    void realizar_apertura_caja_llama_servicio() throws Exception {
        when(estadoCajaService.isCajaAbierta()).thenReturn(false);
        when(featureService.tieneFeature(any())).thenReturn(false);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pwd")
        );

        CierreCajaView view = new CierreCajaView(
                estadisticasService,
                cierreCajaService,
                estadoCajaService,
                featureService
        );

        invokePrivate(view, "realizarAperturaCaja");

        verify(estadoCajaService).abrirCaja("admin");
    }

    @Test
    void realizar_cierre_caja_llama_servicios() throws Exception {
        when(estadoCajaService.isCajaAbierta()).thenReturn(true);
        when(featureService.tieneFeature(any())).thenReturn(false);

        Map<String, Object> datos = new HashMap<>();
        datos.put("total", new BigDecimal("100.00"));
        datos.put("paypal", new BigDecimal("10.00"));
        datos.put("efectivo", new BigDecimal("40.00"));
        datos.put("tarjeta", new BigDecimal("50.00"));

        when(estadisticasService.generarCierreCajaTurno()).thenReturn(datos);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pwd")
        );

        CierreCajaView view = new CierreCajaView(
                estadisticasService,
                cierreCajaService,
                estadoCajaService,
                featureService
        );

        invokePrivate(view, "realizarCierreCaja");

        verify(estadisticasService).generarCierreCajaTurno();
        verify(cierreCajaService).cerrarCaja(
                eq(LocalDate.now()),
                eq(new BigDecimal("100.00")),
                eq(new BigDecimal("40.00")),
                eq(new BigDecimal("50.00")),
                eq(new BigDecimal("10.00"))
        );
        verify(estadoCajaService).cerrarCaja("admin");
    }

    @Test
    void mostrar_resultados_hace_visible_layout() throws Exception {
        when(estadoCajaService.isCajaAbierta()).thenReturn(true);
        when(featureService.tieneFeature(any())).thenReturn(false);

        CierreCajaView view = new CierreCajaView(
                estadisticasService,
                cierreCajaService,
                estadoCajaService,
                featureService
        );

        VerticalLayout resultados = getField(view, "resultadosLayout", VerticalLayout.class);
        assertFalse(resultados.isVisible());

        Map<String, Object> datos = new HashMap<>();
        datos.put("total", BigDecimal.ONE);
        datos.put("paypal", BigDecimal.ZERO);
        datos.put("efectivo", BigDecimal.ZERO);
        datos.put("tarjeta", BigDecimal.ZERO);

        invokePrivate(view, "mostrarResultados", Map.class, datos);

        assertFalse(resultados.getChildren().findAny().isEmpty());
    }

    private <T> T getField(Object target, String name, Class<T> type) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return type.cast(f.get(target));
    }

    private void invokePrivate(Object target, String name) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name);
        m.setAccessible(true);
        m.invoke(target);
    }

    private void invokePrivate(Object target, String name, Class<?> param, Object value) throws Exception {
        Method m = target.getClass().getDeclaredMethod(name, param);
        m.setAccessible(true);
        m.invoke(target, value);
    }
}