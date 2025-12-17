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
import org.springframework.security.core.context.SecurityContextHolder;

@PageTitle("Editar Pedido | Camarero")
@Route(value = "empleado/camarero/pedidos/editar", layout = MainLayout.class)
@Secured("ROLE_CAMARERO")
public class EditarPedidoView extends VerticalLayout {

    // SERVICIOS (transient para Sonar/Vaadin)
    private final transient PedidoService pedidoService;
    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;

    // ESTADO DE LA VISTA
    private transient Pedido pedidoSeleccionado;
    private transient boolean hayCambiosPendientes = false;

    // UI - Selección de pedido
    private final IntegerField filtroMesa = new IntegerField("Filtrar por mesa");
    private final Grid<Pedido> gridPedidos = new Grid<>(Pedido.class, false);

    // UI - Edición de pedido
    private final TextField codigoPedido = new TextField("Código pedido");

    // FILTROS DE PRODUCTO
    private final TextField buscarProducto = new TextField("Buscar producto");
    private final ComboBox<String> filtroCategoria = new ComboBox<>("Categoría");

    private final ComboBox<Producto> comboProducto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");
    private final Button anadir = new Button("Añadir");

    // CONFIRMAR CAMBIOS
    private final Button confirmarCambios = new Button("✅ Confirmar cambios");

    // GRID LINEAS
    private final Grid<LineaPedido> gridLineas = new Grid<>(LineaPedido.class, false);

    public EditarPedidoView(PedidoService pedidoService,
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

        H3 titulo = new H3("Editar pedido en curso");
        titulo.getStyle().set("margin", "0");
        add(titulo);

        // SELECCIONAR PEDIDO

        VerticalLayout cardPedidos = crearCard();
        cardPedidos.getStyle().set("gap", "12px");

        filtroMesa.setMin(1);
        filtroMesa.setClearButtonVisible(true);
        filtroMesa.setWidth("260px");
        filtroMesa.addValueChangeListener(e -> cargarPedidos());

        configurarGridPedidos();
        gridPedidos.setWidthFull();
        gridPedidos.setHeight("320px");
        gridPedidos.getStyle().set("border-radius", "10px");
        gridPedidos.getStyle().set("overflow", "hidden");

        cardPedidos.add(filtroMesa, gridPedidos);
        add(cardPedidos);

        // EDITAR PEDIDO

        H3 tituloEdicion = new H3("Modificar productos del pedido");
        tituloEdicion.getStyle().set("margin", "6px 0 0 0");
        add(tituloEdicion);

        VerticalLayout cardEdicion = crearCard();
        cardEdicion.getStyle().set("gap", "14px");

        codigoPedido.setReadOnly(true);
        codigoPedido.setWidth("320px");

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

        anadir.setWidth("420px");
        anadir.addClickListener(e -> anadirProductoAlPedido());

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

        configurarGridLineas();
        gridLineas.setWidthFull();
        gridLineas.getStyle().set("border-radius", "10px");
        gridLineas.getStyle().set("overflow", "hidden");

        // Confirmar cambios (centrado)
        confirmarCambios.setEnabled(false);
        confirmarCambios.getStyle().set("font-weight", "600");
        confirmarCambios.setWidth("360px");
        confirmarCambios.addClickListener(e -> confirmarCambios());

        HorizontalLayout filaConfirmar = new HorizontalLayout(confirmarCambios);
        filaConfirmar.setWidthFull();
        filaConfirmar.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        cardEdicion.add(codigoPedido, filtros, filaAdd, gridLineas, filaConfirmar);
        add(cardEdicion);

        // Estado inicial
        setUiPedidoSeleccionado(false);
        cargarPedidos();
        recargarProductos();
    }

    // GRID PEDIDOS

    private void configurarGridPedidos() {

        gridPedidos.addColumn(Pedido::getCodigo)
                .setHeader("Código")
                .setAutoWidth(true);

        gridPedidos.addColumn(p ->
                        p.getReservaMesa() != null ? p.getReservaMesa().getNumeroMesa() : "-")
                .setHeader("Mesa")
                .setAutoWidth(true);

        gridPedidos.addColumn(p -> p.getEstado().name())
                .setHeader("Estado pedido")
                .setAutoWidth(true);

        gridPedidos.addColumn(p ->
                        p.getEstadoCocina() != null ? p.getEstadoCocina().name() : "-")
                .setHeader("Estado cocina")
                .setAutoWidth(true);

        gridPedidos.addSelectionListener(e -> {
            pedidoSeleccionado = e.getFirstSelectedItem().orElse(null);

            if (pedidoSeleccionado == null) {
                setUiPedidoSeleccionado(false);
                return;
            }

            // Recarga el detalle desde servicio (con lineas)
            pedidoSeleccionado = pedidoService.obtenerPorCodigo(pedidoSeleccionado.getCodigo());
            codigoPedido.setValue(pedidoSeleccionado.getCodigo());
            refrescarLineas();

            // Reset cambios
            hayCambiosPendientes = false;
            confirmarCambios.setEnabled(false);

            setUiPedidoSeleccionado(true);
        });
    }

