package com.serveat.view.empleado.cocinero;

import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@Route(value = "empleado/cocinero", layout = MainLayout.class)
@Secured("ROLE_COCINERO")
public class PanelCocineroView extends VerticalLayout {

    public PanelCocineroView() {

        setPadding(true);
        setSpacing(false);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Panel Cocinero");
        titulo.getStyle().set("margin", "0");
        add(titulo);


        HorizontalLayout fila1 = new HorizontalLayout(
                crearCardAccion(
                        "🍽️ Ver Comandas Pendientes",
                        "Visualiza todos los pedidos esperando ser preparados.",
                        () -> UI.getCurrent().navigate(ComandasView.class)
                ),
                crearCardAccion(
                        "👨‍🍳 Gestionar Preparación",
                        "Actualiza el estado de preparación de los platos.",
                        () -> UI.getCurrent().navigate(GestionPedidoCocineroView.class)
                )
        );

        configurarFila(fila1);


        HorizontalLayout fila2 = new HorizontalLayout(
                crearCardAccion(
                        "📋 Ver Historial",
                        "Consulta los pedidos ya completados.",
                        () -> {
                            // Función a implementar o navegar a otra vista
                            com.vaadin.flow.component.notification.Notification.show("Próximamente disponible", 3000,
                                    com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
                        }
                )
        );

        configurarFila(fila2);

        add(fila1, fila2);
    }


    private void configurarFila(HorizontalLayout fila) {
        fila.setWidthFull();
        fila.setSpacing(false);
        fila.getStyle().set("gap", "16px");
        fila.setAlignItems(FlexComponent.Alignment.STRETCH);
    }

    private VerticalLayout crearCardAccion(String titulo, String descripcion, Runnable onClick) {

        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle().set("gap", "12px");

        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

        H3 h3 = new H3(titulo);
        h3.getStyle().set("margin", "0");

        Paragraph p = new Paragraph(descripcion);
        p.getStyle().set("margin", "0");
        p.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button btn = new Button("Abrir");
        btn.getStyle().set("font-weight", "600");
        btn.setWidth("260px");
        btn.addClickListener(e -> onClick.run());

        HorizontalLayout filaBoton = new HorizontalLayout(btn);
        filaBoton.setWidthFull();
        filaBoton.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        card.add(h3, p, filaBoton);
        return card;
    }
}