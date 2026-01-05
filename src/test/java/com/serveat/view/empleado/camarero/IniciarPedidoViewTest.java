package com.serveat.view.empleado.camarero;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IniciarPedidoViewTest {

    @BeforeEach
    void setupUi() {
        UI ui = new UI();
        UI.setCurrent(ui);
    }

    @Test
    void constructor_no_revienta_y_estado_inicial_sin_pedido() {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCarritoService carritoService = mock(PedidoCarritoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(any())).thenReturn(false);

        IniciarPedidoView view = new IniciarPedidoView(
                pedidoService,
                carritoService,
                calculoService,
                productoService,
                categoriaService,
                featureService
        );
        UI.getCurrent().add(view);

        assertNotNull(view);

        assertNotNull(findH3ByText(view, "Iniciar pedido de mesa"));

        IntegerField mesa = findIntegerFieldByLabel(view, "Número de mesa");
        TextField codigo = findTextFieldByLabel(view, "Código pedido");
        Button crearPedido = findButtonByText(view, "Crear pedido");
        Button confirmar = findButtonByText(view, "Confirmar pedido (Enviar a cocina)");

        assertNotNull(mesa);
        assertNotNull(codigo);
        assertNotNull(crearPedido);
        assertNotNull(confirmar);

        assertTrue(codigo.isReadOnly());
        assertFalse(confirmar.isEnabled());
    }

    @Test
    void personalizacion_habilitada_consulta_feature_ingredientes() throws Exception {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCarritoService carritoService = mock(PedidoCarritoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(any())).thenReturn(false);
        when(featureService.tieneFeature(Feature.INGREDIENTES)).thenReturn(true);

        IniciarPedidoView view = new IniciarPedidoView(
                pedidoService,
                carritoService,
                calculoService,
                productoService,
                categoriaService,
                featureService
        );
        UI.getCurrent().add(view);

        boolean out = (boolean) invokePrivateAndReturn(view, "personalizacionHabilitada");

        assertTrue(out);
        verify(featureService, atLeastOnce()).tieneFeature(Feature.INGREDIENTES);
    }

    @Test
    void crear_pedido_mesa_con_valor_invalido_no_llama_servicio() throws Exception {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCarritoService carritoService = mock(PedidoCarritoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(any())).thenReturn(false);

        IniciarPedidoView view = new IniciarPedidoView(
                pedidoService,
                carritoService,
                calculoService,
                productoService,
                categoriaService,
                featureService
        );
        UI.getCurrent().add(view);

        IntegerField mesa = findIntegerFieldByLabel(view, "Número de mesa");
        assertNotNull(mesa);
        mesa.setValue(0);

        invokePrivate(view, "crearPedidoMesa");

        verify(pedidoService, never()).crearPedidoMesa(anyInt());
    }

    @Test
    void set_ui_pedido_creado_false_deja_confirmar_deshabilitado() throws Exception {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCarritoService carritoService = mock(PedidoCarritoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(any())).thenReturn(false);

        IniciarPedidoView view = new IniciarPedidoView(
                pedidoService,
                carritoService,
                calculoService,
                productoService,
                categoriaService,
                featureService
        );
        UI.getCurrent().add(view);

        invokePrivate(view, "setUiPedidoCreado",
                new Class<?>[]{boolean.class},
                new Object[]{false}
        );

        Button confirmar = findButtonByText(view, "Confirmar pedido (Enviar a cocina)");
        assertNotNull(confirmar);
        assertFalse(confirmar.isEnabled());
    }

    @Test
    void set_ui_pedido_confirmado_deshabilita_campos() throws Exception {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCarritoService carritoService = mock(PedidoCarritoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(any())).thenReturn(false);

        IniciarPedidoView view = new IniciarPedidoView(
                pedidoService,
                carritoService,
                calculoService,
                productoService,
                categoriaService,
                featureService
        );
        UI.getCurrent().add(view);

        invokePrivate(view, "setUiPedidoConfirmado");

        Button confirmar = findButtonByText(view, "Confirmar pedido (Enviar a cocina)");
        Button crearPedido = findButtonByText(view, "Crear pedido");
        IntegerField mesa = findIntegerFieldByLabel(view, "Número de mesa");

        assertNotNull(confirmar);
        assertNotNull(crearPedido);
        assertNotNull(mesa);

        assertFalse(confirmar.isEnabled());
        assertFalse(crearPedido.isEnabled());
        assertFalse(mesa.isEnabled());
    }

    @Test
    void crear_pedido_mesa_valida_llama_servicio_y_rellena_codigo() throws Exception {
        PedidoService pedidoService = mock(PedidoService.class);
        PedidoCarritoService carritoService = mock(PedidoCarritoService.class);
        PedidoCalculoService calculoService = mock(PedidoCalculoService.class);
        ProductoService productoService = mock(ProductoService.class);
        CategoriaService categoriaService = mock(CategoriaService.class);
        FeatureService featureService = mock(FeatureService.class);

        when(featureService.tieneFeature(any())).thenReturn(false);

        Pedido pedido = mock(Pedido.class);
        when(pedido.getCodigo()).thenReturn("P-123");
        when(pedidoService.crearPedidoMesa(7)).thenReturn(pedido);

        IniciarPedidoView view = new IniciarPedidoView(
                pedidoService,
                carritoService,
                calculoService,
                productoService,
                categoriaService,
                featureService
        );
        UI.getCurrent().add(view);

        IntegerField mesa = findIntegerFieldByLabel(view, "Número de mesa");
        TextField codigo = findTextFieldByLabel(view, "Código pedido");

        assertNotNull(mesa);
        assertNotNull(codigo);

        mesa.setValue(7);

        invokePrivate(view, "crearPedidoMesa");

        verify(pedidoService, atLeastOnce()).crearPedidoMesa(7);
        assertEquals("P-123", codigo.getValue());
    }

    private static H3 findH3ByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof H3 h3 && text.equals(h3.getText())) {
                return h3;
            }
        }
        return null;
    }

    private static Button findButtonByText(Component root, String text) {
        for (Component c : flatten(root)) {
            if (c instanceof Button b && text.equals(b.getText())) {
                return b;
            }
        }
        return null;
    }

    private static IntegerField findIntegerFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof IntegerField f && label.equals(f.getLabel())) {
                return f;
            }
        }
        return null;
    }

    private static TextField findTextFieldByLabel(Component root, String label) {
        for (Component c : flatten(root)) {
            if (c instanceof TextField tf && label.equals(tf.getLabel())) {
                return tf;
            }
        }
        return null;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        if (c == null) return out;
        out.add(c);
        c.getChildren().forEach(child -> out.addAll(flatten(child)));
        return out;
    }

    private static void invokePrivate(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        m.invoke(target);
    }

    private static void invokePrivate(Object target, String methodName, Class<?>[] argTypes, Object[] args) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName, argTypes);
        m.setAccessible(true);
        m.invoke(target, args);
    }

    private static Object invokePrivateAndReturn(Object target, String methodName) throws Exception {
        var m = target.getClass().getDeclaredMethod(methodName);
        m.setAccessible(true);
        return m.invoke(target);
    }
}