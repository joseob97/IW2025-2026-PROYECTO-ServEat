package com.serveat.view.empleado.cocinero;

import com.serveat.view.compartida.panel.PanelBaseView;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/cocinero", layout = MainLayout.class)
@PageTitle("Panel Cocina")
@Secured("ROLE_COCINERO")
public class PanelCocineroView extends PanelBaseView {

    public PanelCocineroView() {
        H2 titulo = new H2("Cocina");
        titulo.getStyle().set("margin", "0");

        Span subtitulo = new Span("Gestión de pedidos entrantes y en preparación");
        subtitulo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(titulo, subtitulo);

        add(Cards.cardLinkDestacada(
                "📅 Pedidos de hoy",
                "Gestiona y actualiza el estado de los pedidos del día",
                PedidosCocinaHoyView.class
        ));

        HorizontalLayout filaSecundaria = fila(
                Cards.cardLink("🧾 Entrantes", "Pedidos esperando aceptación", PedidosPendientesCocinaView.class),
                Cards.cardLink("🗂 Histórico", "Buscar pedidos antiguos y ver detalle", PedidosCocinaHistoricoView.class)
        );

        add(filaSecundaria);
    }
}