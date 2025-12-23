package com.serveat.view.empleado.administrador;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.estadisticas.EstadisticasService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Month;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route(value = "empleado/admin/estadisticas/graficas", layout = MainLayout.class)
@PageTitle("Gráficas de estadísticas | Admin")
@Secured("ROLE_ADMIN")
public class EstadisticasGraficasView extends VerticalLayout {

    private final transient FeatureService featureService;
    private final transient EstadisticasService estadisticasService;

    // filtros (solo vista, sin lógica)
    private final ComboBox<Integer> year = new ComboBox<>("Año");
    private final ComboBox<Month> month = new ComboBox<>("Mes");
    private final ComboBox<TipoPedidoCliente> tipoPedido = new ComboBox<>("Tipo pedido");
    private final ComboBox<MetodoPago> metodoPago = new ComboBox<>("Método pago");
    private final ComboBox<EstadoPedido> estadoPedido = new ComboBox<>("Estado pedido");
    private final ComboBox<EstadoCocina> estadoCocina = new ComboBox<>("Estado cocina");

    // autocompletar: seleccionas producto, no escribes “a mano”
    private final ComboBox<String> producto = new ComboBox<>("Producto");

    private final Button aplicar = new Button("Aplicar filtros");
    private final Button limpiar = new Button("Limpiar");

    private final Grid<Map<String, Object>> topUnidades = new Grid<>();
    private final Grid<Map<String, Object>> topFacturacion = new Grid<>();
    private final Grid<Map.Entry<String, Long>> cocinaEstados = new Grid<>();

    private final Span emptyUnidades = new Span();
    private final Span emptyFacturacion = new Span();

    private CallbackDataProvider<Map<String, Object>, Void> dpUnidades;
    private CallbackDataProvider<Map<String, Object>, Void> dpFacturacion;

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

        configurarFiltros();
        configurarGrids();
        configurarDataProviders();

        VerticalLayout cardFiltros = crearCard();
        cardFiltros.add(new H3("Filtros"), filtrosLayout());

        VerticalLayout card1 = crearCard();
        card1.add(new H3("Top productos por unidades"), emptyUnidades, topUnidades);

        VerticalLayout card2 = crearCard();
        card2.add(new H3("Top productos por facturación"), emptyFacturacion, topFacturacion);

        VerticalLayout card3 = crearCard();
        card3.add(new H3("Estados de cocina (conteo)"), cocinaEstados);

        add(cardFiltros, card1, card2, card3);

