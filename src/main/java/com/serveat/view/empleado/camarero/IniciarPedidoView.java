package com.serveat.view.empleado.camarero;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.menu.ProductoRepository;
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

    //  SERVICIOS (transient para Sonar/Vaadin)
    private final transient PedidoService pedidoService;
    private final ProductoRepository productoRepository;

    private Pedido pedidoActual;

    private final Grid<LineaPedido> grid = new Grid<>(LineaPedido.class, false);
    private final Span total = new Span("Total: 0 €");

    // UI refs
    private final IntegerField mesa = new IntegerField("Número de mesa");
    private final TextField codigo = new TextField("Código pedido");
    private final Button crearPedido = new Button("Crear pedido");

    private final ComboBox<Producto> comboProducto = new ComboBox<>("Producto");
    private final IntegerField cantidad = new IntegerField("Cantidad");
    private final Button anadir = new Button("Añadir");

    // Confirmación
    private final Button confirmar = new Button("✅ Confirmar pedido (Enviar a cocina)");

    public IniciarPedidoView(PedidoService pedidoService,
                             ProductoRepository productoRepository) {

        this.pedidoService = pedidoService;
        this.productoRepository = productoRepository;

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

        comboProducto.setItems(productoRepository.findAll());
        comboProducto.setItemLabelGenerator(p -> p.getNombre() + " - " + p.getPrecio() + "€");
        comboProducto.setWidth("420px");

        cantidad.setMin(1);
        cantidad.setStepButtonsVisible(true);
        cantidad.setValue(1);
        cantidad.setWidth("140px");

        anadir.addClickListener(e -> anadirProducto());

        HorizontalLayout addRow = new HorizontalLayout(comboProducto, cantidad, anadir);
        addRow.setAlignItems(Alignment.END);
        add(addRow);

        // GRID DE PRODUCTOS

        add(new H3("Productos añadidos"));

        configurarGrid();

        grid.setWidthFull();
        add(grid, total);

        // CONFIRMAR PEDIDO

        confirmar.addClickListener(e -> confirmarPedido());
        add(confirmar);

        // Estado inicial: hasta que no haya pedido, deshabilita añadir / grid / confirmar
        setUiPedidoCreado(false);
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

        if (prod == null) {
            Notification.show("Selecciona un producto", 3000, Notification.Position.MIDDLE);
            return;
        }
        if (qty == null || qty <= 0) {
            Notification.show("Cantidad inválida", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoActual = pedidoService.agregarProducto(
                    pedidoActual.getCodigo(),
                    prod.getCodigo(),
                    qty
            );

            // UX: deja cantidad a 1, mantiene producto seleccionado
            cantidad.setValue(1);

            refrescarGrid();
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
        }
    }

    private void confirmarPedido() {
        if (!hayPedidoCreado()) return;

        if (pedidoActual.getLineaPedidos() == null || pedidoActual.getLineaPedidos().isEmpty()) {
            Notification.show("No puedes confirmar un pedido vacío", 3000, Notification.Position.MIDDLE);
            return;
        }

        try {
            pedidoActual = pedidoService.confirmarPedido(pedidoActual.getCodigo());
            Notification.show("Pedido confirmado y enviado a cocina", 3000, Notification.Position.MIDDLE);

            // Bloquea edición tras confirmar
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
        comboProducto.setEnabled(creado);
        cantidad.setEnabled(creado);
        anadir.setEnabled(creado);
        grid.setEnabled(creado);
        confirmar.setEnabled(creado);
    }

    private void setUiPedidoConfirmado() {
        // Tras confirmar, ya no se puede editar
        comboProducto.setEnabled(false);
        cantidad.setEnabled(false);
        anadir.setEnabled(false);
        grid.setEnabled(false);
        confirmar.setEnabled(false);

        // Si quieres permitir “crear otro pedido” sin salir de la pantalla:
        mesa.setEnabled(true);
        crearPedido.setEnabled(true);
    }

    // GRID

    private void configurarGrid() {

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

        // MODIFICAR: llama al SERVICE (NO tocar la entidad desde la vista)
        grid.addComponentColumn(lp -> {
            IntegerField qty = new IntegerField();
            qty.setMin(1);
            qty.setStepButtonsVisible(true);
            qty.setValue(lp.getCantidad());
            qty.setWidth("110px");

            // Importante: para no disparar eventos al asignar setValue()
            qty.setValueChangeMode(ValueChangeMode.ON_CHANGE);

            qty.addValueChangeListener(ev -> {
                if (!ev.isFromClient()) return;
                if (!hayPedidoCreado()) return;

                Integer nuevaCantidad = ev.getValue();
                if (nuevaCantidad == null || nuevaCantidad <= 0) {
                    Notification.show("Cantidad inválida", 2500, Notification.Position.MIDDLE);
                    // vuelve a mostrar la cantidad real
                    qty.setValue(lp.getCantidad());
                    return;
                }

                try {
                    pedidoActual = pedidoService.actualizarCantidadProducto(
                            pedidoActual.getCodigo(),
                            lp.getProducto().getCodigo(),
                            nuevaCantidad
                    );
                    refrescarGrid();
                } catch (Exception ex) {
                    Notification.show("Error actualizando: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                    refrescarGrid();
                }
            });

            return qty;
        }).setHeader("Modificar");

        // ELIMINAR
        grid.addComponentColumn(lp -> {
            Button borrar = new Button("❌");
            borrar.addClickListener(e -> {
                if (!hayPedidoCreado()) return;

                try {
                    pedidoActual = pedidoService.eliminarProducto(
                            pedidoActual.getCodigo(),
                            lp.getProducto().getCodigo()
                    );
                    refrescarGrid();
                } catch (Exception ex) {
                    Notification.show("Error eliminando: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                }
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