    private void cargarPedidos() {
        Integer mesa = filtroMesa.getValue();

        if (mesa == null) {
            // Usa el MISMO método que cancelación (misma lógica: EN_CURSO o EN_COCINA + PENDIENTE_ACEPTACION)
            gridPedidos.setItems(pedidoService.listarPedidosModificables());
        } else {
            gridPedidos.setItems(pedidoService.listarPedidosModificablesPorMesa(mesa));
        }

        pedidoSeleccionado = null;
        codigoPedido.clear();
        refrescarLineas();

        hayCambiosPendientes = false;
        confirmarCambios.setEnabled(false);

        setUiPedidoSeleccionado(false);
    }

    // FILTROS PRODUCTO

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
        comboProducto.setItemLabelGenerator(p -> p.getNombre() + " - " + p.getPrecio() + "€");
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

        // Estado inicial (sin filtros)
        comboProducto.setItems(productoService.buscarPorNombreParcial(""));
    }

    // GRID LINEAS

    private void configurarGridLineas() {

        gridLineas.addColumn(lp -> lp.getProducto().getNombre())
                .setHeader("Producto")
                .setAutoWidth(true)
                .setFlexGrow(1);

        gridLineas.addColumn(LineaPedido::getCantidad)
                .setHeader("Cantidad")
                .setAutoWidth(true);

        gridLineas.addColumn(lp -> lp.calcularPrecio() + " €")
                .setHeader("Subtotal")
                .setAutoWidth(true);

        // MODIFICAR (marcar cambios pendientes)
        gridLineas.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setMin(1);
            qty.setStepButtonsVisible(true);
            qty.setValue(lp.getCantidad());
            qty.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            qty.setWidth("140px");

            qty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;
                if (!hayPedidoSeleccionado()) return;

                Integer nueva = ev.getValue();
                if (nueva == null || nueva <= 0) {
                    Notification.show("Cantidad inválida", 2500, Notification.Position.MIDDLE);
                    qty.setValue(lp.getCantidad());
                    return;
                }

                try {
                    pedidoSeleccionado = pedidoService.actualizarCantidadProducto(
                            pedidoSeleccionado.getCodigo(),
                            lp.getProducto().getCodigo(),
                            nueva
                    );
                    refrescarLineas();
                    marcarCambiosPendientes();
                } catch (Exception ex) {
                    Notification.show("Error actualizando: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    refrescarLineas();
                }
            });

            return qty;
        }).setHeader("Modificar");

        // ELIMINAR (marcar cambios pendientes)
        gridLineas.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                if (!hayPedidoSeleccionado()) return;

                try {
                    pedidoSeleccionado = pedidoService.eliminarProducto(
                            pedidoSeleccionado.getCodigo(),
                            lp.getProducto().getCodigo()
                    );
                    refrescarLineas();
                    marcarCambiosPendientes();
                } catch (Exception ex) {
                    Notification.show("Error eliminando: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
            });
            return borrar;
        }).setHeader("Eliminar");
    }

    private void refrescarLineas() {
        if (pedidoSeleccionado == null) {
            gridLineas.setItems();
            return;
        }
        gridLineas.setItems(pedidoSeleccionado.getLineaPedidos());
    }

    // ACCIONES

    private void anadirProductoAlPedido() {
        if (!hayPedidoSeleccionado()) return;

        Producto prod = comboProducto.getValue();
        Integer qty = cantidad.getValue();

        if (prod == null || qty == null || qty <= 0) {
            Notification.show("Producto o cantidad inválidos", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoSeleccionado = pedidoService.agregarProducto(
                    pedidoSeleccionado.getCodigo(),
                    prod.getCodigo(),
                    qty
            );

            cantidad.setValue(1);
            refrescarLineas();
            marcarCambiosPendientes();

        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarCambios() {
        if (!hayPedidoSeleccionado()) return;

        if (!hayCambiosPendientes) {
            Notification.show("No hay cambios pendientes", 2500, Notification.Position.MIDDLE);
            return;
        }

        try {
            // Confirmación “lógica”: registra usuario/hora en el servicio
            // (implementa el método en el servicio: confirmarCambiosPedido(codigo, username))
            String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
            pedidoSeleccionado = pedidoService.confirmarCambiosPedido(pedidoSeleccionado.getCodigo(), usuario);

            hayCambiosPendientes = false;
            confirmarCambios.setEnabled(false);

            Notification.show("Cambios confirmados", 2500, Notification.Position.BOTTOM_START);
            refrescarLineas();

        } catch (Exception ex) {
            Notification.show("Error confirmando cambios: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void marcarCambiosPendientes() {
        hayCambiosPendientes = true;
        confirmarCambios.setEnabled(true);
    }

    private boolean hayPedidoSeleccionado() {
        if (pedidoSeleccionado == null) {
            Notification.show("Selecciona un pedido primero", 2500, Notification.Position.MIDDLE);
            return false;
        }
        return true;
    }

    private void setUiPedidoSeleccionado(boolean seleccionado) {
        codigoPedido.setEnabled(seleccionado);

        buscarProducto.setEnabled(seleccionado);
        filtroCategoria.setEnabled(seleccionado);

        comboProducto.setEnabled(seleccionado);
        cantidad.setEnabled(seleccionado);
        anadir.setEnabled(seleccionado);

        gridLineas.setEnabled(seleccionado);
        confirmarCambios.setEnabled(seleccionado && hayCambiosPendientes);
    }

    // UI HELPERS

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
        card.getStyle().set("gap", "12px");

        return card;
    }
}