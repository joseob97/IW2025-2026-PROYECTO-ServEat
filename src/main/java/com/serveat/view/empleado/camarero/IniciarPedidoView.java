package com.serveat.view.empleado.camarero;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.Categoria;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.menu.ProductoService;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.pedido.PedidoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
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

        setSpacing(true);
        setPadding(true);

        add(new H3("Iniciar pedido de mesa"));

        // CREAR PEDIDO

        mesa.setMin(1);
        mesa.setStepButtonsVisible(true);

        codigo.setReadOnly(true);

        crearPedido.addClickListener(e -> crearPedidoMesa());

        add(new HorizontalLayout(mesa, crearPedido), codigo);

        // AÑADIR PRODUCTOS

        add(new H3("Añadir productos"));

        configurarFiltrosProducto();
        configurarComboProducto();

        cantidad.setMin(1);
        cantidad.setStepButtonsVisible(true);
        cantidad.setValue(1);
        cantidad.setWidth("140px");

        anadir.addClickListener(e -> anadirProducto());

        HorizontalLayout filtros = new HorizontalLayout(buscarProducto, filtroCategoria);
        HorizontalLayout addRow = new HorizontalLayout(comboProducto, cantidad, anadir);
        filtros.setAlignItems(Alignment.END);
        addRow.setAlignItems(Alignment.END);

        add(filtros, addRow);

        // GRID DE PRODUCTOS

        add(new H3("Productos añadidos"));

        configurarGrid();

        grid.setWidthFull();
        add(grid, total);

        // CONFIRMAR PEDIDO

        confirmar.addClickListener(e -> confirmarPedido());
        add(confirmar);

        // Estado inicial
        setUiPedidoCreado(false);
        recargarProductos();
    }

    // ===================== FILTROS =====================

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

    // ===================== ACCIONES =====================

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
        comboProducto.setEnabled(creado);
        cantidad.setEnabled(creado);
        anadir.setEnabled(creado);
        grid.setEnabled(creado);
        confirmar.setEnabled(creado);
    }

    private void setUiPedidoConfirmado() {
        comboProducto.setEnabled(false);
        cantidad.setEnabled(false);
        anadir.setEnabled(false);
        grid.setEnabled(false);
        confirmar.setEnabled(false);
    }

    // ===================== GRID =====================

    private void configurarGrid() {

        configurarGridColumnasBasicas();

        grid.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setMin(1);
            qty.setValue(lp.getCantidad());
            qty.setValueChangeMode(ValueChangeMode.ON_CHANGE);

            qty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;

                pedidoActual = pedidoService.actualizarCantidadProducto(
                        pedidoActual.getCodigo(),
                        lp.getProducto().getCodigo(),
                        ev.getValue()
                );
                refrescarGrid();
            });

            return qty;
        }).setHeader("Modificar");

        configurarGridEliminar();
    }

    private void configurarGridColumnasBasicas() {
        grid.addColumn(lp -> lp.getProducto().getNombre()).setHeader("Producto");
        grid.addColumn(LineaPedido::getCantidad).setHeader("Cantidad");
        grid.addColumn(lp -> lp.calcularPrecio() + " €").setHeader("Subtotal");
    }

    private void configurarGridEliminar() {
        grid.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                pedidoActual = pedidoService.eliminarProducto(
                        pedidoActual.getCodigo(),
                        lp.getProducto().getCodigo()
                );
                refrescarGrid();
            });
            return borrar;
        }).setHeader("Eliminar");
    }

    // ===================== REFRESCO =====================

    private void refrescarGrid() {
        if (pedidoActual == null) return;
        grid.setItems(pedidoActual.getLineaPedidos());
        total.setText("Total: " + pedidoActual.calcularPrecioTotal() + " €");
    }
}