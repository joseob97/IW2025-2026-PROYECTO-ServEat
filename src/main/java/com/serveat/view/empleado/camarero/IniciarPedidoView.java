package com.serveat.view.empleado.camarero;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

@PageTitle("Iniciar Pedido | Camarero")
@Route(value = "empleado/camarero/pedidos/nuevo", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class IniciarPedidoView extends VerticalLayout {

    // SERVICIOS (transient para Sonar/Vaadin)
    private final transient PedidoService pedidoService;
    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;

    // ESTADO DE LA VISTA
    private transient Pedido pedidoActual;

    private final Grid<LineaPedido> grid = new Grid<>(LineaPedido.class, false);
    private final Span total = new Span("Total: 0 €");

    // UI refs
    private final IntegerField mesa = new IntegerField("Número de mesa");
    private final TextField codigo = new TextField("Código pedido");
    private final Button crearPedido = new Button("Crear pedido");

    // FILTROS DE PRODUCTO
    private final TextField buscarProducto = new TextField("Buscar producto");
    private final ComboBox<String> filtroCategoria = new ComboBox<>("Categoría");

    private final ComboBox<Producto> comboProducto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");
    private final Button anadir = new Button("Añadir");

    // Confirmación
    private final Button confirmar = new Button("✅ Confirmar pedido (Enviar a cocina)");

    public IniciarPedidoView(PedidoService pedidoService,
                             ProductoService productoService,
                             CategoriaService categoriaService) {

        this.pedidoService = pedidoService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setSpacing(false);
        setPadding(true);
        setWidthFull();

        // Un poco de “aire” general y ancho cómodo tipo “card”
        getStyle().set("gap", "18px");
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");

        H3 titulo = new H3("Iniciar pedido de mesa");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        // CREAR PEDIDO

        VerticalLayout cardPedido = crearCard();
        cardPedido.getStyle().set("gap", "14px");

        mesa.setMin(1);
        mesa.setStepButtonsVisible(true);
        mesa.setWidth("260px");

        crearPedido.addClickListener(e -> crearPedidoMesa());
        crearPedido.setWidth("260px");

        codigo.setReadOnly(true);
        codigo.setWidth("320px");

        // Centrado y con separación (botón debajo)
        VerticalLayout bloqueMesa = new VerticalLayout(mesa, crearPedido);
        bloqueMesa.setPadding(false);
        bloqueMesa.setSpacing(false);
        bloqueMesa.getStyle().set("gap", "10px");
        bloqueMesa.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout filaPedido = new HorizontalLayout(bloqueMesa, codigo);
        filaPedido.setWidthFull();
        filaPedido.setSpacing(true);
        filaPedido.getStyle().set("gap", "18px");
        filaPedido.setAlignItems(FlexComponent.Alignment.END);
        filaPedido.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        cardPedido.add(filaPedido);
        add(cardPedido);

        // AÑADIR PRODUCTOS

        H3 tituloAdd = new H3("Añadir productos");
        tituloAdd.getStyle().set("margin", "6px 0 0 0");
        add(tituloAdd);

        VerticalLayout cardProductos = crearCard();
        cardProductos.getStyle().set("gap", "14px");

        configurarFiltrosProducto();
        configurarComboProducto();

        buscarProducto.setWidth("360px");
        filtroCategoria.setWidth("260px");

        HorizontalLayout filtros = new HorizontalLayout(buscarProducto, filtroCategoria);
        filtros.setWidthFull();
        filtros.setSpacing(false);
        filtros.getStyle().set("gap", "14px");
        filtros.setAlignItems(FlexComponent.Alignment.END);
        filtros.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        cantidad.setMin(1);
        cantidad.setStepButtonsVisible(true);
        cantidad.setValue(1);
        cantidad.setWidth("160px");

        anadir.addClickListener(e -> anadirProducto());
        anadir.setWidth("420px");

        // “Producto + botón debajo” con aire
        VerticalLayout bloqueProducto = new VerticalLayout(comboProducto, anadir);
        bloqueProducto.setPadding(false);
        bloqueProducto.setSpacing(false);
        bloqueProducto.getStyle().set("gap", "10px");
        bloqueProducto.setAlignItems(FlexComponent.Alignment.STRETCH);
        bloqueProducto.setWidth("420px");

        HorizontalLayout filaAdd = new HorizontalLayout(bloqueProducto, cantidad);
        filaAdd.setWidthFull();
        filaAdd.setSpacing(false);
        filaAdd.getStyle().set("gap", "14px");
        filaAdd.setAlignItems(FlexComponent.Alignment.END);
        filaAdd.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        cardProductos.add(filtros, filaAdd);
        add(cardProductos);

        // GRID DE PRODUCTOS

        H3 tituloGrid = new H3("Productos añadidos");
        tituloGrid.getStyle().set("margin", "6px 0 0 0");
        add(tituloGrid);

        VerticalLayout cardGrid = crearCard();
        cardGrid.getStyle().set("gap", "12px");

        configurarGrid();

        grid.setWidthFull();
        grid.getStyle().set("border-radius", "10px");
        grid.getStyle().set("overflow", "hidden");

        total.getStyle().set("font-weight", "600");
        total.getStyle().set("font-size", "1.05rem");

        HorizontalLayout pie = new HorizontalLayout(total);
        pie.setWidthFull();
        pie.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        cardGrid.add(grid, pie);
        add(cardGrid);

        // CONFIRMAR PEDIDO

        VerticalLayout cardConfirmar = crearCard();
        cardConfirmar.setPadding(true);
        cardConfirmar.getStyle().set("gap", "10px");

        confirmar.addClickListener(e -> confirmarPedido());
        confirmar.getStyle().set("font-weight", "600");
        confirmar.setWidth("360px");

        HorizontalLayout filaConfirmar = new HorizontalLayout(confirmar);
        filaConfirmar.setWidthFull();
        filaConfirmar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        cardConfirmar.add(filaConfirmar);
        add(cardConfirmar);

        // Estado inicial
        setUiPedidoCreado(false);
        recargarProductos();
    }

    private VerticalLayout crearCard() {
        VerticalLayout card = new VerticalLayout();
        card.setPadding(true);
        card.setSpacing(false);
        card.setWidthFull();

        // “card” simple sin dependencias externas
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");

        return card;
    }

    // FILTROS

    private void configurarFiltrosProducto() {

        buscarProducto.setPlaceholder("Buscar por nombre");
        buscarProducto.setClearButtonVisible(true);
        buscarProducto.setValueChangeMode(ValueChangeMode.EAGER);

        filtroCategoria.setItems(
                categoriaService.listarCategorias().stream()
                        .map(Categoria::getNombre)
                        .toList()
        );
        filtroCategoria.setClearButtonVisible(true);

        // Buscar por nombre → limpia categoría
        buscarProducto.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                filtroCategoria.clear();
                recargarProductos();
            }
        });

        // Filtrar por categoría → limpia buscador
        filtroCategoria.addValueChangeListener(e -> {
            if (e.isFromClient()) {
                buscarProducto.clear();
                recargarProductos();
            }
        });
    }

    private void configurarComboProducto() {
        comboProducto.setItemLabelGenerator(
                p -> p.getNombre() + " - " + p.getPrecio() + "€"
        );
        comboProducto.setWidth("420px");
    }

    private void recargarProductos() {

        String texto = buscarProducto.getValue();
        String categoria = filtroCategoria.getValue();

        if (texto != null && !texto.isBlank()) {
            comboProducto.setItems(productoService.buscarPorNombreParcial(texto));
            return;
        }

        if (categoria != null && !categoria.isBlank()) {
            comboProducto.setItems(productoService.buscarPorCategoria(categoria));
            return;
        }

        // Estado inicial
        comboProducto.setItems(productoService.buscarPorNombreParcial(""));
    }

    // ACCIONES

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
            Notification.show("Pedido creado: " + pedidoActual.getCodigo(), 3000, Notification.Position.MIDDLE);
            refrescarGrid();
        } catch (Exception ex) {
            Notification.show("Error creando pedido: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void anadirProducto() {
        if (!hayPedidoCreado()) return;

        Producto prod = comboProducto.getValue();
        Integer qty = cantidad.getValue();

        if (prod == null || qty == null || qty <= 0) {
            Notification.show("Producto o cantidad inválidos", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoActual = pedidoService.agregarProducto(
                    pedidoActual.getCodigo(),
                    prod.getCodigo(),
                    qty
            );

            cantidad.setValue(1);
            refrescarGrid();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarPedido() {
        if (!hayPedidoCreado()) return;

        try {
            pedidoActual = pedidoService.confirmarPedido(pedidoActual.getCodigo());
            Notification.show("Pedido enviado a cocina", 3000, Notification.Position.MIDDLE);
            setUiPedidoConfirmado();
            refrescarGrid();
        } catch (Exception ex) {
            Notification.show("Error confirmando: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private boolean hayPedidoCreado() {
        if (pedidoActual == null) {
            Notification.show("Primero crea un pedido", 3000, Notification.Position.MIDDLE);
            return false;
        }
        return true;
    }

    private void setUiPedidoCreado(boolean creado) {
        buscarProducto.setEnabled(creado);
        filtroCategoria.setEnabled(creado);

        comboProducto.setEnabled(creado);
        cantidad.setEnabled(creado);
        anadir.setEnabled(creado);

        grid.setEnabled(creado);
        confirmar.setEnabled(creado);
    }

    private void setUiPedidoConfirmado() {
        buscarProducto.setEnabled(false);
        filtroCategoria.setEnabled(false);

        comboProducto.setEnabled(false);
        cantidad.setEnabled(false);
        anadir.setEnabled(false);

        grid.setEnabled(false);
        confirmar.setEnabled(false);
    }

    // GRID

    private void configurarGrid() {

        configurarGridColumnasBasicas();

        grid.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setMin(1);
            qty.setValue(lp.getCantidad());
            qty.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            qty.setWidth("140px");

            qty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;
                if (!hayPedidoCreado()) return;

                Integer nueva = ev.getValue();
                if (nueva == null || nueva <= 0) {
                    Notification.show("Cantidad inválida", 2500, Notification.Position.MIDDLE);
                    qty.setValue(lp.getCantidad());
                    return;
                }

                pedidoActual = pedidoService.actualizarCantidadProducto(
                        pedidoActual.getCodigo(),
                        lp.getProducto().getCodigo(),
                        nueva
                );
                refrescarGrid();
            });

            return qty;
        }).setHeader("Modificar");

        configurarGridEliminar();
    }

    private void configurarGridColumnasBasicas() {
        grid.addColumn(lp -> lp.getProducto().getNombre())
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad")
                .setAutoWidth(true);

        grid.addColumn(lp -> lp.calcularPrecio() + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true);
    }

    private void configurarGridEliminar() {
        grid.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                if (!hayPedidoCreado()) return;

                pedidoActual = pedidoService.eliminarProducto(
                        pedidoActual.getCodigo(),
                        lp.getProducto().getCodigo()
                );
                refrescarGrid();
            });
            return borrar;
        }).setHeader("Eliminar");
    }

    // REFRESCO

    private void refrescarGrid() {
        if (pedidoActual == null) return;
        grid.setItems(pedidoActual.getLineaPedidos());
        total.setText("Total: " + pedidoActual.calcularPrecioTotal() + " €");
    }
}