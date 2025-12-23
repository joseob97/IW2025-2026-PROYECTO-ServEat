package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.estadisticas.EstadisticasService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route(value = "empleado/admin/estadisticas/graficas", layout = MainLayout.class)
@PageTitle("Gráficas de estadísticas | Admin")
@Secured("ROLE_ADMIN")
public class EstadisticasGraficasView extends VerticalLayout {

    private final transient FeatureService featureService;
    private final transient EstadisticasService estadisticasService;

    private final Grid<Map<String, Object>> topUnidades = new Grid<>();
    private final Grid<Map<String, Object>> topFacturacion = new Grid<>();
    private final Grid<Map.Entry<String, Long>> cocinaEstados = new Grid<>();

    public EstadisticasGraficasView(FeatureService featureService,
                                         EstadisticasService estadisticasService) {

        this.featureService = featureService;
        this.estadisticasService = estadisticasService;

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Gráficas");
        titulo.getStyle().set("margin", "0");

        Span subtitulo = new Span("Rankings y distribución por estados en tablas.");
        subtitulo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        Button volver = new Button("⬅ Volver");
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(EstadisticasAdminView.class)));

        HorizontalLayout top = new HorizontalLayout(titulo, volver);
        top.setWidthFull();
        top.setAlignItems(FlexComponent.Alignment.CENTER);
        top.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        add(top, subtitulo);

        if (!featureService.tieneFeature(Feature.ESTADISTICAS)) {
            add(bloqueado());
            return;
        }

        configurarGrids();

        VerticalLayout card1 = crearCard();
        card1.add(new H3("Top productos por unidades"), topUnidades);

        VerticalLayout card2 = crearCard();
        card2.add(new H3("Top productos por facturación"), topFacturacion);

        VerticalLayout card3 = crearCard();
        card3.add(new H3("Estados de cocina (conteo)"), cocinaEstados);

        add(card1, card2, card3);

        cargar();
    }

    private void configurarGrids() {
        topUnidades.addColumn(m -> String.valueOf(m.getOrDefault("producto", "-")))
                .setHeader("Producto").setAutoWidth(true).setFlexGrow(1);

        topUnidades.addColumn(m -> String.valueOf(m.getOrDefault("unidades", 0)))
                .setHeader("Unidades").setAutoWidth(true);

        topUnidades.setWidthFull();
        topUnidades.setHeight("320px");

        topFacturacion.addColumn(m -> String.valueOf(m.getOrDefault("producto", "-")))
                .setHeader("Producto").setAutoWidth(true).setFlexGrow(1);

        topFacturacion.addColumn(m -> formatoEuro((BigDecimal) m.get("total")))
                .setHeader("Total").setAutoWidth(true);

        topFacturacion.setWidthFull();
        topFacturacion.setHeight("320px");

        cocinaEstados.addColumn(Map.Entry::getKey).setHeader("Estado cocina").setAutoWidth(true).setFlexGrow(1);
        cocinaEstados.addColumn(e -> String.valueOf(e.getValue())).setHeader("Cantidad").setAutoWidth(true);
        cocinaEstados.setWidthFull();
        cocinaEstados.setHeight("260px");
    }

    private void cargar() {
        try {
            List<Map<String, Object>> u = estadisticasService.topProductosPorUnidades(10);
            List<Map<String, Object>> f = estadisticasService.topProductosPorFacturacion(10);
            Map<String, Long> cocina = estadisticasService.resumenEstadosCocina();

            topUnidades.setItems(u);
            topFacturacion.setItems(f);
            cocinaEstados.setItems(cocina.entrySet());

        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private VerticalLayout bloqueado() {
        VerticalLayout card = crearCard();
        H3 h3 = new H3("Funcionalidad no disponible");
        Span p1 = new Span("Esta funcionalidad requiere el plan PRO.");
        p1.getStyle().set("color", "var(--lumo-secondary-text-color)");
        Span p2 = new Span("Ve a “Suscripción / Plan” para activarla.");
        p2.getStyle().set("color", "var(--lumo-secondary-text-color)");
        card.add(h3, p1, p2);
        return card;
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