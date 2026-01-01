package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.seguridad.FeatureService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepo;

    @Mock
    private PedidoCalculoService calculoService;

    @Mock
    private FeatureService featureService;

    private TicketServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TicketServiceImpl(pedidoRepo, calculoService, featureService);
    }

    @Test
    void generarTicketCliente_featureDesactivada_lanzaIllegalState() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.generarTicketCliente("PED-1", "cliente1"));

        assertEquals("Funcionalidad de ticket no disponible", ex.getMessage());
        verifyNoInteractions(pedidoRepo);
        verifyNoInteractions(calculoService);
    }

    @Test
    void generarTicketCliente_codigoVacio_lanzaIllegalArgument() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generarTicketCliente("   ", "cliente1"));

        assertEquals("Código de pedido inválido", ex.getMessage());
        verifyNoInteractions(pedidoRepo);
        verifyNoInteractions(calculoService);
    }

    @Test
    void generarTicketCliente_usernameVacio_lanzaIllegalArgument() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generarTicketCliente("PED-1", " "));

        assertEquals("Usuario inválido", ex.getMessage());
        verifyNoInteractions(pedidoRepo);
        verifyNoInteractions(calculoService);
    }

    @Test
    void generarTicketCliente_pedidoNoEncontrado_lanzaIllegalArgument() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);
        when(pedidoRepo.findWithDetalleByCodigoAndCliente_Username("PED-1", "cliente1"))
                .thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generarTicketCliente("PED-1", "cliente1"));

        assertEquals("Pedido no encontrado o no pertenece al cliente", ex.getMessage());
        verify(pedidoRepo).findWithDetalleByCodigoAndCliente_Username("PED-1", "cliente1");
        verifyNoInteractions(calculoService);
    }

    @Test
    void generarTicketCamarero_codigoVacio_lanzaIllegalArgument() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generarTicketCamarero(""));

        assertEquals("Código de pedido inválido", ex.getMessage());
        verifyNoInteractions(pedidoRepo);
        verifyNoInteractions(calculoService);
    }

    @Test
    void generarTicketCamarero_pedidoNoEncontrado_lanzaIllegalArgumentConCodigo() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);
        when(pedidoRepo.findWithDetalleByCodigo("PED-X")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generarTicketCamarero("PED-X"));

        assertTrue(ex.getMessage().contains("Pedido no encontrado: PED-X"));
        verify(pedidoRepo).findWithDetalleByCodigo("PED-X");
        verifyNoInteractions(calculoService);
    }

    @Test
    void generarTicketRepartidor_pedidoNoEncontrado_lanzaIllegalArgumentConCodigo() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);
        when(pedidoRepo.findWithDetalleByCodigo("PED-X")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generarTicketRepartidor("PED-X"));

        assertTrue(ex.getMessage().contains("Pedido no encontrado: PED-X"));
        verify(pedidoRepo).findWithDetalleByCodigo("PED-X");
        verifyNoInteractions(calculoService);
    }

    @Test
    void generarTicketCliente_pedidoSinLineas_lanzaIllegalArgument() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);

        Pedido pedido = new Pedido();
        pedido.setCodigo("PED-1");
        pedido.setFechaCreacion(LocalDateTime.now());
        pedido.setLineaPedidos(new LinkedHashSet<>()); // vacío

        when(pedidoRepo.findWithDetalleByCodigoAndCliente_Username("PED-1", "cliente1"))
                .thenReturn(Optional.of(pedido));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.generarTicketCliente("PED-1", "cliente1"));

        assertEquals("El pedido no puede estar vacío", ex.getMessage());
        verifyNoInteractions(calculoService);
    }

    @Test
    void generarTicketCamarero_ok_devuelvePdfNoVacio_yLlamaCalculoServicePorLinea() throws IOException {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);

        Pedido pedido = pedidoConLineasDemo("PED-OK");
        when(pedidoRepo.findWithDetalleByCodigo("PED-OK")).thenReturn(Optional.of(pedido));

        // devolvemos un subtotal por línea
        when(calculoService.calcularPrecioLinea(any(LineaPedido.class)))
                .thenReturn(new BigDecimal("12.34"));

        byte[] pdf = service.generarTicketCamarero("PED-OK");

        assertNotNull(pdf);
        assertTrue(pdf.length > 200, "El PDF debería tener contenido");

        // Verifica que es un PDF válido
        try (PDDocument doc = PDDocument.load(pdf)) {
            assertEquals(1, doc.getNumberOfPages());
        }

        // Se llama una vez por cada línea (incluyendo aunque tenga ingredientes)
        verify(calculoService, times(pedido.getLineaPedidos().size())).calcularPrecioLinea(any(LineaPedido.class));
    }

    @Test
    void generarTicketCliente_ok_buscaPorCodigoYUsername() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);

        Pedido pedido = pedidoConLineasDemo("PED-CL");
        when(pedidoRepo.findWithDetalleByCodigoAndCliente_Username("PED-CL", "cliente1"))
                .thenReturn(Optional.of(pedido));

        when(calculoService.calcularPrecioLinea(any(LineaPedido.class)))
                .thenReturn(new BigDecimal("5.00"));

        byte[] pdf = service.generarTicketCliente("PED-CL", "cliente1");

        assertNotNull(pdf);
        assertTrue(pdf.length > 200);

        verify(pedidoRepo).findWithDetalleByCodigoAndCliente_Username("PED-CL", "cliente1");
        verify(pedidoRepo, never()).findWithDetalleByCodigo("PED-CL");
    }

    @Test
    void generarTicketRepartidor_ok_buscaPorCodigo() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);

        Pedido pedido = pedidoConLineasDemo("PED-RP");
        when(pedidoRepo.findWithDetalleByCodigo("PED-RP"))
                .thenReturn(Optional.of(pedido));

        when(calculoService.calcularPrecioLinea(any(LineaPedido.class)))
                .thenReturn(new BigDecimal("7.00"));

        byte[] pdf = service.generarTicketRepartidor("PED-RP");

        assertNotNull(pdf);
        assertTrue(pdf.length > 200);

        verify(pedidoRepo).findWithDetalleByCodigo("PED-RP");
    }

    @Test
    void generarTicketCamarero_ticketDemasiadoLargo_lanzaIllegalState() {
        when(featureService.tieneFeature(Feature.FACTURACION_TICKET)).thenReturn(true);

        Pedido pedido = pedidoLargoParaForzarOverflow("PED-LARGO");
        when(pedidoRepo.findWithDetalleByCodigo("PED-LARGO")).thenReturn(Optional.of(pedido));

        // No importa el subtotal para el overflow, pero debe devolver algo
        when(calculoService.calcularPrecioLinea(any(LineaPedido.class)))
                .thenReturn(new BigDecimal("1.00"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.generarTicketCamarero("PED-LARGO"));

        assertEquals("Ticket demasiado largo para una sola página", ex.getMessage());
    }

    // =======================
    // Helpers (sin setters de id -> reflexión)
    // =======================

    private static Pedido pedidoConLineasDemo(String codigo) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setFechaCreacion(LocalDateTime.now());
        p.setLineaPedidos(new LinkedHashSet<>());

        // Producto 1
        Producto prod1 = new Producto();
        prod1.setCodigo("PR-1");
        prod1.setNombre("Hamburguesa");
        prod1.setPrecio(new BigDecimal("10.00"));

        LineaPedido lp1 = new LineaPedido(p, prod1, 2);
        lp1.setCodigo("LP-AAA"); // para orden estable

        // Ingrediente: sin Queso
        Ingrediente queso = new Ingrediente();
        queso.setNombre("Queso");
        setPrivateField(queso, "id", UUID.randomUUID());

        lp1.getIngredientes().add(new LineaPedidoIngrediente(lp1, queso, false, 0, new BigDecimal("0.50")));

        // Ingrediente: extra Bacon x2 (+1.00)
        Ingrediente bacon = new Ingrediente();
        bacon.setNombre("Bacon");
        setPrivateField(bacon, "id", UUID.randomUUID());

        lp1.getIngredientes().add(new LineaPedidoIngrediente(lp1, bacon, true, 2, new BigDecimal("0.50")));

        // Producto 2
        Producto prod2 = new Producto();
        prod2.setCodigo("PR-2");
        prod2.setNombre("Coca-Cola");
        prod2.setPrecio(new BigDecimal("2.50"));

        LineaPedido lp2 = new LineaPedido(p, prod2, 1);
        lp2.setCodigo("LP-BBB");

        p.getLineaPedidos().add(lp1);
        p.getLineaPedidos().add(lp2);

        return p;
    }

    /**
     * Fuerza el "Ticket demasiado largo..." generando 1 línea con muchísimos ingredientes extra,
     * para que el cursor Y baje de 80.
     */
    private static Pedido pedidoLargoParaForzarOverflow(String codigo) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setFechaCreacion(LocalDateTime.now());
        p.setLineaPedidos(new LinkedHashSet<>());

        Producto prod = new Producto();
        prod.setCodigo("PR-LARGO");
        prod.setNombre("Producto con muchos ingredientes");
        prod.setPrecio(new BigDecimal("1.00"));

        LineaPedido lp = new LineaPedido(p, prod, 1);
        lp.setCodigo("LP-LARGO");

        // Muchísimos extras -> cada uno pinta una línea de texto y consume Y
        for (int i = 0; i < 80; i++) {
            Ingrediente ing = new Ingrediente();
            ing.setNombre("Ing " + i);
            setPrivateField(ing, "id", UUID.randomUUID());

            // extraCant=1 -> genera línea "- Extra ..."
            lp.getIngredientes().add(new LineaPedidoIngrediente(lp, ing, true, 1, new BigDecimal("0.10")));
        }

        p.getLineaPedidos().add(lp);
        return p;
    }

    private static void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo setear el campo '" + fieldName + "' por reflexión", e);
        }
    }
}