package com.serveat.view.cliente.pedido;

import com.serveat.view.layout.MainLayout;
import com.serveat.view.publico.carta.CartaView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H3;

@PageTitle("Panel Pedidos | Cliente")
@Route(value = "cliente/pedido", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class PanelPedidoClienteView extends VerticalLayout {

    public PanelPedidoClienteView() {
        setPadding(true);
        setSpacing(false);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Pedidos");
        titulo.getStyle().set("margin", "0");

        Span subtitulo = new Span("Elige qué quieres hacer.");
        subtitulo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(titulo, subtitulo);

        // FILA 1
        HorizontalLayout fila1 = new HorizontalLayout(
                crearCardLink(
                        "🛒 Nuevo pedido",
                        "Elige si quieres recogerlo o recibirlo en casa.",
                        ElegirTipoPedidoView.class
                ),
                crearCardLink(
                        "🍽️ Pedido en mesa",
                        "Crear pedido asociado a mesa.",
                        CrearPedidoMesaView.class
                )
        );

        // FILA 2
        HorizontalLayout fila2 = new HorizontalLayout(
                crearCardLink(
                        "📦 Mis pedidos",
                        "Ver pedidos y estado de cocina / reparto.",
                        ConsultaPedidosView.class
                )
        );

        // FILA 3
        HorizontalLayout fila3 = new HorizontalLayout(
                crearCardLink(
                        "📖 Ver carta",
                        "Consultar productos y precios sin iniciar un pedido.",
                        CartaView.class
                )
        );

        add(fila1, fila2, fila3);
    }

    private VerticalLayout crearCardLink(String titulo, String descripcion, Class<? extends Component> destino) {

        RouterLink link = new RouterLink("", destino);
        link.getStyle().set("text-decoration", "none");
        link.getStyle().set("display", "block");

        H3 h3 = new H3(titulo);
        h3.getStyle().set("margin", "0");

        Span desc = new Span(descripcion);
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout card = new VerticalLayout(h3, desc);
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle().set("gap", "8px");

        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");
        card.getStyle().set("cursor", "pointer");

        link.add(card);

        VerticalLayout wrapper = new VerticalLayout(link);
        wrapper.setPadding(false);
        wrapper.setSpacing(false);
        wrapper.setWidthFull();

        return wrapper;
    }
}