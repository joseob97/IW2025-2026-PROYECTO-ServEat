package com.serveat.view.cliente.pedido;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.compartida.pedido.CartaCarritoBaseView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class CrearPedidoCartaBaseView extends CartaCarritoBaseView {

    protected final transient PedidoService pedidoService;
    protected final transient FeatureService featureService;

    protected final Button continuar = new Button("Continuar");

    private boolean showIngredientes = false;

    protected CrearPedidoCartaBaseView(PedidoService pedidoService,
                                       PedidoCarritoService pedidoCarritoService,
                                       PedidoCalculoService pedidoCalculoService,
                                       ProductoService productoService,
                                       CategoriaService categoriaService,
                                       FeatureService featureService) {
        super(pedidoCarritoService, pedidoCalculoService, productoService, categoriaService);
        this.pedidoService = pedidoService;
        this.featureService = featureService;
    }

    protected final void construirUI(String tituloPantalla) {
        showIngredientes = featureService.tieneFeature(Feature.INGREDIENTES);

        H3 titulo = new H3(tituloPantalla);
        titulo.getStyle().set("margin", "0");

        VerticalLayout cardDetalles = crearCard();
        cardDetalles.getStyle().set("gap", "10px");
        H3 hDetalles = new H3("Detalles");
        hDetalles.getStyle().set("margin", "0");
        cardDetalles.add(hDetalles, construirBloqueDetalles());

        continuar.setWidthFull();
        continuar.getStyle().set("font-weight", "700");
        continuar.addClickListener(e -> onContinuar());
        cardDetalles.add(continuar);

        Component main = construirCartaYCarrito(cardDetalles);

        add(titulo, main);

        cargarProductos();
        refrescarCarrito();
    }

    @Override
    protected final boolean puedeInteractuarConCarta() {
        return true;
    }

    @Override
    protected final boolean personalizacionHabilitada() {
        return showIngredientes;
    }

    @Override
    protected final void onCarritoActualizado() {
        continuar.setEnabled(puedeContinuar());
    }

    protected final String usernameActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    protected abstract Component construirBloqueDetalles();

    protected abstract boolean puedeContinuar();

    protected abstract void onContinuar();
}