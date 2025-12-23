package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.estadisticas.EstadisticasService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Route(value = "empleado/admin/estadisticas", layout = MainLayout.class)
@PageTitle("Estadísticas | Admin")
@Secured("ROLE_ADMIN")
public class EstadisticasAdminView extends VerticalLayout {

    private final transient FeatureService featureService;
    private final transient EstadisticasService estadisticasService;

    private final Span totalPedidos = new Span("-");
    private final Span pedidosConfirmados = new Span("-");
    private final Span pedidosCancelados = new Span("-");
    private final Span pagosConfirmados = new Span("-");
    private final Span totalFacturado = new Span("-");

    public EstadisticasAdminView(FeatureService featureService,
                                 EstadisticasService estadisticasService) {

        this.featureService = featureService;
        this.estadisticasService = estadisticasService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Estadísticas");
        titulo.getStyle().set("margin", "0");

        Span subtitulo = new Span("Resumen del rendimiento del establecimiento.");
        subtitulo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(titulo, subtitulo);

        if (!featureService.tieneFeature(Feature.ESTADISTICAS)) {
            add(bloqueado());
            return;
        }

        VerticalLayout bloqueResumen = crearCard();
        bloqueResumen.add(new H3("Resumen general"));
        bloqueResumen.add(
                filaCards(
                        kpiCard("Total pedidos", totalPedidos),
                        kpiCard("Pedidos confirmados", pedidosConfirmados),
                        kpiCard("Pedidos cancelados", pedidosCancelados)
                ),
                filaCards(
                        kpiCard("Pagos confirmados", pagosConfirmados),
                        kpiCard("Total facturado", totalFacturado)
                )
        );

        add(bloqueResumen);

        cargar();
    }

    private void cargar() {
        try {
            totalPedidos.setText(String.valueOf(estadisticasService.totalPedidos()));
            pedidosConfirmados.setText(String.valueOf(estadisticasService.pedidosConfirmados()));
            pedidosCancelados.setText(String.valueOf(estadisticasService.pedidosCancelados()));
            pagosConfirmados.setText(String.valueOf(estadisticasService.pagosConfirmados()));

            BigDecimal total = estadisticasService.totalFacturado();
            totalFacturado.setText(formatoEuro(total));
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private Component bloqueado() {
        VerticalLayout card = crearCard();

        H3 h3 = new H3("Funcionalidad no disponible");
        h3.getStyle().set("margin", "0");

        Span p1 = new Span("Esta funcionalidad requiere el plan PRO.");
        p1.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Span p2 = new Span("Ve a “Suscripción / Plan” para activarla.");
        p2.getStyle().set("color", "var(--lumo-secondary-text-color)");

        card.add(h3, p1, p2);
        return card;
    }

    private HorizontalLayout filaCards(Component... cards) {
        HorizontalLayout row = new HorizontalLayout(cards);
        row.setWidthFull();
        row.setSpacing(false);
        row.getStyle().set("gap", "14px");
        return row;
    }

    private Component kpiCard(String titulo, Span valor) {
        VerticalLayout c = new VerticalLayout();
        c.setPadding(true);
        c.setSpacing(false);
        c.setWidthFull();
        c.getStyle().set("gap", "8px");
        c.getStyle().set("background", "var(--lumo-base-color)");
        c.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        c.getStyle().set("border-radius", "14px");
        c.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

        Span t = new Span(titulo);
        t.getStyle().set("color", "var(--lumo-secondary-text-color)");
        t.getStyle().set("font-size", "var(--lumo-font-size-s)");

        valor.getStyle().set("font-weight", "700");
        valor.getStyle().set("font-size", "var(--lumo-font-size-xxl)");

        c.add(t, valor);
        return c;
    }

    private VerticalLayout crearCard() {
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

    private String formatoEuro(BigDecimal total) {
        if (total == null) return "0,00 €";
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        return nf.format(total);
    }
}