package com.serveat.view.cliente.pedido;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.cliente.menu.MenusClienteView;
import com.serveat.view.compartida.panel.PanelBaseView;
import com.serveat.view.layout.MainLayout;
import com.serveat.view.publico.carta.CartaView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@PageTitle("Panel Pedidos | Cliente")
@Route(value = "cliente/pedido", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class PanelPedidoClienteView extends PanelBaseView {

    public PanelPedidoClienteView(FeatureService featureService) {
        H2 titulo = new H2("Pedidos");
        titulo.getStyle().set("margin", "0");

        Span subtitulo = new Span("Elige qué quieres hacer.");
        subtitulo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(titulo, subtitulo);

        HorizontalLayout fila1 = fila(
                Cards.cardLink(
                        "🛒 Nuevo pedido",
                        "Elige si quieres recogerlo o recibirlo en casa.",
                        ElegirTipoPedidoView.class
                ),
                Cards.cardLink(
                        "🍽️ Pedido en mesa",
                        "Crear pedido asociado a mesa.",
                        CrearPedidoMesaCartaView.class
                )
        );

        HorizontalLayout fila2 = fila(
                Cards.cardLink(
                        "📦 Mis pedidos",
                        "Ver pedidos y estado de cocina / reparto.",
                        ConsultaPedidosView.class
                ),
                Cards.cardLink(
                        "📍 Seguimiento",
                        "Seguimiento en tiempo real de cocina y reparto.",
                        SeguimientoPedidosActivosView.class
                )
        );

        HorizontalLayout fila3;
        if (featureService.tieneFeature(Feature.MENUS_OFERTAS)) {
            fila3 = fila(
                    Cards.cardLink(
                            "📖 Ver carta",
                            "Consultar productos y precios sin iniciar un pedido.",
                            CartaView.class
                    ),
                    Cards.cardLink(
                            "🍱 Menús y ofertas",
                            "Consulta menús y combinaciones a precio especial.",
                            MenusClienteView.class
                    )
            );
        } else {
            fila3 = fila(
                    Cards.cardLink(
                            "📖 Ver carta",
                            "Consultar productos y precios sin iniciar un pedido.",
                            CartaView.class
                    )
            );
        }

        add(fila1, fila2, fila3);
    }
}