package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public abstract class CrearPedidoCartaBaseView extends VerticalLayout {

    protected final transient PedidoService pedidoService;
    protected final transient ProductoService productoService;
    protected final transient CategoriaService categoriaService;

    protected Pedido carrito = new Pedido();

    /* Filtros */
    protected final ComboBox<String> categoriaFiltro = new ComboBox<>("Categoría");
    protected final TextField buscador = new TextField("Buscar");

    /* Zona productos */
    protected final VerticalLayout contenido = new VerticalLayout();

    /* Carrito */
    protected final Grid<LineaPedido> gridCarrito = new Grid<>(LineaPedido.class, false);
    protected final Span total = new Span("Total: 0 €");

    /* Acción principal */
    protected final Button continuar = new Button("➡ Continuar");

    /* Modo edición del carrito */
    private boolean editarCarrito = false;
    private Grid.Column<LineaPedido> colAcciones;
    private final Button btnEditarCarrito = new Button("✏️ Editar carrito");

    protected CrearPedidoCartaBaseView(PedidoService pedidoService,
                                       ProductoService productoService,
                                       CategoriaService categoriaService) {
        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setWidthFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1280px");
        getStyle().set("margin", "0 auto");

        /* Inicialización defensiva del carrito en memoria */
        if (carrito.getLineaPedidos() == null) {
            carrito.setLineaPedidos(new ArrayList<>());
        }
    }

    protected void construirUI(String tituloPantalla) {

        H3 titulo = new H3(tituloPantalla);
        titulo.getStyle().set("margin", "0");

        configurarFiltros();

        HorizontalLayout filtros = new HorizontalLayout(categoriaFiltro, buscador);
        filtros.setWidthFull();
        filtros.setSpacing(false);
        filtros.getStyle().set("gap", "12px");
        categoriaFiltro.setWidth("260px");
        buscador.setWidth("360px");

        HorizontalLayout main = new HorizontalLayout();
        main.setWidthFull();
        main.setSpacing(false);
        main.getStyle().set("gap", "16px");

        VerticalLayout izquierda = crearCard();
        izquierda.setWidthFull();
        izquierda.getStyle().set("flex", "2");

        contenido.setWidthFull();
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.getStyle().set("gap", "18px");

        izquierda.add(filtros, contenido);

        VerticalLayout derecha = new VerticalLayout();
        derecha.setPadding(false);
        derecha.setSpacing(false);
        derecha.getStyle().set("gap", "14px");
        derecha.getStyle().set("flex", "1");
        derecha.setWidth("520px");

        VerticalLayout cardCarrito = crearCard();

        H3 hCarrito = new H3("Carrito");
        hCarrito.getStyle().set("margin", "0");

        btnEditarCarrito.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnEditarCarrito.getStyle().set("font-weight", "700");

        HorizontalLayout filaTopCarrito = new HorizontalLayout(hCarrito);
        filaTopCarrito.setWidthFull();
        filaTopCarrito.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout filaBotonEditar = new HorizontalLayout(btnEditarCarrito);
        filaBotonEditar.setWidthFull();
        filaBotonEditar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        btnEditarCarrito.addClickListener(e -> {
            editarCarrito = !editarCarrito;
            btnEditarCarrito.setText(editarCarrito ? "✅ Listo" : "✏️ Editar carrito");

            if (colAcciones != null) {
                colAcciones.setVisible(editarCarrito);
            }
            gridCarrito.getDataProvider().refreshAll();
        });

        configurarGridCarrito();

        cardCarrito.add(filaTopCarrito, filaBotonEditar, gridCarrito, total);

        VerticalLayout cardDetalles = crearCard();
        H3 hDetalles = new H3("Detalles");
        hDetalles.getStyle().set("margin", "0");
        cardDetalles.add(hDetalles, construirBloqueDetalles());

        continuar.setWidthFull();
        continuar.getStyle().set("font-weight", "700");
        continuar.addClickListener(e -> onContinuar());

        cardDetalles.add(continuar);

        derecha.add(cardCarrito, cardDetalles);

        main.add(izquierda, derecha);
        main.setFlexGrow(2, izquierda);
        main.setFlexGrow(1, derecha);

        add(titulo, main);

        cargarProductos();
        refrescarCarrito();
    }

    private void configurarFiltros() {
        categoriaFiltro.setItems(
                categoriaService.listarCategorias().stream()
                        .map(Categoria::getNombre)
                        .filter(Objects::nonNull)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList()
        );
        categoriaFiltro.setClearButtonVisible(true);
        categoriaFiltro.addValueChangeListener(e -> cargarProductos());

        buscador.setPlaceholder("Buscar por nombre...");
        buscador.setClearButtonVisible(true);
        buscador.setValueChangeMode(ValueChangeMode.EAGER);
        buscador.addValueChangeListener(e -> cargarProductos());
    }

    protected void cargarProductos() {
        String categoria = categoriaFiltro.getValue();
        String texto = buscador.getValue() != null ? buscador.getValue().trim() : "";

        List<Producto> productos;

        if (categoria != null && !categoria.isBlank()) {
            productos = productoService.buscarPorCategoria(categoria);
            if (!texto.isBlank()) {
                String t = texto.toLowerCase(Locale.ROOT);
                productos = productos.stream()
                        .filter(p -> p.getNombre() != null && p.getNombre().toLowerCase(Locale.ROOT).contains(t))
                        .toList();
            }
        } else {
            productos = productoService.buscarPorNombreParcial(texto);
        }

        renderizarPorCategorias(productos);
    }

    private void renderizarPorCategorias(List<Producto> productos) {
        contenido.removeAll();

        Map<String, List<Producto>> porCategoria = productos.stream()
                .collect(Collectors.groupingBy(p ->
                        p.getCategoria() != null && p.getCategoria().getNombre() != null
                                ? p.getCategoria().getNombre()
                                : "Otros"
                ));

        List<String> categoriasOrdenadas = new ArrayList<>(porCategoria.keySet());
        categoriasOrdenadas.sort(String::compareToIgnoreCase);

        for (String nombreCat : categoriasOrdenadas) {
            H3 tituloCat = new H3(nombreCat);
            tituloCat.getStyle().set("margin", "6px 0 0 0");

            FlexLayout grid = new FlexLayout();
            grid.setWidthFull();
            grid.getStyle().set("gap", "14px");
            grid.getStyle().set("flex-wrap", "wrap");
            grid.getStyle().set("align-items", "stretch");

            List<Producto> lista = porCategoria.get(nombreCat).stream()
                    .sorted(Comparator.comparing(Producto::getNombre, Comparator.nullsLast(String::compareToIgnoreCase)))
                    .toList();

            for (Producto p : lista) {
                Component card = crearCardProducto(p);

                card.getElement().getStyle().set("flex", "1 1 320px");
                card.getElement().getStyle().set("max-width", "380px");
                card.getElement().getStyle().set("box-sizing", "border-box");

                grid.add(card);
            }

            contenido.add(tituloCat, grid);
        }

        if (productos.isEmpty()) {
            Span vacio = new Span("No hay productos con esos filtros.");
            vacio.getStyle().set("color", "var(--lumo-secondary-text-color)");
            contenido.add(vacio);
        }
    }

    private Component crearCardProducto(Producto p) {

        Image img = new Image(
                p.getImagenUrl() != null ? p.getImagenUrl() : "/images/productos/placeholder.png",
                p.getNombre() != null ? p.getNombre() : "Producto"
        );
        img.setWidthFull();
        img.setHeight("250px");


        img.getStyle().set("object-fit", "contain");
        img.getStyle().set("background", "var(--lumo-contrast-5pct)");
        img.getStyle().set("border-radius", "12px");
        img.getStyle().set("padding", "6px");

        Span nombre = new Span(p.getNombre() != null ? p.getNombre() : "-");
        nombre.getStyle().set("font-weight", "800");
        nombre.getStyle().set("font-size", "1.05rem");

        Span desc = new Span(p.getDescripcion() != null ? p.getDescripcion() : "");
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");
        desc.getStyle().set("font-size", "0.95rem");

        Span precio = new Span(p.getPrecio() != null ? (p.getPrecio() + " €") : "-");
        precio.getStyle().set("font-weight", "800");

        IntegerField qty = new IntegerField();
        qty.setMin(1);
        qty.setValue(1);
        qty.setStepButtonsVisible(true);
        qty.setWidth("120px");

        Button add = new Button("➕ Añadir", e -> {
            try {
                int cantidad = qty.getValue() != null ? qty.getValue() : 1;
                carrito = pedidoService.agregarProductoEnMemoria(carrito, p, cantidad);
                refrescarCarrito();
                Notification.show("Añadido: " + (p.getNombre() != null ? p.getNombre() : p.getCodigo()),
                        1500, Notification.Position.BOTTOM_START);
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
            }
        });


        add.getElement().getStyle().set("height", "32px");
        add.getElement().getStyle().set("min-height", "32px");
        add.getElement().getStyle().set("padding", "0 12px");
        add.getElement().getStyle().set("border-radius", "8px");

        add.getElement().getStyle().set("background", "var(--lumo-success-color)");
        add.getElement().getStyle().set("color", "var(--lumo-success-contrast-color)");
        add.getElement().getStyle().set("border", "1px solid var(--lumo-success-color)");

        add.getElement().getStyle().set("font-weight", "700");
        add.getElement().getStyle().set("font-size", "var(--lumo-font-size-s)");
        add.getElement().getStyle().set("white-space", "nowrap");
        add.getElement().getStyle().set("cursor", "pointer");

        add.setWidth("120px");

        HorizontalLayout acciones = new HorizontalLayout(qty, add);
        acciones.setWidthFull();
        acciones.setSpacing(false);
        acciones.getStyle().set("gap", "10px");
        acciones.setAlignItems(Alignment.END);
        acciones.setFlexGrow(0, qty);
        acciones.setFlexGrow(1, add);
        acciones.getElement().getStyle().set("min-width", "0");

        VerticalLayout card = new VerticalLayout(img, nombre, desc, precio, acciones);
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle().set("gap", "8px");

        card.setWidthFull();
        card.getStyle().set("min-height", "320px");

        card.getStyle().set("overflow", "hidden");
        card.getStyle().set("box-sizing", "border-box");

        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

        return card;
    }

    private void configurarGridCarrito() {
        gridCarrito.setWidthFull();
        gridCarrito.setHeight("380px");
        gridCarrito.getStyle().set("border-radius", "10px");
        gridCarrito.getStyle().set("overflow", "hidden");
        gridCarrito.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        gridCarrito.addColumn(lp -> lp.getProducto() != null ? lp.getProducto().getNombre() : "-")
                .setHeader("Producto")
                .setFlexGrow(1);

        gridCarrito.addColumn(LineaPedido::getCantidad)
                .setHeader("Cant.")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridCarrito.addColumn(lp -> lp.calcularPrecio() + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true)
                .setFlexGrow(0);

        colAcciones = gridCarrito.addComponentColumn(this::accionesCarritoPorFila)
                .setHeader("Acciones")
                .setAutoWidth(true)
                .setFlexGrow(0);

        colAcciones.setVisible(editarCarrito);

        total.getStyle().set("font-weight", "800");
    }

    private Component accionesCarritoPorFila(LineaPedido lp) {
        if (!editarCarrito) {
            return new Span("");
        }

        Button minus = new Button("–", e -> cambiarCantidad(lp, -1));
        Button plus = new Button("+", e -> cambiarCantidad(lp, +1));
        Button trash = new Button("🗑", e -> eliminarLinea(lp));

        minus.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        plus.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        trash.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        minus.getStyle().set("padding", "0 10px");
        plus.getStyle().set("padding", "0 10px");
        trash.getStyle().set("padding", "0 10px");

        HorizontalLayout acciones = new HorizontalLayout(minus, plus, trash);
        acciones.setSpacing(false);
        acciones.getStyle().set("gap", "8px");
        acciones.setAlignItems(Alignment.CENTER);

        return acciones;
    }

    private void cambiarCantidad(LineaPedido lp, int delta) {
        try {
            int actual = lp.getCantidad();
            int nueva = actual + delta;

            carrito = pedidoService.actualizarCantidadEnMemoria(
                    carrito,
                    lp.getProducto().getCodigo(),
                    nueva
            );

            refrescarCarrito();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
        }
    }

    private void eliminarLinea(LineaPedido lp) {
        try {
            carrito = pedidoService.eliminarProductoEnMemoria(
                    carrito,
                    lp.getProducto().getCodigo()
            );
            refrescarCarrito();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
        }
    }

    protected void refrescarCarrito() {
        List<LineaPedido> items =
                carrito != null && carrito.getLineaPedidos() != null ? carrito.getLineaPedidos() : List.of();

        gridCarrito.setItems(items);

        BigDecimal totalCalc = BigDecimal.ZERO;
        if (!items.isEmpty()) {
            totalCalc = carrito.calcularPrecioTotal();
        }
        total.setText("Total: " + totalCalc + " €");

        continuar.setEnabled(puedeContinuar());
    }

    protected String usernameActual() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
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

    protected abstract Component construirBloqueDetalles();
    protected abstract boolean puedeContinuar();
    protected abstract void onContinuar();
}