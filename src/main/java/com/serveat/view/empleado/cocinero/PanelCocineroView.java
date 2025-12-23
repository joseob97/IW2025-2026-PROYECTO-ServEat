package com.serveat.view.empleado.cocinero;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/cocinero", layout = MainLayout.class)
@PageTitle("Panel Cocina")
@Secured("ROLE_COCINERO")
public class PanelCocineroView extends VerticalLayout {

    public PanelCocineroView() {

        setPadding(true);
        setSpacing(false);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Cocina");
        titulo.getStyle().set("margin", "0");

        Span subtitulo = new Span("Gestión de pedidos entrantes y en preparación");
        subtitulo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(titulo, subtitulo);


        HorizontalLayout cardsLayout = new HorizontalLayout(
                crearCard(
                        "🧾 Pedidos Entrantes",
                        "Pedidos esperando ser aceptados por cocina",
                        PedidosPendientesCocinaView.class
                ),
                crearCard(
                        "🔄 Modificar estado de un pedido",
                        "Gestiona y actualiza el estado de los pedidos en curso",
                        GestionPedidoCocineroView.class // Usamos esta vista como placeholder para la gestión de estados
                )
        );
        cardsLayout.setWidthFull();
        cardsLayout.getStyle().set("gap", "14px");

        add(cardsLayout);
    }

    private Component crearCard(String titulo, String descripcion,
                                Class<? extends Component> destino) {

        RouterLink link = new RouterLink("", destino);
        link.getStyle().set("text-decoration", "none");
        link.getStyle().set("width", "100%");

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
        return link;
    }
}
