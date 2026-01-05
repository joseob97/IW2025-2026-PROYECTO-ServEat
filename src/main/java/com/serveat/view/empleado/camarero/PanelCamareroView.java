package com.serveat.view.empleado.camarero;

import com.serveat.view.compartida.panel.PanelBaseView;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/camarero", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class PanelCamareroView extends PanelBaseView {

    public PanelCamareroView() {
        H2 titulo = new H2("Panel Camarero");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        add(fila(
                Cards.cardAccion(
                        "➕ Iniciar pedido",
                        "Crea un nuevo pedido para una mesa y añade productos.",
                        "Abrir",
                        () -> UI.getCurrent().navigate(IniciarPedidoView.class)
                ),
                Cards.cardAccion(
                        "📋 Consultar pedidos",
                        "Filtra pedidos por fecha/estado/mesa, ver detalles y generar ticket.",
                        "Abrir",
                        () -> UI.getCurrent().navigate(PedidosCamareroView.class)
                )
        ));
    }
}