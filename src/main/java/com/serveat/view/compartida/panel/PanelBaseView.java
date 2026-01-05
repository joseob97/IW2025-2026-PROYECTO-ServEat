package com.serveat.view.compartida.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

import java.util.List;
/*
 * Base para los paneles del camarero, cocinero y repartidor
 */
public abstract class PanelBaseView extends VerticalLayout {

    protected PanelBaseView() {
        setPadding(true);
        setSpacing(false);
        setWidthFull();

        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");
    }

    protected HorizontalLayout fila(Component... cards) {
        HorizontalLayout row = new HorizontalLayout(cards);
        row.setWidthFull();
        row.setSpacing(false);
        row.getStyle().set("gap", "14px");
        row.setAlignItems(FlexComponent.Alignment.STRETCH);
        return row;
    }

    protected static final class Cards {

        private Cards() {}

        public static VerticalLayout cardAccion(String titulo, String descripcion, String textoBoton, Runnable onClick) {
            H3 h3 = new H3(titulo);
            h3.getStyle().set("margin", "0");

            Paragraph p = new Paragraph(descripcion);
            p.getStyle().set("margin", "0");
            p.getStyle().set("color", "var(--lumo-secondary-text-color)");

            Button btn = new Button(textoBoton);
            btn.getStyle().set("font-weight", "600");
            btn.setWidth("260px");
            btn.addClickListener(e -> onClick.run());

            HorizontalLayout filaBoton = new HorizontalLayout(btn);
            filaBoton.setWidthFull();
            filaBoton.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

            VerticalLayout card = cardBase();
            card.add(h3, p, filaBoton);
            return card;
        }

        public static Component cardLink(String titulo, String descripcion, Class<? extends Component> destino) {
            RouterLink link = new RouterLink("", destino);
            link.getStyle().set("text-decoration", "none");
            link.getStyle().set("width", "100%");

            H3 h3 = new H3(titulo);
            h3.getStyle().set("margin", "0");

            Span desc = new Span(descripcion);
            desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

            VerticalLayout card = cardBase();
            card.getStyle().set("cursor", "pointer");
            card.getStyle().set("gap", "8px");
            card.add(h3, desc);

            link.add(card);
            return link;
        }

        public static Component cardLinkDestacada(String titulo, String descripcion, Class<? extends Component> destino) {
            RouterLink link = new RouterLink("", destino);
            link.getStyle().set("text-decoration", "none");
            link.getStyle().set("width", "100%");

            H3 h3 = new H3(titulo);
            h3.getStyle().set("margin", "0");

            Span desc = new Span(descripcion);
            desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

            VerticalLayout card = cardBase();
            card.getStyle().set("gap", "8px");
            card.getStyle().set("cursor", "pointer");

            card.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            card.getStyle().set("border", "1px solid var(--lumo-primary-color-50pct)");
            card.getStyle().set("border-radius", "16px");
            card.getStyle().set("box-shadow", "0 10px 26px rgba(0,0,0,0.08)");

            card.add(h3, desc);

            link.add(card);
            return link;
        }

        public static VerticalLayout renderEnFilasDeDos(List<Component> cards, String cardWidth) {
            VerticalLayout contenedor = new VerticalLayout();
            contenedor.setPadding(false);
            contenedor.setSpacing(true);
            contenedor.setWidthFull();

            for (int i = 0; i < cards.size(); i += 2) {
                HorizontalLayout fila = new HorizontalLayout();
                fila.setWidthFull();
                fila.setSpacing(true);
                fila.getStyle().set("justify-content", "center");

                Component izquierda = cards.get(i);
                fila.add(izquierda);

                if (i + 1 < cards.size()) {
                    Component derecha = cards.get(i + 1);
                    fila.add(derecha);
                } else {
                    VerticalLayout spacer = new VerticalLayout();
                    spacer.setWidth(cardWidth);
                    spacer.setPadding(false);
                    spacer.setSpacing(false);
                    spacer.getStyle().set("visibility", "hidden");
                    fila.add(spacer);
                }

                contenedor.add(fila);
            }

            return contenedor;
        }

        public static VerticalLayout cardAccionPro(String titulo,
                                                   String descripcion,
                                                   Class<? extends Component> destino,
                                                   boolean habilitado,
                                                   String textoBoton) {

            H3 h3 = new H3(titulo);
            h3.getStyle().set("margin", "0");

            Paragraph p = new Paragraph(descripcion);
            p.getStyle().set("margin", "0");
            p.getStyle().set("color", "var(--lumo-secondary-text-color)");

            Button btn = new Button(textoBoton);
            btn.setEnabled(habilitado);
            btn.getStyle().set("font-weight", "600");
            btn.setWidth("260px");

            if (habilitado) {
                btn.addClickListener(e -> UI.getCurrent().navigate(destino));
            } else {
                btn.addClickListener(e ->
                        Notification.show("Funcionalidad disponible con plan PRO", 3000, Notification.Position.MIDDLE)
                );
            }

            HorizontalLayout filaBoton = new HorizontalLayout(btn);
            filaBoton.setWidthFull();
            filaBoton.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

            VerticalLayout card = cardBase();
            card.add(h3, p, filaBoton);
            return card;
        }

        private static VerticalLayout cardBase() {
            VerticalLayout card = new VerticalLayout();
            card.setPadding(true);
            card.setSpacing(false);
            card.setWidthFull();
            card.getStyle().set("gap", "12px");

            card.getStyle().set("background", "var(--lumo-base-color)");
            card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
            card.getStyle().set("border-radius", "14px");
            card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

            return card;
        }
    }
}