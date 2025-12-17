package com.serveat.view.empleado.camarero;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/camarero", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class PanelCamareroView extends VerticalLayout {

    public PanelCamareroView() {
        setSpacing(true);
        setPadding(true);

        H2 titulo = new H2("Panel Camarero");

        RouterLink iniciar = new RouterLink("➕ Iniciar pedido (mesa)", IniciarPedidoView.class);
        RouterLink cancelar = new RouterLink("❌ Cancelar pedido", CancelarPedidoView.class);

        add(titulo, iniciar, cancelar);
    }
}