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

        add(crearCardGrande(
                "📅 Pedidos de hoy",
                "Gestiona y actualiza el estado de los pedidos del día",
                PedidosCocinaHoyView.class
        ));

        HorizontalLayout filaSecundaria = new HorizontalLayout(
                crearCardPequena(
                        "🧾 Entrantes",
                        "Pedidos esperando aceptación",
                        PedidosPendientesCocinaView.class
                ),
                crearCardPequena(
                        "🗂 Histórico",
                        "Buscar pedidos antiguos y ver detalle",
                        PedidosCocinaHistoricoView.class
                )
        );
        filaSecundaria.setWidthFull();
        filaSecundaria.setSpacing(false);
        filaSecundaria.getStyle().set("gap", "14px");

        add(filaSecundaria);
    }

    private Component crearCardGrande(String titulo, String descripcion, Class<? extends Component> destino) {
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

        card.getStyle().set("background", "var(--lumo-primary-color-10pct)");
        card.getStyle().set("border", "1px solid var(--lumo-primary-color-50pct)");
        card.getStyle().set("border-radius", "16px");
        card.getStyle().set("box-shadow", "0 10px 26px rgba(0,0,0,0.08)");
        card.getStyle().set("cursor", "pointer");

        link.add(card);
        return link;
    }

    private Component crearCardPequena(String titulo, String descripcion, Class<? extends Component> destino) {
        RouterLink link = new RouterLink("", destino);
        link.getStyle().set("text-decoration", "none");
        link.getStyle().set("width", "100%");

        Span t = new Span(titulo);
        t.getStyle().set("font-weight", "800");

        Span desc = new Span(descripcion);
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");
        desc.getStyle().set("font-size", "var(--lumo-font-size-s)");

        VerticalLayout card = new VerticalLayout(t, desc);
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle().set("gap", "6px");

        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");
        card.getStyle().set("cursor", "pointer");

        link.add(card);
        return link;
    }
}