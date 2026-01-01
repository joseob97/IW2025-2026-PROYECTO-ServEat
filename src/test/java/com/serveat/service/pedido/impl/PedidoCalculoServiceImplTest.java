package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PedidoCalculoServiceImplTest {

    private PedidoCalculoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PedidoCalculoServiceImpl();
    }

    @Test
    void calcularPrecioLinea_lineaNull_devuelveCero() {
        assertEquals(0, service.calcularPrecioLinea(null).compareTo(BigDecimal.ZERO));
    }

    @Test
    void calcularPrecioLinea_productoNull_devuelveCero() {
        Pedido pedido = new Pedido();
        LineaPedido lp = new LineaPedido(pedido, null, 2);

        assertEquals(0, service.calcularPrecioLinea(lp).compareTo(BigDecimal.ZERO));
    }

    @Test
    void calcularPrecioLinea_precioProductoNull_devuelveCero() {
        Pedido pedido = new Pedido();

        Producto producto = new Producto();
        producto.setPrecio(null);

        LineaPedido lp = new LineaPedido(pedido, producto, 2);

        assertEquals(0, service.calcularPrecioLinea(lp).compareTo(BigDecimal.ZERO));
    }

    @Test
    void calcularPrecioLinea_cantidadNegativa_seTrataComoCero() {
        Pedido pedido = new Pedido();

        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("10.00"));

        LineaPedido lp = new LineaPedido(pedido, producto, -5);

        assertEquals(0, service.calcularPrecioLinea(lp).compareTo(BigDecimal.ZERO));
    }

    @Test
    void calcularPrecioLinea_sinIngredientes_basePorCantidad() {
        Pedido pedido = new Pedido();

        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("3.50"));

        LineaPedido lp = new LineaPedido(pedido, producto, 4);

        assertEquals(0, service.calcularPrecioLinea(lp).compareTo(new BigDecimal("14.00")));
    }

    @Test
    void calcularPrecioLinea_extrasNoIncluidos_noSuman() {
        Pedido pedido = new Pedido();

        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("5.00"));

        LineaPedido lp = new LineaPedido(pedido, producto, 2);

        lp.getIngredientes().add(extra(lp, UUID.randomUUID(), false, 3, new BigDecimal("0.50")));

        assertEquals(0, service.calcularPrecioLinea(lp).compareTo(new BigDecimal("10.00")));
    }

    @Test
    void calcularPrecioLinea_extraCantidadCeroONegativa_noSuma() {
        Pedido pedido = new Pedido();

        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("5.00"));

        LineaPedido lp = new LineaPedido(pedido, producto, 2);

        lp.getIngredientes().add(extra(lp, UUID.randomUUID(), true, 0, new BigDecimal("0.50")));
        lp.getIngredientes().add(extra(lp, UUID.randomUUID(), true, -2, new BigDecimal("0.50")));

        assertEquals(0, service.calcularPrecioLinea(lp).compareTo(new BigDecimal("10.00")));
    }

    @Test
    void calcularPrecioLinea_precioExtraNull_seTrataComoCero() {
        Pedido pedido = new Pedido();

        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("5.00"));

        LineaPedido lp = new LineaPedido(pedido, producto, 2);

        lp.getIngredientes().add(extra(lp, UUID.randomUUID(), true, 3, null));

        assertEquals(0, service.calcularPrecioLinea(lp).compareTo(new BigDecimal("10.00")));
    }

    @Test
    void calcularPrecioLinea_extrasSeMultiplicanPorCantidad() {
        Pedido pedido = new Pedido();

        Producto producto = new Producto();
        producto.setPrecio(new BigDecimal("5.00"));

        LineaPedido lp = new LineaPedido(pedido, producto, 2);

        lp.getIngredientes().add(extra(lp, UUID.randomUUID(), true, 3, new BigDecimal("0.50")));
        lp.getIngredientes().add(extra(lp, UUID.randomUUID(), true, 1, new BigDecimal("1.00")));

        assertEquals(0, service.calcularPrecioLinea(lp).compareTo(new BigDecimal("15.00")));
    }

    @Test
    void calcularTotalPedido_pedidoNull_devuelveCero() {
        assertEquals(0, service.calcularTotalPedido(null).compareTo(BigDecimal.ZERO));
    }

    @Test
    void calcularTotalPedido_lineasNull_devuelveCero() {
        Pedido pedido = new Pedido();
        pedido.setLineaPedidos(null);

        assertEquals(0, service.calcularTotalPedido(pedido).compareTo(BigDecimal.ZERO));
    }

    @Test
    void calcularTotalPedido_sumaLineas() {
        Pedido pedido = new Pedido();
        pedido.setLineaPedidos(new LinkedHashSet<>());

        Producto a = new Producto();
        a.setPrecio(new BigDecimal("2.00"));
        LineaPedido l1 = new LineaPedido(pedido, a, 3); // 6.00

        Producto b = new Producto();
        b.setPrecio(new BigDecimal("10.00"));
        LineaPedido l2 = new LineaPedido(pedido, b, 1); // 10.00

        pedido.getLineaPedidos().add(l1);
        pedido.getLineaPedidos().add(l2);

        assertEquals(0, service.calcularTotalPedido(pedido).compareTo(new BigDecimal("16.00")));
    }

    // Helpers

    private static LineaPedidoIngrediente extra(LineaPedido linea,
                                                UUID ingredienteId,
                                                boolean incluido,
                                                int extraCant,
                                                BigDecimal precioExtra) {
        Ingrediente ing = new Ingrediente();
        ing.setNombre("ING");
        setPrivateField(ing, "id", ingredienteId);
        return new LineaPedidoIngrediente(linea, ing, incluido, extraCant, precioExtra);
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