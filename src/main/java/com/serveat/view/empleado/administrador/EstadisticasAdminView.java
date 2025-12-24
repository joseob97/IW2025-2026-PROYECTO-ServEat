package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.estadisticas.EstadisticasService;
import com.serveat.service.estadisticas.EstadisticasSnapshot;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
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

@Route(value = "empleado/admin/estadisticas", layout = MainLayout.class)
@PageTitle("Estadísticas | Admin")
@Secured("ROLE_ADMIN")
public class EstadisticasAdminView extends VerticalLayout {

    private final transient FeatureService featureService;
    private final transient EstadisticasService estadisticasService;

    /* Filtro por rango */
    private final DatePicker desde = new DatePicker("Desde");
    private final DatePicker hasta = new DatePicker("Hasta");
    private final Button buscar = new Button("Buscar");
    private final Button limpiar = new Button("Limpiar");

    private final Span totalPedidos = new Span("-");
    private final Span pedidosConfirmados = new Span("-");
    private final Span pedidosCancelados = new Span("-");
    private final Span pagosConfirmados = new Span("-");
    private final Span totalFacturado = new Span("-");

    private final Span modoResumen = new Span();
    private final Span mensajeNoDatos = new Span();

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

        /* Card filtros */
        VerticalLayout bloqueFiltros = crearCard();
        bloqueFiltros.add(new H3("Filtro por fecha"), filtrosFechaLayout());

        /* Card resumen */
        VerticalLayout bloqueResumen = crearCard();
        bloqueResumen.add(new H3("Resumen general"));

        modoResumen.getStyle().set("color", "var(--lumo-secondary-text-color)");
        mensajeNoDatos.getStyle().set("color", "var(--lumo-error-text-color)");
        mensajeNoDatos.getStyle().set("font-weight", "600");

        bloqueResumen.add(
                modoResumen,
                mensajeNoDatos,
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

        /* Acciones */
        Button verGraficas = new Button("📊 Ver gráficas");
        verGraficas.setWidth("180px");
        verGraficas.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(EstadisticasGraficasView.class)));

        Button refrescar = new Button("🔄 Refrescar (async)");
        refrescar.setWidth("190px");
        refrescar.addClickListener(e -> {
            estadisticasService.recalcularEstadisticasAsync();
            Notification.show("Refresco lanzado en segundo plano.", 2500, Notification.Position.MIDDLE);
        });

        HorizontalLayout accionesRow = new HorizontalLayout(verGraficas, refrescar);
        accionesRow.setAlignItems(FlexComponent.Alignment.CENTER);
        accionesRow.getStyle().set("gap", "10px");

        VerticalLayout acciones = crearCard();
        acciones.add(new H3("Detalle"), accionesRow);

        add(bloqueFiltros, bloqueResumen, acciones);

        configurarFiltros();
        cargar();
    }

    private void configurarFiltros() {
        desde.setClearButtonVisible(true);
        hasta.setClearButtonVisible(true);

        buscar.addClickListener(e -> {
            LocalDate d = desde.getValue();
            LocalDate h = hasta.getValue();
            if (d != null && h != null && d.isAfter(h)) {
                Notification.show("La fecha 'Desde' no puede ser posterior a 'Hasta'.", 3500, Notification.Position.MIDDLE);
                return;
            }
            cargar();
        });

        limpiar.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            cargar();
        });
    }

    private Component filtrosFechaLayout() {
        desde.setWidthFull();
        hasta.setWidthFull();

        buscar.setWidth("140px");
        limpiar.setWidth("140px");

        HorizontalLayout row = new HorizontalLayout(desde, hasta, buscar, limpiar);
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.END);
        row.getStyle().set("gap", "12px");
        return row;
    }

    private void cargar() {
        try {
            mensajeNoDatos.setText("");

            LocalDate d = desde.getValue();
            LocalDate h = hasta.getValue();

            EstadisticasSnapshot snap = estadisticasService.snapshotRango(d, h);

            if (d == null && h == null) {
                modoResumen.setText("Mostrando: Global (sin filtro de fecha)");
            } else {
                String dd = (d == null) ? "—" : d.toString();
                String hh = (h == null) ? "—" : h.toString();
                modoResumen.setText("Mostrando: " + dd + " → " + hh);
            }

            if (!snap.isHayDatos()) {
                ponerKpisCero();
                mensajeNoDatos.setText("No hay datos disponibles");
                return;
            }

            totalPedidos.setText(String.valueOf(snap.getTotalPedidos()));
            pedidosConfirmados.setText(String.valueOf(snap.getPedidosConfirmados()));
            pedidosCancelados.setText(String.valueOf(snap.getPedidosCancelados()));
            pagosConfirmados.setText(String.valueOf(snap.getPagosConfirmados()));
            totalFacturado.setText(formatoEuro(snap.getTotalFacturado()));

        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void ponerKpisCero() {
        totalPedidos.setText("0");
        pedidosConfirmados.setText("0");
        pedidosCancelados.setText("0");
        pagosConfirmados.setText("0");
        totalFacturado.setText(formatoEuro(BigDecimal.ZERO));
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