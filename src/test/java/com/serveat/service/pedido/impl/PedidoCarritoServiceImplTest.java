package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoCarritoServiceImplTest {

    @Mock
    private ProductoRepository productoRepo;

    @Mock
    private PedidoRepository pedidoRepo;

    @InjectMocks
    private PedidoCarritoServiceImpl service;

    private Pedido carrito;

    @BeforeEach
    void setUp() {
        carrito = new Pedido();
        carrito.setLineaPedidos(new LinkedHashSet<>());
    }

    @Test
    void agregarProducto_carritoNull_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.agregarProducto(null, new Producto(), 1));
        assertEquals("Carrito inválido", ex.getMessage());
    }

    @Test
    void agregarProducto_productoNull_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.agregarProducto(carrito, null, 1));
        assertEquals("Producto inválido", ex.getMessage());
    }

    @Test
    void agregarProducto_cantidadInvalida_lanzaExcepcion() {
        Producto p = producto("P1");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.agregarProducto(carrito, p, 0));
        assertEquals("Cantidad inválida", ex.getMessage());
    }

    @Test
    void agregarProducto_inicializaLineaPedidosSiNull() {
        Pedido c = new Pedido();

        forceNullLineaPedidosIfPossible(c);

        Producto p = producto("P1");
        Pedido res = service.agregarProducto(c, p, 2);

        assertNotNull(res.getLineaPedidos());
        assertEquals(1, res.getLineaPedidos().size());
        LineaPedido lp = res.getLineaPedidos().iterator().next();
        assertEquals("P1", lp.getProducto().getCodigo());
        assertEquals(2, lp.getCantidad());
    }

    private static void forceNullLineaPedidosIfPossible(Pedido pedido) {
        try {
            Field f = Pedido.class.getDeclaredField("lineaPedidos");
            f.setAccessible(true);
            f.set(pedido, null);
        } catch (Exception ignored) {
        }
    }

    @Test
    void agregarProducto_agregaNuevaLinea() {
        Producto p = producto("P1");

        service.agregarProducto(carrito, p, 2);

        assertEquals(1, carrito.getLineaPedidos().size());
        LineaPedido lp = carrito.getLineaPedidos().iterator().next();
        assertEquals("P1", lp.getProducto().getCodigo());
        assertEquals(2, lp.getCantidad());
    }

    @Test
    void agregarProducto_mismaLineaSinIngredientes_sumaCantidad() {
        Producto p = producto("P1");

        service.agregarProducto(carrito, p, 2);
        service.agregarProducto(carrito, p, 3);

        assertEquals(1, carrito.getLineaPedidos().size());
        LineaPedido lp = carrito.getLineaPedidos().iterator().next();
        assertEquals(5, lp.getCantidad());
    }

    @Test
    void agregarProductoPersonalizado_productoNoEncontrado_lanzaExcepcion() {
        when(productoRepo.findWithIngredientesByCodigo("P1")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.agregarProductoPersonalizado(carrito, "P1", 1, null, null));

        assertEquals("Producto no encontrado", ex.getMessage());
        verify(productoRepo).findWithIngredientesByCodigo("P1");
    }

    @Test
    void agregarProductoPersonalizado_sinReceta_delegaEnAgregarNormal() {
        Producto p = producto("P1");
        p.setIngredientes(new ArrayList<>());

        when(productoRepo.findWithIngredientesByCodigo("P1")).thenReturn(Optional.of(p));

        service.agregarProductoPersonalizado(carrito, "P1", 2, null, null);

        assertEquals(1, carrito.getLineaPedidos().size());
        LineaPedido lp = carrito.getLineaPedidos().iterator().next();
        assertEquals("P1", lp.getProducto().getCodigo());
        assertEquals(2, lp.getCantidad());

        verify(productoRepo).findWithIngredientesByCodigo("P1");
        verifyNoMoreInteractions(productoRepo);
    }

    @Test
    void agregarProductoPersonalizado_conReceta_creaLineaConIngredientesSnapshot() {
        UUID ing1Id = UUID.randomUUID();
        UUID ing2Id = UUID.randomUUID();

        Ingrediente ing1 = ingredienteConId(ing1Id, "ING1");
        Ingrediente ing2 = ingredienteConId(ing2Id, "ING2");

        Producto p = producto("P1");
        ProductoIngrediente pi1 = new ProductoIngrediente(p, ing1, true, true, new BigDecimal("0.50"));   // opcional, por defecto incluido
        ProductoIngrediente pi2 = new ProductoIngrediente(p, ing2, false, false, new BigDecimal("1.00")); // NO opcional, por defecto NO incluido
        p.setIngredientes(List.of(pi1, pi2));

        when(productoRepo.findWithIngredientesByCodigo("P1")).thenReturn(Optional.of(p));

        Map<UUID, Boolean> incluido = new HashMap<>();
        incluido.put(ing1Id, false); // opcional -> permitido
        incluido.put(ing2Id, true);  // no opcional -> se ignora (queda por defecto)

        Map<UUID, Integer> extra = new HashMap<>();
        extra.put(ing1Id, 2); // permitido
        extra.put(ing2Id, 5); // no opcional -> se fuerza 0

        service.agregarProductoPersonalizado(carrito, "P1", 1, incluido, extra);

        assertEquals(1, carrito.getLineaPedidos().size());
        LineaPedido lp = carrito.getLineaPedidos().iterator().next();
        assertEquals("P1", lp.getProducto().getCodigo());
        assertEquals(1, lp.getCantidad());

        assertNotNull(lp.getIngredientes());
        assertEquals(2, lp.getIngredientes().size());

        Map<UUID, LineaPedidoIngrediente> byId = new HashMap<>();
        for (LineaPedidoIngrediente lpi : lp.getIngredientes()) {
            assertNotNull(lpi.getIngrediente());
            byId.put(lpi.getIngrediente().getId(), lpi);
        }

        LineaPedidoIngrediente sel1 = byId.get(ing1Id);
        assertNotNull(sel1);
        assertFalse(sel1.isIncluido());
        assertEquals(2, sel1.getExtraCantidad());
        assertEquals(new BigDecimal("0.50"), sel1.getPrecioExtra());

        LineaPedidoIngrediente sel2 = byId.get(ing2Id);
        assertNotNull(sel2);
        assertFalse(sel2.isIncluido());
        assertEquals(0, sel2.getExtraCantidad());
        assertEquals(new BigDecimal("1.00"), sel2.getPrecioExtra());
    }

    @Test
    void agregarProductoPersonalizado_mismaPersonalizacion_sumaCantidad() {
        UUID ingId = UUID.randomUUID();
        Ingrediente ing = ingredienteConId(ingId, "ING");

        Producto p = producto("P1");
        ProductoIngrediente pi = new ProductoIngrediente(p, ing, true, true, new BigDecimal("0.80"));
        p.setIngredientes(List.of(pi));

        when(productoRepo.findWithIngredientesByCodigo("P1")).thenReturn(Optional.of(p));

        Map<UUID, Boolean> incluido = Map.of(ingId, true);
        Map<UUID, Integer> extra = Map.of(ingId, 1);

        service.agregarProductoPersonalizado(carrito, "P1", 2, incluido, extra);
        service.agregarProductoPersonalizado(carrito, "P1", 3, incluido, extra);

        assertEquals(1, carrito.getLineaPedidos().size());
        LineaPedido lp = carrito.getLineaPedidos().iterator().next();
        assertEquals(5, lp.getCantidad());
        assertEquals(1, lp.getIngredientes().size());
    }

    @Test
    void actualizarCantidadLinea_siNuevaCantidadCero_eliminaLinea() {
        Producto p = producto("P1");
        LineaPedido lp = new LineaPedido(carrito, p, 2);
        lp.setCodigo("L1");
        carrito.getLineaPedidos().add(lp);

        service.actualizarCantidadLinea(carrito, "L1", 0);

        assertTrue(carrito.getLineaPedidos().isEmpty());
    }

    @Test
    void actualizarCantidadLinea_actualizaCantidad() {
        Producto p = producto("P1");
        LineaPedido lp = new LineaPedido(carrito, p, 2);
        lp.setCodigo("L1");
        carrito.getLineaPedidos().add(lp);

        service.actualizarCantidadLinea(carrito, "L1", 7);

        assertEquals(1, carrito.getLineaPedidos().size());
        assertEquals(7, carrito.getLineaPedidos().iterator().next().getCantidad());
    }

    @Test
    void actualizarCantidadLinea_lineaNoEncontrada_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.actualizarCantidadLinea(carrito, "NOEXISTE", 2));
        assertEquals("Línea no encontrada", ex.getMessage());
    }

    @Test
    void eliminarLinea_elimina() {
        Producto p = producto("P1");
        LineaPedido lp = new LineaPedido(carrito, p, 2);
        lp.setCodigo("L1");
        carrito.getLineaPedidos().add(lp);

        service.eliminarLinea(carrito, "L1");

        assertTrue(carrito.getLineaPedidos().isEmpty());
    }

    @Test
    void eliminarLinea_noExiste_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.eliminarLinea(carrito, "NOEXISTE"));
        assertEquals("Línea no encontrada", ex.getMessage());
    }

    @Test
    void volcarCarritoEnPedido_carritoVacio_lanzaExcepcion() {
        Pedido vacio = new Pedido();
        vacio.setLineaPedidos(new LinkedHashSet<>());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.volcarCarritoEnPedido("PED-1", vacio));

        assertEquals("El pedido no puede estar vacío", ex.getMessage());
        verifyNoInteractions(pedidoRepo);
        verifyNoInteractions(productoRepo);
    }

    @Test
    void volcarCarritoEnPedido_pedidoNoEncontrado_lanzaExcepcion() {
        Pedido c = new Pedido();
        c.setLineaPedidos(new LinkedHashSet<>());
        c.getLineaPedidos().add(new LineaPedido(c, producto("P1"), 1));

        when(pedidoRepo.findWithDetalleByCodigo("PED-1")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.volcarCarritoEnPedido("PED-1", c));

        assertEquals("Pedido no encontrado", ex.getMessage());
        verify(pedidoRepo).findWithDetalleByCodigo("PED-1");
        verifyNoMoreInteractions(pedidoRepo);
        verifyNoInteractions(productoRepo);
    }

    @Test
    void volcarCarritoEnPedido_productoNoEncontrado_lanzaExcepcion() {
        Pedido c = new Pedido();
        c.setLineaPedidos(new LinkedHashSet<>());
        c.getLineaPedidos().add(new LineaPedido(c, producto("P1"), 1));

        Pedido pedido = new Pedido();
        pedido.setLineaPedidos(new LinkedHashSet<>());

        when(pedidoRepo.findWithDetalleByCodigo("PED-1")).thenReturn(Optional.of(pedido));
        when(productoRepo.findByCodigo("P1")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.volcarCarritoEnPedido("PED-1", c));

        assertTrue(ex.getMessage().startsWith("Producto no encontrado: P1"));
        verify(pedidoRepo).findWithDetalleByCodigo("PED-1");
        verify(productoRepo).findByCodigo("P1");
        verify(pedidoRepo, never()).save(any());
    }

    @Test
    void volcarCarritoEnPedido_copiaLineas_yGuarda() {
        UUID ingId = UUID.randomUUID();
        Ingrediente ing = ingredienteConId(ingId, "ING");

        Pedido c = new Pedido();
        c.setLineaPedidos(new LinkedHashSet<>());

        Producto prodCarrito = producto("P1");
        LineaPedido lpCarrito = new LineaPedido(c, prodCarrito, 2);

        LineaPedidoIngrediente lpi = new LineaPedidoIngrediente(
                lpCarrito,
                ing,
                true,
                1,
                new BigDecimal("0.80")
        );
        lpCarrito.getIngredientes().add(lpi);

        c.getLineaPedidos().add(lpCarrito);

        Pedido pedido = new Pedido();
        pedido.setLineaPedidos(new LinkedHashSet<>());
        pedido.getLineaPedidos().add(new LineaPedido(pedido, producto("OLD"), 1));

        Producto prodBD = producto("P1");

        when(pedidoRepo.findWithDetalleByCodigo("PED-1")).thenReturn(Optional.of(pedido));
        when(productoRepo.findByCodigo("P1")).thenReturn(Optional.of(prodBD));

        service.volcarCarritoEnPedido("PED-1", c);

        assertEquals(1, pedido.getLineaPedidos().size());
        LineaPedido lp = pedido.getLineaPedidos().iterator().next();
        assertEquals("P1", lp.getProducto().getCodigo());
        assertEquals(2, lp.getCantidad());
        assertEquals(1, lp.getIngredientes().size());

        LineaPedidoIngrediente copied = lp.getIngredientes().iterator().next();
        assertEquals(ingId, copied.getIngrediente().getId());
        assertTrue(copied.isIncluido());
        assertEquals(1, copied.getExtraCantidad());
        assertEquals(new BigDecimal("0.80"), copied.getPrecioExtra());

        verify(pedidoRepo).save(pedido);
    }

    // Helpers

    private static Producto producto(String codigo) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        return p;
    }

    private static Ingrediente ingredienteConId(UUID id, String nombre) {
        Ingrediente i = new Ingrediente();
        i.setNombre(nombre);
        setPrivateField(i, "id", id);
        return i;
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