        refrescarTodo();
    }

    private void configurarFiltros() {
        year.setClearButtonVisible(true);
        month.setClearButtonVisible(true);
        tipoPedido.setClearButtonVisible(true);
        metodoPago.setClearButtonVisible(true);
        estadoPedido.setClearButtonVisible(true);
        estadoCocina.setClearButtonVisible(true);
        producto.setClearButtonVisible(true);

        year.setItems(estadisticasService.añosDisponibles());
        month.setItems(estadisticasService.mesesDisponibles());
        month.setItemLabelGenerator(estadisticasService::etiquetaMes);

        tipoPedido.setItems(TipoPedidoCliente.values());
        tipoPedido.setItemLabelGenerator(estadisticasService::etiquetaTipoPedido);

        metodoPago.setItems(MetodoPago.values());
        metodoPago.setItemLabelGenerator(estadisticasService::etiquetaMetodoPago);

        estadoPedido.setItems(EstadoPedido.values());
        estadoPedido.setItemLabelGenerator(estadisticasService::etiquetaEstadoPedido);

        estadoCocina.setItems(EstadoCocina.values());
        estadoCocina.setItemLabelGenerator(estadisticasService::etiquetaEstadoCocina);

        producto.setPlaceholder("Escribe para buscar (ej: coc)");
        producto.setItems(query -> {
            String filter = query.getFilter().orElse("");
            List<String> sug = estadisticasService.sugerirProductos(
                    filter,
                    year.getValue(), month.getValue(),
                    tipoPedido.getValue(),
                    metodoPago.getValue(),
                    estadoPedido.getValue(),
                    estadoCocina.getValue(),
                    15
            );
            return sug.stream();
        });

        aplicar.addClickListener(e -> refrescarTodo());
        limpiar.addClickListener(e -> {
            year.clear();
            month.clear();
            tipoPedido.clear();
            metodoPago.clear();
            estadoPedido.clear();
            estadoCocina.clear();
            producto.clear();
            refrescarTodo();
        });
    }

    private HorizontalLayout filtrosLayout() {
        HorizontalLayout row1 = new HorizontalLayout(year, month, tipoPedido);
        row1.setWidthFull();
        row1.getStyle().set("gap", "12px");

        HorizontalLayout row2 = new HorizontalLayout(metodoPago, estadoPedido, estadoCocina);
        row2.setWidthFull();
        row2.getStyle().set("gap", "12px");

        HorizontalLayout row3 = new HorizontalLayout(producto, aplicar, limpiar);
        row3.setWidthFull();
        row3.setAlignItems(FlexComponent.Alignment.END);
        row3.getStyle().set("gap", "12px");

        VerticalLayout wrap = new VerticalLayout(row1, row2, row3);
        wrap.setPadding(false);
        wrap.setSpacing(false);
        wrap.getStyle().set("gap", "10px");

        return new HorizontalLayout(wrap);
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

        cocinaEstados.addColumn(Map.Entry::getKey)
                .setHeader("Estado cocina").setAutoWidth(true).setFlexGrow(1);

        cocinaEstados.addColumn(e -> String.valueOf(e.getValue()))
                .setHeader("Cantidad").setAutoWidth(true);

        cocinaEstados.setWidthFull();
        cocinaEstados.setHeight("260px");

        emptyUnidades.getStyle().set("color", "var(--lumo-secondary-text-color)");
        emptyFacturacion.getStyle().set("color", "var(--lumo-secondary-text-color)");
    }

    private void configurarDataProviders() {
        dpUnidades = new CallbackDataProvider<>(
                q -> estadisticasService.topProductosPorUnidadesPage(
                        year.getValue(), month.getValue(),
                        tipoPedido.getValue(),
                        metodoPago.getValue(),
                        estadoPedido.getValue(),
                        estadoCocina.getValue(),
                        producto.getValue(),
                        q.getOffset(), q.getLimit()
                ).stream(),
                q -> (int) estadisticasService.topProductosPorUnidadesCount(
                        year.getValue(), month.getValue(),
                        tipoPedido.getValue(),
                        metodoPago.getValue(),
                        estadoPedido.getValue(),
                        estadoCocina.getValue(),
                        producto.getValue()
                )
        );

        dpFacturacion = new CallbackDataProvider<>(
                q -> estadisticasService.topProductosPorFacturacionPage(
                        year.getValue(), month.getValue(),
                        tipoPedido.getValue(),
                        metodoPago.getValue(),
                        estadoPedido.getValue(),
                        estadoCocina.getValue(),
                        producto.getValue(),
                        q.getOffset(), q.getLimit()
                ).stream(),
                q -> (int) estadisticasService.topProductosPorFacturacionCount(
                        year.getValue(), month.getValue(),
                        tipoPedido.getValue(),
                        metodoPago.getValue(),
                        estadoPedido.getValue(),
                        estadoCocina.getValue(),
                        producto.getValue()
                )
        );

        topUnidades.setDataProvider(dpUnidades);
        topFacturacion.setDataProvider(dpFacturacion);
    }

    private void refrescarTodo() {
        try {
            long c1 = estadisticasService.topProductosPorUnidadesCount(
                    year.getValue(), month.getValue(),
                    tipoPedido.getValue(),
                    metodoPago.getValue(),
                    estadoPedido.getValue(),
                    estadoCocina.getValue(),
                    producto.getValue()
            );

            long c2 = estadisticasService.topProductosPorFacturacionCount(
                    year.getValue(), month.getValue(),
                    tipoPedido.getValue(),
                    metodoPago.getValue(),
                    estadoPedido.getValue(),
                    estadoCocina.getValue(),
                    producto.getValue()
            );

            emptyUnidades.setText(c1 == 0 ? estadisticasService.mensajeSinResultados() : "");
            emptyFacturacion.setText(c2 == 0 ? estadisticasService.mensajeSinResultados() : "");

            dpUnidades.refreshAll();
            dpFacturacion.refreshAll();

            Map<String, Long> cocina = estadisticasService.resumenEstadosCocina(
                    year.getValue(), month.getValue(),
                    tipoPedido.getValue(),
                    estadoPedido.getValue()
            );
            cocinaEstados.setItems(cocina.entrySet());

        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4500, Notification.Position.MIDDLE);
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