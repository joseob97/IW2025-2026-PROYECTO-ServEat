package com.serveat.view.cliente.pedido;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public abstract class CrearPedidoCartaBaseView extends VerticalLayout {

    protected final transient PedidoService pedidoService;

    protected final transient PedidoCarritoService pedidoCarritoService;
    protected final transient PedidoCalculoService pedidoCalculoService;

    protected final transient ProductoService productoService;
    protected final transient CategoriaService categoriaService;
    protected final transient FeatureService featureService;

    protected Pedido carrito = new Pedido();

    private final Map<String, Boolean> cacheTieneIngredientes = new HashMap<>();

    protected final ComboBox<String> categoriaFiltro = new ComboBox<>("Categoría");
    protected final TextField buscador = new TextField("Buscar");

    protected final VerticalLayout contenido = new VerticalLayout();

    protected final Grid<LineaPedido> gridCarrito = new Grid<>(LineaPedido.class, false);
    protected final Span total = new Span("Total: 0 €");

    protected final Button continuar = new Button("➡ Continuar");

    private boolean editarCarrito = false;
    private Grid.Column<LineaPedido> colAcciones;
    private final Button btnEditarCarrito = new Button("✏️ Editar carrito");

    private boolean showIngredientes = false;

    protected CrearPedidoCartaBaseView(PedidoService pedidoService,
                                       PedidoCarritoService pedidoCarritoService,
                                       PedidoCalculoService pedidoCalculoService,
                                       ProductoService productoService,
                                       CategoriaService categoriaService,
                                       FeatureService featureService) {
        this.pedidoService = pedidoService;
        this.pedidoCarritoService = pedidoCarritoService;
        this.pedidoCalculoService = pedidoCalculoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.featureService = featureService;

        setWidthFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1280px");
        getStyle().set("margin", "0 auto");

        if (carrito.getLineaPedidos() == null) {
            carrito.setLineaPedidos(new LinkedHashSet<>());
        }
    }

    protected void construirUI(String tituloPantalla) {

        showIngredientes = featureService.tieneFeature(Feature.INGREDIENTES);

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
            if (colAcciones != null) colAcciones.setVisible(editarCarrito);
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
        String texto = (buscador.getValue() != null) ? buscador.getValue().trim() : "";

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

        cacheTieneIngredientes.clear();
        if (showIngredientes) {
            for (Producto p : productos) {
                String cod = p.getCodigo();
                if (cod != null && !cod.isBlank()) {
                    cacheTieneIngredientes.put(cod, productoService.productoTieneIngredientes(cod));
                }
            }
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
        qty.setWidth("110px");

        Button add = new Button("➕ Añadir", e -> {
            try {
                int cantidad = (qty.getValue() != null) ? qty.getValue() : 1;
                carrito = pedidoCarritoService.agregarProducto(carrito, p, cantidad);
                refrescarCarrito();
                Notification.show("Añadido: " + (p.getNombre() != null ? p.getNombre() : p.getCodigo()),
                        1500, Notification.Position.BOTTOM_START);
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
            }
        });

        add.getElement().getStyle().set("height", "32px");
        add.getElement().getStyle().set("border-radius", "8px");
        add.getElement().getStyle().set("font-weight", "700");
        add.getElement().getStyle().set("background", "var(--lumo-success-color)");
        add.getElement().getStyle().set("color", "var(--lumo-success-contrast-color)");
        add.getElement().getStyle().set("border", "1px solid var(--lumo-success-color)");
        add.setWidth("120px");

        boolean tieneIng = showIngredientes && p.getCodigo() != null && cacheTieneIngredientes.getOrDefault(p.getCodigo(), false);

        Button personalizar = new Button("⚙ Personalizar");
        personalizar.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        personalizar.getElement().getStyle().set("height", "26px");
        personalizar.getElement().getStyle().set("min-height", "26px");
        personalizar.getElement().getStyle().set("padding", "0 10px");
        personalizar.getElement().getStyle().set("border-radius", "8px");
        personalizar.getElement().getStyle().set("font-size", "var(--lumo-font-size-xs)");
        personalizar.getElement().getStyle().set("font-weight", "700");

        personalizar.setEnabled(tieneIng);

        if (!showIngredientes) {
            personalizar.getElement().setProperty("title", "Personalización desactivada");
        } else if (!tieneIng) {
            personalizar.getElement().setProperty("title", "Este producto no tiene ingredientes configurados");
        }

        personalizar.addClickListener(e -> {
            if (!showIngredientes) return;
            abrirDialogoPersonalizar(p.getCodigo(), qty);
        });

        HorizontalLayout filaPersonalizar = new HorizontalLayout(personalizar);
        filaPersonalizar.setWidthFull();
        filaPersonalizar.setJustifyContentMode(JustifyContentMode.END);

        HorizontalLayout acciones = new HorizontalLayout(qty, add);
        acciones.setWidthFull();
        acciones.setSpacing(false);
        acciones.getStyle().set("gap", "10px");
        acciones.setAlignItems(Alignment.END);

        VerticalLayout card = new VerticalLayout(img, nombre, desc, precio, filaPersonalizar, acciones);
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle().set("gap", "8px");
        card.setWidthFull();
        card.getStyle().set("min-height", "340px");
        card.getStyle().set("overflow", "hidden");
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

        return card;
    }

    // Cada sección tiene SU FormLayout
    // + deduplicamos por ingredienteId para que no salgan repetidos.
    private void abrirDialogoPersonalizar(String codigoProducto, IntegerField qtyField) {
        if (!showIngredientes) return;
        if (codigoProducto == null || codigoProducto.isBlank()) return;

        try {
            Producto producto = productoService.obtenerConIngredientesPorCodigo(codigoProducto);
            List<ProductoIngrediente> recetaRaw = (producto.getIngredientes() == null) ? List.of() : producto.getIngredientes();

            if (recetaRaw.isEmpty()) {
                Notification.show("Este producto no tiene ingredientes configurados", 2500, Notification.Position.MIDDLE);
                return;
            }

            Map<UUID, ProductoIngrediente> unique = new LinkedHashMap<>();
            for (ProductoIngrediente pi : recetaRaw) {
                if (pi == null || pi.getIngrediente() == null || pi.getIngrediente().getId() == null) continue;
                unique.putIfAbsent(pi.getIngrediente().getId(), pi);
            }
            List<ProductoIngrediente> receta = new ArrayList<>(unique.values());

            Dialog dialog = new Dialog();
            dialog.setWidth("860px");
            dialog.setHeaderTitle("Personalizar: " + (producto.getNombre() != null ? producto.getNombre() : producto.getCodigo()));

            VerticalLayout content = new VerticalLayout();
            content.setPadding(false);
            content.setSpacing(false);
            content.getStyle().set("gap", "12px");

            List<ProductoIngrediente> porDefecto = receta.stream()
                    .filter(ProductoIngrediente::isPorDefecto)
                    .sorted(Comparator.comparing(pi -> safe(pi.getIngrediente().getNombre()), String.CASE_INSENSITIVE_ORDER))
                    .toList();

            List<ProductoIngrediente> noPorDefecto = receta.stream()
                    .filter(pi -> !pi.isPorDefecto())
                    .sorted(Comparator.comparing(pi -> safe(pi.getIngrediente().getNombre()), String.CASE_INSENSITIVE_ORDER))
                    .toList();

            Map<UUID, Checkbox> quitarChecks = new LinkedHashMap<>();
            Map<UUID, IntegerField> extras = new LinkedHashMap<>();

            if (!porDefecto.isEmpty()) {
                H3 h = new H3("Por defecto");
                h.getStyle().set("margin", "0");
                h.getStyle().set("font-size", "var(--lumo-font-size-m)");

                FormLayout formDef = new FormLayout();
                formDef.setWidth("820px");
                formDef.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

                for (ProductoIngrediente pi : porDefecto) {
                    formDef.add(crearFilaIngredientePersonalizar(pi, quitarChecks, extras));
                }

                content.add(h, formDef);
            }

            if (!noPorDefecto.isEmpty()) {
                H3 h = new H3("No por defecto");
                h.getStyle().set("margin", porDefecto.isEmpty() ? "0" : "14px 0 0 0");
                h.getStyle().set("font-size", "var(--lumo-font-size-m)");

                FormLayout formNoDef = new FormLayout();
                formNoDef.setWidth("820px");
                formNoDef.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

                for (ProductoIngrediente pi : noPorDefecto) {
                    formNoDef.add(crearFilaIngredientePersonalizar(pi, quitarChecks, extras));
                }

                content.add(h, formNoDef);
            }

            Button cancelar = new Button("Cancelar", e -> dialog.close());
            Button anadir = new Button("✅ Añadir personalizado");
            anadir.getStyle().set("font-weight", "700");

            anadir.addClickListener(e -> {
                try {
                    int cantidad = (qtyField.getValue() != null) ? qtyField.getValue() : 1;

                    Map<UUID, Boolean> incluidoMap = new HashMap<>();
                    Map<UUID, Integer> extraMap = new HashMap<>();

                    for (var entry : quitarChecks.entrySet()) {
                        boolean incluido = !Boolean.TRUE.equals(entry.getValue().getValue());
                        incluidoMap.put(entry.getKey(), incluido);
                    }
                    for (var entry : extras.entrySet()) {
                        Integer v = entry.getValue().getValue();
                        extraMap.put(entry.getKey(), (v == null) ? 0 : Math.max(v, 0));
                    }

                    carrito = pedidoCarritoService.agregarProductoPersonalizado(
                            carrito,
                            codigoProducto,
                            cantidad,
                            incluidoMap,
                            extraMap
                    );

                    refrescarCarrito();
                    dialog.close();

                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });

            HorizontalLayout acciones = new HorizontalLayout(cancelar, anadir);
            acciones.setWidthFull();
            acciones.setJustifyContentMode(JustifyContentMode.END);

            VerticalLayout wrapper = new VerticalLayout(content, acciones);
            wrapper.setPadding(false);
            wrapper.setSpacing(true);

            dialog.add(wrapper);
            dialog.open();

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private Component crearFilaIngredientePersonalizar(ProductoIngrediente pi,
                                                       Map<UUID, Checkbox> quitarChecks,
                                                       Map<UUID, IntegerField> extras) {

        Ingrediente ing = pi.getIngrediente();
        UUID ingId = (ing != null) ? ing.getId() : null;
        if (ingId == null) return new Span("");

        boolean esPorDefecto = pi.isPorDefecto();
        boolean esOpcional = pi.isOpcional();

        BigDecimal precioExtra = (pi.getPrecioExtra() != null) ? pi.getPrecioExtra()
                : (ing.getPrecioExtra() != null ? ing.getPrecioExtra() : BigDecimal.ZERO);
        String precioExtraTxt = precioExtra.setScale(2, RoundingMode.HALF_UP).toPlainString();

        boolean incluidoInicial = esPorDefecto;

        Span nombre = new Span(ing.getNombre() != null ? ing.getNombre() : "Ingrediente");
        nombre.getStyle().set("font-weight", "900");

        Span sub = new Span(esPorDefecto ? "Por defecto" : "No por defecto");
        sub.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        sub.getStyle().set("color", "var(--lumo-secondary-text-color)");

        VerticalLayout nombreBox = new VerticalLayout(nombre, sub);
        nombreBox.setPadding(false);
        nombreBox.setSpacing(false);
        nombreBox.getStyle().set("gap", "2px");

        Span badge = new Span();
        badge.getStyle().set("font-size", "var(--lumo-font-size-xs)");
        badge.getStyle().set("font-weight", "900");
        badge.getStyle().set("padding", "4px 10px");
        badge.getStyle().set("border-radius", "999px");

        Checkbox quitar = new Checkbox("Quitar");
        quitar.setValue(!incluidoInicial);

        IntegerField extraQty = new IntegerField();
        extraQty.setLabel("Extra\n+ " + precioExtraTxt + " € / extra");
        extraQty.setMin(0);
        extraQty.setValue(0);
        extraQty.setStepButtonsVisible(true);
        extraQty.setWidth("220px");

        HorizontalLayout row = new HorizontalLayout(nombreBox, badge, quitar, extraQty);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.expand(nombreBox);
        row.getStyle().set("gap", "12px");

        Runnable aplicarEstiloYEstado = () -> {
            boolean incluido = !Boolean.TRUE.equals(quitar.getValue());

            if (incluido) {
                row.getStyle().set("background", "var(--lumo-success-10pct)");
                row.getStyle().set("border", "1px solid var(--lumo-success-50pct)");
                badge.setText("INCLUIDO");
                badge.getStyle().set("background", "var(--lumo-success-50pct)");
                badge.getStyle().set("color", "var(--lumo-base-color)");
                nombre.getStyle().remove("text-decoration");
                nombre.getStyle().remove("opacity");
            } else {
                row.getStyle().set("background", "var(--lumo-error-10pct)");
                row.getStyle().set("border", "1px solid var(--lumo-error-50pct)");
                badge.setText("QUITADO");
                badge.getStyle().set("background", "var(--lumo-error-50pct)");
                badge.getStyle().set("color", "var(--lumo-base-color)");
                nombre.getStyle().set("text-decoration", "line-through");
                nombre.getStyle().set("opacity", "0.85");
            }

            row.getStyle().set("border-radius", "12px");
            row.getStyle().set("padding", "10px 12px");

            boolean incluidoNow = !Boolean.TRUE.equals(quitar.getValue());
            if (!incluidoNow) {
                if (!Objects.equals(extraQty.getValue(), 0)) extraQty.setValue(0);
                extraQty.setEnabled(false);
            } else {
                extraQty.setEnabled(esOpcional);
            }
        };

        aplicarEstiloYEstado.run();

        if (!esOpcional) {
            quitar.setEnabled(false);
            extraQty.setEnabled(false);
        } else {
            quitar.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;
                aplicarEstiloYEstado.run();
            });

            extraQty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;

                int v = valueOrZero(ev.getValue());
                if (!Objects.equals(extraQty.getValue(), v)) extraQty.setValue(v);

                if (Boolean.TRUE.equals(quitar.getValue())) quitar.setValue(false);

                aplicarEstiloYEstado.run();
            });
        }

        quitarChecks.put(ingId, quitar);
        extras.put(ingId, extraQty);

        return row;
    }

    private void configurarGridCarrito() {
        gridCarrito.setWidthFull();
        gridCarrito.setHeight("380px");
        gridCarrito.getStyle().set("border-radius", "10px");
        gridCarrito.getStyle().set("overflow", "hidden");
        gridCarrito.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        gridCarrito.addComponentColumn(this::celdaProductoConPersonalizacion)
                .setHeader("Producto")
                .setFlexGrow(1);

        gridCarrito.addColumn(LineaPedido::getCantidad)
                .setHeader("Cant.")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridCarrito.addColumn(lp -> pedidoCalculoService.calcularPrecioLinea(lp) + " €")
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

    private Component celdaProductoConPersonalizacion(LineaPedido lp) {
        String nombre = (lp.getProducto() != null && lp.getProducto().getNombre() != null)
                ? lp.getProducto().getNombre()
                : "-";

        Span titulo = new Span(nombre);
        titulo.getStyle().set("font-weight", "800");

        VerticalLayout box = new VerticalLayout(titulo);
        box.setPadding(false);
        box.setSpacing(false);
        box.getStyle().set("gap", "4px");

        if (showIngredientes) {
            List<String> detalles = construirDetallePersonalizacion(lp);
            for (String d : detalles) {
                Span chip = new Span(d);
                chip.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                chip.getStyle().set("color", "var(--lumo-secondary-text-color)");
                box.add(chip);
            }
        }

        return box;
    }

    private List<String> construirDetallePersonalizacion(LineaPedido lp) {
        Collection<LineaPedidoIngrediente> ingsCol =
                (lp.getIngredientes() == null) ? List.of() : lp.getIngredientes();

        if (ingsCol.isEmpty()) return List.of();

        List<LineaPedidoIngrediente> ings = new ArrayList<>(ingsCol);

        List<String> res = new ArrayList<>();

        for (LineaPedidoIngrediente li : ings) {
            if (li == null || li.getIngrediente() == null) continue;
            String n = li.getIngrediente().getNombre();
            if (n == null || n.isBlank()) continue;

            if (!li.isIncluido()) {
                res.add("Sin " + n);
            }
        }

        for (LineaPedidoIngrediente li : ings) {
            if (li == null || li.getIngrediente() == null) continue;

            int nExtra = Math.max(li.getExtraCantidad(), 0);
            if (nExtra <= 0) continue;

            String n = li.getIngrediente().getNombre();
            if (n == null || n.isBlank()) n = "Ingrediente";

            BigDecimal unit = (li.getPrecioExtra() == null) ? BigDecimal.ZERO : li.getPrecioExtra();
            BigDecimal plus = unit.multiply(BigDecimal.valueOf(nExtra));

            res.add("Extra " + n + " x" + nExtra + " (+" + plus + " €)");
        }

        return res;
    }

    private Component accionesCarritoPorFila(LineaPedido lp) {
        if (!editarCarrito) return new Span("");

        Button minus = new Button("–", e -> cambiarCantidadLinea(lp, -1));
        Button plus = new Button("+", e -> cambiarCantidadLinea(lp, +1));
        Button trash = new Button("🗑", e -> eliminarLinea(lp));

        minus.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        plus.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        trash.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout acciones = new HorizontalLayout(minus, plus, trash);
        acciones.setSpacing(false);
        acciones.getStyle().set("gap", "8px");
        acciones.setAlignItems(Alignment.CENTER);
        return acciones;
    }

    private void cambiarCantidadLinea(LineaPedido lp, int delta) {
        try {
            if (lp == null || lp.getCodigo() == null || lp.getCodigo().isBlank()) {
                throw new IllegalArgumentException("Línea inválida");
            }

            int nueva = lp.getCantidad() + delta;
            carrito = pedidoCarritoService.actualizarCantidadLinea(carrito, lp.getCodigo(), nueva);
            refrescarCarrito();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
        }
    }

    private void eliminarLinea(LineaPedido lp) {
        try {
            if (lp == null || lp.getCodigo() == null || lp.getCodigo().isBlank()) {
                throw new IllegalArgumentException("Línea inválida");
            }

            carrito = pedidoCarritoService.eliminarLinea(carrito, lp.getCodigo());
            refrescarCarrito();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
        }
    }

    protected void refrescarCarrito() {
        List<LineaPedido> items =
                (carrito != null && carrito.getLineaPedidos() != null)
                        ? new ArrayList<>(carrito.getLineaPedidos())
                        : List.of();

        gridCarrito.setItems(items);

        BigDecimal totalCalc = items.isEmpty()
                ? BigDecimal.ZERO
                : pedidoCalculoService.calcularTotalPedido(carrito);

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

    private String safe(String s) {
        return (s == null) ? "-" : s;
    }

    private int valueOrZero(Integer v) {
        return v == null ? 0 : Math.max(0, v);
    }
}