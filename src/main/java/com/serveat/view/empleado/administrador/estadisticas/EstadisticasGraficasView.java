package com.serveat.view.empleado.administrador.estadisticas;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Route(value = "empleado/admin/estadisticas/graficas", layout = MainLayout.class)
@PageTitle("Gráficas de estadísticas | Admin")
@Secured("ROLE_ADMIN")
public class EstadisticasGraficasView extends VerticalLayout {

    private final transient FeatureService featureService;
    private final transient EstadisticasService estadisticasService;

    /* Filtros TOP (solo fechas)  */
    private final DatePicker desde = new DatePicker("Desde");
    private final DatePicker hasta = new DatePicker("Hasta");
    private final Button buscarTop = new Button("Buscar");
    private final Button limpiarTop = new Button("Limpiar");

    /* Filtros Serie mensual (solo años + tipo) */
    private final ComboBox<Integer> yearInicio = new ComboBox<>("Año inicio");
    private final ComboBox<Integer> yearFin = new ComboBox<>("Año fin");
    private final ComboBox<String> tipoSerie = new ComboBox<>("Serie");
    private final Button buscarSerie = new Button("Buscar");
    private final Button limpiarSerie = new Button("Limpiar");

    /* Grids */
    private final Grid<Map<String, Object>> topUnidades = new Grid<>();
    private final Grid<Map<String, Object>> topFacturacion = new Grid<>();
    private final Grid<Map<String, Object>> serieMensual = new Grid<>();

    private final Span emptyTop = new Span();
    private final Span emptySerie = new Span();

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

        Button volver = new Button("⬅ Volver");
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(EstadisticasAdminView.class)));

        HorizontalLayout header = new HorizontalLayout(titulo, volver);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        add(header);

        if (!featureService.tieneFeature(Feature.ESTADISTICAS)) {
            add(bloqueado());
            return;
        }

        configurarFiltros();
        configurarGrids();

        add(
                crearCard(new H3("Filtros"), filtrosTopLayout()),

                crearCard(new H3("Top productos por unidades"), emptyTop, topUnidades),
                crearCard(new H3("Top productos por facturación"), topFacturacion),

                crearCard(new H3("Evolución mensual"), filtrosSerieLayout(), emptySerie, serieMensual)
        );
    }

    /* Config filtros */

    private void configurarFiltros() {
        // TOP
        desde.setClearButtonVisible(true);
        hasta.setClearButtonVisible(true);

        buscarTop.addClickListener(e -> cargarTop());
        limpiarTop.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            limpiarTopTablas();
        });

        // Serie
        yearInicio.setItems(estadisticasService.añosDisponibles());
        yearFin.setItems(estadisticasService.añosDisponibles());

        tipoSerie.setItems("Unidades", "Facturación");
        tipoSerie.setValue("Unidades");

        buscarSerie.addClickListener(e -> cargarSerie());
        limpiarSerie.addClickListener(e -> {
            yearInicio.clear();
            yearFin.clear();
            tipoSerie.setValue("Unidades");
            limpiarSerieTabla();
        });

        emptyTop.getStyle().set("color", "var(--lumo-secondary-text-color)");
        emptySerie.getStyle().set("color", "var(--lumo-secondary-text-color)");
    }

    /* Layout filtros */

    private Component filtrosTopLayout() {
        desde.setWidthFull();
        hasta.setWidthFull();
        buscarTop.setWidth("140px");
        limpiarTop.setWidth("140px");

        HorizontalLayout row = new HorizontalLayout(desde, hasta, buscarTop, limpiarTop);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.END);
        row.getStyle().set("gap", "12px");
        return row;
    }

    private Component filtrosSerieLayout() {
        yearInicio.setWidthFull();
        yearFin.setWidthFull();
        tipoSerie.setWidthFull();

        buscarSerie.setWidth("140px");
        limpiarSerie.setWidth("140px");

        HorizontalLayout row1 = new HorizontalLayout(yearInicio, yearFin, tipoSerie);
        row1.setWidthFull();
        row1.getStyle().set("gap", "12px");

        HorizontalLayout row2 = new HorizontalLayout(buscarSerie, limpiarSerie);
        row2.setWidthFull();
        row2.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        row2.getStyle().set("gap", "12px");

        VerticalLayout v = new VerticalLayout(row1, row2);
        v.setPadding(false);
        v.setSpacing(true);
        v.setWidthFull();
        return v;
    }

    /* Config grids */

    private void configurarGrids() {
        // TOP unidades
        topUnidades.addColumn(m -> m.get("producto"))
                .setHeader("Producto").setFlexGrow(1);
        topUnidades.addColumn(m -> m.get("unidades"))
                .setHeader("Unidades");

        // TOP facturación
        topFacturacion.addColumn(m -> m.get("producto"))
                .setHeader("Producto").setFlexGrow(1);
        topFacturacion.addColumn(m -> formatoEuro((BigDecimal) m.get("total")))
                .setHeader("Total");

        // Serie mensual
        serieMensual.addColumn(m -> m.get("mes"))
                .setHeader("Mes");
        serieMensual.addColumn(m -> m.get("valor"))
                .setHeader("Valor");

        serieMensual.setClassNameGenerator(m ->
                Boolean.TRUE.equals(m.get("max")) ? "max-row" : ""
        );
    }

    /*  Acciones TOP */

    private void cargarTop() {
        try {
            LocalDate d = desde.getValue();
            LocalDate h = hasta.getValue();

            if (d != null && h != null && d.isAfter(h)) {
                Notification.show("Rango de fechas inválido.", 3000, Notification.Position.MIDDLE);
                return;
            }

            var topU = estadisticasService.topProductosPorUnidades(d, h, 15);
            var topF = estadisticasService.topProductosPorFacturacion(d, h, 15);

            emptyTop.setText(topU.isEmpty() && topF.isEmpty() ? "No hay datos disponibles" : "");

            topUnidades.setItems(topU);
            topFacturacion.setItems(topF);

        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void limpiarTopTablas() {
        topUnidades.setItems();
        topFacturacion.setItems();
        emptyTop.setText("");
    }

    /*  Acciones Serie mensual */

    private void cargarSerie() {
        try {
            Integer yi = yearInicio.getValue();
            Integer yf = yearFin.getValue();

            if (yi == null || yf == null || yi > yf) {
                emptySerie.setText("Selecciona un rango de años válido");
                serieMensual.setItems();
                return;
            }

            var rows = estadisticasService.serieMensualVista(yi, yf, tipoSerie.getValue());

            if (rows.isEmpty()) {
                emptySerie.setText("No hay datos disponibles");
                serieMensual.setItems();
                return;
            }

            serieMensual.setItems(rows);
            emptySerie.setText("");

        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void limpiarSerieTabla() {
        serieMensual.setItems();
        emptySerie.setText("");
    }

    /* UI helpers */

    private Component bloqueado() {
        return crearCard(
                new H3("Funcionalidad no disponible"),
                new Span("Esta funcionalidad requiere el plan PRO.")
        );
    }

    private VerticalLayout crearCard(Component... content) {
        VerticalLayout card = new VerticalLayout(content);
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle().set("gap", "12px");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        return card;
    }

    private String formatoEuro(BigDecimal total) {
        if (total == null) return "0,00 €";
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
        return nf.format(total);
    }
}