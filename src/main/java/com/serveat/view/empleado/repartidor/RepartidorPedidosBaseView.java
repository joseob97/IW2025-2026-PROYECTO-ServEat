package com.serveat.view.empleado.repartidor;

import com.serveat.domain.pedido.Pedido;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Vista base para abstraer MisRepartosView y PedidosDisponiblesView */
public abstract class RepartidorPedidosBaseView extends VerticalLayout {

    // filtros comunes
    protected final DatePicker desde = new DatePicker("Desde");
    protected final DatePicker hasta = new DatePicker("Hasta");
    protected final Button btnBuscar = new Button("Buscar");
    protected final Button btnLimpiar = new Button("Limpiar");
    protected final Button refrescar = new Button("🔄 Refrescar");

    // grid + paginación comunes
    protected final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    protected final Button prev = new Button("◀ Anterior");
    protected final Button next = new Button("Siguiente ▶");
    protected final Span infoPagina = new Span("");

    protected int pageIndex = 0;
    protected final int pageSize = 10;
    protected long totalItems = 0;

    protected final Span info = new Span();

    private boolean initialized = false;

    protected RepartidorPedidosBaseView() {
        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");
    }

    /**
     * Llamar desde el constructor del HIJO cuando ya estén inicializados
     * los campos del hijo (p.ej. filtroEstado).
     */
    protected final void initBase() {
        if (initialized) return;
        initialized = true;


        desde.addValueChangeListener(e -> {
            LocalDate d = e.getValue();
            hasta.setMin(d);

            if (d != null && hasta.getValue() != null && hasta.getValue().isBefore(d)) {
                hasta.clear();
            }
        });

        hasta.addValueChangeListener(e -> {
            LocalDate h = e.getValue();
            desde.setMax(h);

            if (h != null && desde.getValue() != null && desde.getValue().isAfter(h)) {
                desde.clear();
            }
        });

        H3 titulo = new H3(tituloPantalla());
        titulo.getStyle().set("margin", "0");

        info.setText(textoInfo());
        info.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(titulo, crearBloqueFiltros(), crearBloqueGrid(), crearBloquePaginacion());

        configurarGrid();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // por si alguien se olvida de llamar initBase() en el hijo
        if (!initialized) initBase();

        cargarPagina(0);
    }

    // Hooks obligatorios
    protected abstract String tituloPantalla();
    protected abstract String textoInfo();
    protected abstract void configurarGrid();
    protected abstract Page<Pedido> buscarPage(LocalDateTime d, LocalDateTime h, Pageable pageable);

    // Hooks opcionales
    protected Component filtroExtra() { return null; }
    protected void limpiarFiltroExtra() {}
    protected void onEmptyResults() {}

    // UI común

    private Component crearBloqueFiltros() {
        VerticalLayout card = crearCard();

        btnBuscar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnBuscar.getStyle().set("font-weight", "700");
        btnBuscar.addClickListener(e -> {
            LocalDate d = desde.getValue();
            LocalDate h = hasta.getValue();
            if (d != null && h != null && h.isBefore(d)) {
                Notification.show("La fecha 'Hasta' no puede ser anterior a 'Desde'", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        btnLimpiar.addClickListener(e -> {
            desde.clear();
            hasta.clear();
            limpiarFiltroExtra();
            pageIndex = 0;
            cargarPagina(pageIndex);
        });

        refrescar.addClickListener(e -> cargarPagina(pageIndex));

        HorizontalLayout fila = new HorizontalLayout();
        fila.setWidthFull();
        fila.setSpacing(false);
        fila.getStyle().set("gap", "12px");
        fila.setAlignItems(Alignment.END);

        fila.add(desde, hasta);

        Component extra = filtroExtra();
        if (extra != null) fila.add(extra);

        fila.add(btnBuscar, btnLimpiar);

        HorizontalLayout barra = new HorizontalLayout(refrescar);
        barra.setWidthFull();
        barra.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        card.add(info, fila, barra);
        return card;
    }

    private Component crearBloqueGrid() {
        VerticalLayout card = crearCard();
        grid.setWidthFull();
        grid.setHeight("520px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.getStyle().set("border-radius", "10px");
        grid.getStyle().set("overflow", "hidden");
        card.add(grid);
        return card;
    }

    private Component crearBloquePaginacion() {
        HorizontalLayout barra = new HorizontalLayout(prev, infoPagina, next);
        barra.setWidthFull();
        barra.setAlignItems(Alignment.CENTER);
        barra.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        barra.setSpacing(false);
        barra.getStyle().set("gap", "12px");

        prev.addClickListener(e -> {
            if (pageIndex > 0) {
                pageIndex--;
                cargarPagina(pageIndex);
            }
        });

        next.addClickListener(e -> {
            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            if (pageIndex < maxPage) {
                pageIndex++;
                cargarPagina(pageIndex);
            }
        });

        return barra;
    }

    protected final void cargarPagina(int index) {
        try {
            Pageable pageable = PageRequest.of(index, pageSize);

            LocalDateTime d = toStartOfDay(desde.getValue());
            LocalDateTime h = toEndOfDay(hasta.getValue());

            Page<Pedido> page = buscarPage(d, h, pageable);

            totalItems = page.getTotalElements();
            grid.setItems(page.getContent());

            int maxPage = (int) Math.max(0, (totalItems - 1) / pageSize);
            prev.setEnabled(index > 0);
            next.setEnabled(index < maxPage);

            long from = totalItems == 0 ? 0 : (index * pageSize + 1L);
            long to = Math.min(totalItems, (long) (index + 1) * pageSize);
            infoPagina.setText("Mostrando " + from + "-" + to + " de " + totalItems);

            if (totalItems == 0) onEmptyResults();

        } catch (Exception ex) {
            totalItems = 0;
            grid.setItems();
            prev.setEnabled(false);
            next.setEnabled(false);
            infoPagina.setText("");
            Notification.show("Error al cargar pedidos: " + ex.getMessage(), 4500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    protected VerticalLayout crearCard() {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");
        card.getStyle().set("gap", "12px");
        return card;
    }

    protected LocalDateTime toStartOfDay(LocalDate d) {
        return d == null ? null : d.atStartOfDay();
    }

    protected LocalDateTime toEndOfDay(LocalDate d) {
        return d == null ? null : d.atTime(LocalTime.MAX);
    }
}