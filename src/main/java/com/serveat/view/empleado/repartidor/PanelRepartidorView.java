package com.serveat.view.empleado.repartidor;

import com.serveat.view.compartida.panel.PanelBaseView;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@PageTitle("Panel Repartidor | Empleado")
@Route(value = "empleado/repartidor", layout = MainLayout.class)
@Secured("ROLE_REPARTIDOR")
public class PanelRepartidorView extends PanelBaseView {

    public PanelRepartidorView() {
        H2 titulo = new H2("Repartos");
        titulo.getStyle().set("margin", "0");

        Span subtitulo = new Span("Gestiona pedidos a domicilio listos para repartir y tus entregas asignadas.");
        subtitulo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(titulo, subtitulo);

        add(fila(
                Cards.cardLink("🧾 Pedidos disponibles", "Ver pedidos listos para asignarte.", PedidosDisponiblesView.class),
                Cards.cardLink("🚚 Mis repartos", "Actualizar estados y completar entregas.", MisRepartosView.class)
        ));
    }
}