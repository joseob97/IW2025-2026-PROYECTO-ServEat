package com.serveat.view.cliente.pedido;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "cliente/pedido/tipo", layout = MainLayout.class)
@PageTitle("Tipo de pedido")
@Secured("ROLE_CLIENTE")
public class ElegirTipoPedidoView extends VerticalLayout {

    public ElegirTipoPedidoView() {
        setPadding(true);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        add(new H3("¿Cómo quieres tu pedido?"));

        Button recoger = new Button("🛍 Para recoger",
                e -> getUI().ifPresent(ui -> ui.navigate("cliente/pedido/recoger")));

        Button domicilio = new Button("🏠 A domicilio",
                e -> getUI().ifPresent(ui -> ui.navigate("cliente/pedido/domicilio")));

        recoger.setWidth("280px");
        domicilio.setWidth("280px");

        add(recoger, domicilio);
    }
}