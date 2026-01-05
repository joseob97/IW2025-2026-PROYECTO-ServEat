package com.serveat.view.empleado.camarero;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.compartida.pedido.CartaCarritoBaseView;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@PageTitle("Iniciar Pedido | Camarero")
@Route(value = "empleado/camarero/pedidos/nuevo", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class IniciarPedidoView extends CartaCarritoBaseView {

    private final transient PedidoService pedidoService;
    private final transient FeatureService featureService;

    private transient Pedido pedidoActual;

    private final IntegerField mesa = new IntegerField("Número de mesa");
    private final TextField codigo = new TextField("Código pedido");
    private final Button crearPedido = new Button("Crear pedido");

    private final Button confirmar = new Button("Confirmar pedido (Enviar a cocina)");

    public IniciarPedidoView(PedidoService pedidoService,
                             PedidoCarritoService carritoService,
                             PedidoCalculoService calculoService,
                             ProductoService productoService,
                             CategoriaService categoriaService,
                             FeatureService featureService) {
        super(carritoService, calculoService, productoService, categoriaService);
        this.pedidoService = pedidoService;
        this.featureService = featureService;

        setSpacing(false);
        setPadding(true);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1280px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Iniciar pedido de mesa");
        titulo.getStyle().set("margin", "0");

        add(titulo);
        add(crearBloqueMesa());

        Component cardConfirmar = crearBloqueConfirmar();
        add(construirCartaYCarrito(cardConfirmar));

        cargarProductos();
        refrescarCarrito();

        setUiPedidoCreado(false);
    }

    @Override
    protected boolean puedeInteractuarConCarta() {
        return hayPedidoCreado();
    }

    @Override
    protected boolean personalizacionHabilitada() {
        return featureService.tieneFeature(Feature.INGREDIENTES);
    }

    @Override
    protected void onCarritoActualizado() {
        confirmar.setEnabled(hayPedidoCreado()
                && carrito != null
                && carrito.getLineaPedidos() != null
                && !carrito.getLineaPedidos().isEmpty());
    }

    private Component crearBloqueMesa() {
        VerticalLayout card = crearCard();
        card.getStyle().set("gap", "14px");

        mesa.setMin(1);
        mesa.setStepButtonsVisible(true);
        mesa.setWidth("260px");

        crearPedido.setWidth("260px");
        crearPedido.getStyle().set("font-weight", "700");
        crearPedido.addClickListener(e -> crearPedidoMesa());

        codigo.setReadOnly(true);
        codigo.setWidth("320px");

        VerticalLayout bloqueMesa = new VerticalLayout(mesa, crearPedido);
        bloqueMesa.setPadding(false);
        bloqueMesa.setSpacing(false);
        bloqueMesa.getStyle().set("gap", "10px");
        bloqueMesa.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout fila = new HorizontalLayout(bloqueMesa, codigo);
        fila.setWidthFull();
        fila.setSpacing(true);
        fila.getStyle().set("gap", "18px");
        fila.setAlignItems(FlexComponent.Alignment.END);

        card.add(fila);
        return card;
    }

    private Component crearBloqueConfirmar() {
        VerticalLayout cardConfirmar = crearCard();
        cardConfirmar.getStyle().set("gap", "10px");

        confirmar.setWidthFull();
        confirmar.getStyle().set("font-weight", "700");
        confirmar.addClickListener(e -> confirmarPedido());

        cardConfirmar.add(new H3("Acción"), confirmar);
        return cardConfirmar;
    }

    private void crearPedidoMesa() {
        Integer nMesa = mesa.getValue();
        if (nMesa == null || nMesa <= 0) {
            Notification.show("Mesa inválida", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoActual = pedidoService.crearPedidoMesa(nMesa);
            codigo.setValue(pedidoActual.getCodigo());

            setUiPedidoCreado(true);

            refrescarCarrito();
            Notification.show("Pedido creado: " + pedidoActual.getCodigo(), 3000, Notification.Position.MIDDLE);
        } catch (Exception ex) {
            Notification.show("Error creando pedido: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarPedido() {
        if (!hayPedidoCreado()) return;

        if (carrito == null || carrito.getLineaPedidos() == null || carrito.getLineaPedidos().isEmpty()) {
            Notification.show("El pedido no puede estar vacío", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoService.volcarCarritoEnPedido(pedidoActual.getCodigo(), carrito);
            pedidoService.confirmarPedido(pedidoActual.getCodigo());

            Notification.show("Pedido enviado a cocina", 3000, Notification.Position.MIDDLE);
            setUiPedidoConfirmado();
        } catch (Exception ex) {
            Notification.show("Error confirmando: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private boolean hayPedidoCreado() {
        return pedidoActual != null;
    }

    private void setUiPedidoCreado(boolean creado) {
        setCartaEnabled(creado);

        confirmar.setEnabled(creado
                && carrito != null
                && carrito.getLineaPedidos() != null
                && !carrito.getLineaPedidos().isEmpty());
    }

    private void setUiPedidoConfirmado() {
        setCartaEnabled(false);

        confirmar.setEnabled(false);
        crearPedido.setEnabled(false);
        mesa.setEnabled(false);

        cargarProductos();
    }
}