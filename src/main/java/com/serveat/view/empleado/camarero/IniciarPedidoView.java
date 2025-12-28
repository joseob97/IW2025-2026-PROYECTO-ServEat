package com.serveat.view.empleado.camarero;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Iniciar Pedido | Camarero")
@Route(value = "empleado/camarero/pedidos/nuevo", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class IniciarPedidoView extends VerticalLayout {

    private final transient PedidoService pedidoService;
    private final transient PedidoCarritoService carritoService;
    private final transient PedidoCalculoService calculoService;

    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;

    private transient Pedido pedidoActual;
    private transient Pedido carrito = new Pedido();

    private final Map<String, Boolean> cacheTieneIngredientes = new HashMap<>();

    private final ComboBox<String> filtroCategoria = new ComboBox<>("Categoría");
    private final TextField buscador = new TextField("Buscar");

    private final VerticalLayout contenido = new VerticalLayout();

    private final Grid<LineaPedido> gridCarrito = new Grid<>(LineaPedido.class, false);
    private final Span total = new Span("Total: 0 €");

    private final IntegerField mesa = new IntegerField("Número de mesa");
    private final TextField codigo = new TextField("Código pedido");
    private final Button crearPedido = new Button("Crear pedido");

    private final Button confirmar = new Button("✅ Confirmar pedido (Enviar a cocina)");

    private boolean editarCarrito = false;
    private Grid.Column<LineaPedido> colAcciones;
    private final Button btnEditarCarrito = new Button("✏️ Editar carrito");

    public IniciarPedidoView(PedidoService pedidoService,
                             PedidoCarritoService carritoService,
                             PedidoCalculoService calculoService,
                             ProductoService productoService,
                             CategoriaService categoriaService) {
        this.pedidoService = pedidoService;
        this.carritoService = carritoService;
        this.calculoService = calculoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setSpacing(false);
        setPadding(true);
        setWidthFull();
        getStyle().set("gap", "16px");
        getStyle().set("max-width", "1280px");
        getStyle().set("margin", "0 auto");

        if (carrito.getLineaPedidos() == null) {
            carrito.setLineaPedidos(new LinkedHashSet<>());
        }

        H3 titulo = new H3("Iniciar pedido de mesa");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        add(crearBloqueMesa());
        add(crearBloqueCartaYCarrito());

        cargarProductos();
        refrescarCarrito();

        setUiPedidoCreado(false);
    }

    private Component crearBloqueMesa() {
        VerticalLayout card = crearCard();
        card.getStyle().set("gap", "14px");

        mesa.setMin(1);
        mesa.setStepButtonsVisible(true);
        mesa.setWidth("260px");

        crearPedido.setWidth("260px");
        crearPedido.getStyle().set("font-weight", "700");
        crearPedido.addClickListener(e -> crearPedidoMesa());

        codigo.setReadOnly(true);
        codigo.setWidth("320px");

        VerticalLayout bloqueMesa = new VerticalLayout(mesa, crearPedido);
        bloqueMesa.setPadding(false);
        bloqueMesa.setSpacing(false);
        bloqueMesa.getStyle().set("gap", "10px");
        bloqueMesa.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout fila = new HorizontalLayout(bloqueMesa, codigo);
        fila.setWidthFull();
        fila.setSpacing(true);
        fila.getStyle().set("gap", "18px");
        fila.setAlignItems(FlexComponent.Alignment.END);

        card.add(fila);
        return card;
    }

    private Component crearBloqueCartaYCarrito() {
        configurarFiltros();

        HorizontalLayout filtros = new HorizontalLayout(filtroCategoria, buscador);
        filtros.setWidthFull();
        filtros.setSpacing(false);
        filtros.getStyle().set("gap", "12px");
        filtroCategoria.setWidth("260px");
        buscador.setWidth("360px");

        HorizontalLayout main = new HorizontalLayout();
        main.setWidthFull();
        main.setSpacing(false);
        main.getStyle().set("gap", "16px");

        VerticalLayout izquierda = crearCard();
        izquierda.setWidthFull();
        izquierda.getStyle().set("flex", "2");
        izquierda.getStyle().set("gap", "12px");

        contenido.setWidthFull();
        contenido.setPadding(false);
        contenido.setSpacing(false);
        contenido.getStyle().set("gap", "18px");

        izquierda.add(new H3("Carta"), filtros, contenido);

        VerticalLayout derecha = new VerticalLayout();
        derecha.setPadding(false);
        derecha.setSpacing(false);
        derecha.getStyle().set("gap", "14px");
        derecha.getStyle().set("flex", "1");
        derecha.setWidth("520px");

        VerticalLayout cardCarrito = crearCard();
        cardCarrito.getStyle().set("gap", "12px");

        H3 hCarrito = new H3("Carrito");
        hCarrito.getStyle().set("margin", "0");

        btnEditarCarrito.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnEditarCarrito.getStyle().set("font-weight", "700");

        HorizontalLayout filaTop = new HorizontalLayout(hCarrito);
        filaTop.setWidthFull();
        filaTop.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout filaEditar = new HorizontalLayout(btnEditarCarrito);
        filaEditar.setWidthFull();
        filaEditar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        btnEditarCarrito.addClickListener(e -> {
            editarCarrito = !editarCarrito;
            btnEditarCarrito.setText(editarCarrito ? "✅ Listo" : "✏️ Editar carrito");
            if (colAcciones != null) colAcciones.setVisible(editarCarrito);
            gridCarrito.getDataProvider().refreshAll();
        });

        configurarGridCarrito();
        cardCarrito.add(filaTop, filaEditar, gridCarrito, total);

        VerticalLayout cardConfirmar = crearCard();
        cardConfirmar.getStyle().set("gap", "10px");

        confirmar.setWidthFull();
        confirmar.getStyle().set("font-weight", "700");
        confirmar.addClickListener(e -> confirmarPedido());

        cardConfirmar.add(new H3("Acción"), confirmar);

        derecha.add(cardCarrito, cardConfirmar);

        main.add(izquierda, derecha);
        main.setFlexGrow(2, izquierda);
        main.setFlexGrow(1, derecha);

        return main;
    }

    private void configurarFiltros() {
        filtroCategoria.setItems(
                categoriaService.listarCategorias().stream()
                        .map(Categoria::getNombre)
                        .filter(Objects::nonNull)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList()
        );
        filtroCategoria.setClearButtonVisible(true);
        filtroCategoria.addValueChangeListener(e -> cargarProductos());

        buscador.setPlaceholder("Buscar por nombre...");
        buscador.setClearButtonVisible(true);
        buscador.setValueChangeMode(ValueChangeMode.EAGER);
        buscador.addValueChangeListener(e -> cargarProductos());
    }

    private void cargarProductos() {
        String categoria = filtroCategoria.getValue();
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
        for (Producto p : productos) {
            String cod = p.getCodigo();
            if (cod != null && !cod.isBlank()) {
                cacheTieneIngredientes.put(cod, productoService.productoTieneIngredientes(cod));
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
            if (!hayPedidoCreado()) return;

            try {
                int cantidad = qty.getValue() != null ? qty.getValue() : 1;
                carrito = carritoService.agregarProducto(carrito, p, cantidad);
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

        boolean tieneIng = p.getCodigo() != null && cacheTieneIngredientes.getOrDefault(p.getCodigo(), false);

        Button personalizar = new Button("⚙ Personalizar");
        personalizar.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        personalizar.getElement().getStyle().set("height", "26px");
        personalizar.getElement().getStyle().set("min-height", "26px");
        personalizar.getElement().getStyle().set("padding", "0 10px");
        personalizar.getElement().getStyle().set("border-radius", "8px");
        personalizar.getElement().getStyle().set("font-size", "var(--lumo-font-size-xs)");
        personalizar.getElement().getStyle().set("font-weight", "700");

        personalizar.setEnabled(hayPedidoCreado() && tieneIng);

        if (!tieneIng) {
            personalizar.getElement().setProperty("title", "Este producto no tiene ingredientes configurados");
        }

        personalizar.addClickListener(e -> {
            if (!hayPedidoCreado()) return;
            abrirDialogoPersonalizar(p.getCodigo(), qty);
        });

        HorizontalLayout filaPersonalizar = new HorizontalLayout(personalizar);
        filaPersonalizar.setWidthFull();
        filaPersonalizar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

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
        card.getStyle().set("box-sizing", "border-box");
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

        return card;
    }

    private void abrirDialogoPersonalizar(String codigoProducto, IntegerField qtyField) {
        if (codigoProducto == null || codigoProducto.isBlank()) return;

        try {
            Producto producto = productoService.obtenerConIngredientesPorCodigo(codigoProducto);
            List<ProductoIngrediente> receta = (producto.getIngredientes() == null) ? List.of() : producto.getIngredientes();

            if (receta.isEmpty()) {
                Notification.show("Este producto no tiene ingredientes configurados", 2500, Notification.Position.MIDDLE);
                return;
            }

            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Personalizar: " + (producto.getNombre() != null ? producto.getNombre() : producto.getCodigo()));

            FormLayout form = new FormLayout();
            form.setWidth("600px");
            form.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0", 1)
            );

            Map<UUID, Checkbox> quitarChecks = new LinkedHashMap<>();
            Map<UUID, IntegerField> extras = new LinkedHashMap<>();
            Map<UUID, Runnable> stylers = new LinkedHashMap<>();

            for (ProductoIngrediente pi : receta) {
                Ingrediente ing = pi.getIngrediente();
                UUID ingId = (ing != null) ? ing.getId() : null;
                if (ingId == null) continue;

                String nombreIng = (ing.getNombre() != null) ? ing.getNombre() : "Ingrediente";

                BigDecimal precioExtra = (pi.getPrecioExtra() != null) ? pi.getPrecioExtra()
                        : (ing.getPrecioExtra() != null ? ing.getPrecioExtra() : BigDecimal.ZERO);
                String precioExtraTxt = precioExtra.setScale(2, RoundingMode.HALF_UP).toPlainString();

                boolean incluidoInicial = pi.isPorDefecto();

                Span nombre = new Span(nombreIng);
                nombre.getStyle().set("font-weight", "800");

                Span badge = new Span();
                badge.getStyle().set("font-size", "var(--lumo-font-size-xs)");
                badge.getStyle().set("font-weight", "800");
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

                HorizontalLayout row = new HorizontalLayout(nombre, badge, quitar, extraQty);
                row.setWidthFull();
                row.setAlignItems(Alignment.CENTER);
                row.expand(nombre);
                row.getStyle().set("gap", "12px");

                Runnable aplicarEstilo = () -> {
                    boolean incluido = !quitar.getValue();
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
                        badge.setText("SIN");
                        badge.getStyle().set("background", "var(--lumo-error-50pct)");
                        badge.getStyle().set("color", "var(--lumo-base-color)");
                        nombre.getStyle().set("text-decoration", "line-through");
                        nombre.getStyle().set("opacity", "0.85");
                    }
                    row.getStyle().set("border-radius", "10px");
                    row.getStyle().set("padding", "8px 10px");
                };
                aplicarEstilo.run();

                if (!pi.isOpcional()) {
                    quitar.setEnabled(false);
                    extraQty.setEnabled(false);
                } else {
                    quitar.addValueChangeListener(ev -> {
                        if (!ev.isFromClient()) return;
                        aplicarEstilo.run();
                    });
                }

                quitarChecks.put(ingId, quitar);
                extras.put(ingId, extraQty);
                stylers.put(ingId, aplicarEstilo);

                form.add(row);
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

                    carrito = carritoService.agregarProductoPersonalizado(
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
            acciones.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

            VerticalLayout content = new VerticalLayout(form, acciones);
            content.setPadding(false);
            content.setSpacing(true);

            dialog.add(content);
            dialog.open();

        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
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

        gridCarrito.addColumn(lp -> calculoService.calcularPrecioLinea(lp) + " €")
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

        List<String> detalles = construirDetallePersonalizacion(lp);
        for (String d : detalles) {
            Span line = new Span(d);
            line.getStyle().set("font-size", "var(--lumo-font-size-xs)");
            line.getStyle().set("color", "var(--lumo-secondary-text-color)");
            box.add(line);
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
            int nueva = lp.getCantidad() + delta;
            carrito = carritoService.actualizarCantidadLinea(carrito, lp.getCodigo(), nueva);
            refrescarCarrito();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
        }
    }

    private void eliminarLinea(LineaPedido lp) {
        try {
            carrito = carritoService.eliminarLinea(carrito, lp.getCodigo());
            refrescarCarrito();
        } catch (Exception ex) {
            Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
        }
    }

    private void crearPedidoMesa() {
        Integer nMesa = mesa.getValue();
        if (nMesa == null || nMesa <= 0) {
            Notification.show("Mesa inválida", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoActual = pedidoService.crearPedidoMesa(nMesa);
            codigo.setValue(pedidoActual.getCodigo());

            setUiPedidoCreado(true);

            refrescarCarrito();
            Notification.show("Pedido creado: " + pedidoActual.getCodigo(), 3000, Notification.Position.MIDDLE);
        } catch (Exception ex) {
            Notification.show("Error creando pedido: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarPedido() {
        if (!hayPedidoCreado()) return;

        if (carrito == null || carrito.getLineaPedidos() == null || carrito.getLineaPedidos().isEmpty()) {
            Notification.show("El pedido no puede estar vacío", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoService.volcarCarritoEnPedido(pedidoActual.getCodigo(), carrito);
            pedidoService.confirmarPedido(pedidoActual.getCodigo());

            Notification.show("Pedido enviado a cocina", 3000, Notification.Position.MIDDLE);
            setUiPedidoConfirmado();
        } catch (Exception ex) {
            Notification.show("Error confirmando: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void refrescarCarrito() {
        List<LineaPedido> items =
                carrito != null && carrito.getLineaPedidos() != null
                        ? new ArrayList<>(carrito.getLineaPedidos())
                        : List.of();

        gridCarrito.setItems(items);

        BigDecimal totalCalc = BigDecimal.ZERO;
        if (!items.isEmpty()) {
            totalCalc = calculoService.calcularTotalPedido(carrito);
        }

        total.setText("Total: " + totalCalc + " €");
        confirmar.setEnabled(hayPedidoCreado() && !items.isEmpty());
    }

    private boolean hayPedidoCreado() {
        return pedidoActual != null;
    }

    private void setUiPedidoCreado(boolean creado) {
        filtroCategoria.setEnabled(creado);
        buscador.setEnabled(creado);
        btnEditarCarrito.setEnabled(creado);
        gridCarrito.setEnabled(creado);

        confirmar.setEnabled(creado
                && carrito != null
                && carrito.getLineaPedidos() != null
                && !carrito.getLineaPedidos().isEmpty());

        cargarProductos();
    }

    private void setUiPedidoConfirmado() {
        filtroCategoria.setEnabled(false);
        buscador.setEnabled(false);
        btnEditarCarrito.setEnabled(false);
        gridCarrito.setEnabled(false);
        confirmar.setEnabled(false);
        crearPedido.setEnabled(false);
        mesa.setEnabled(false);

        cargarProductos();
    }

    private VerticalLayout crearCard() {
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
}