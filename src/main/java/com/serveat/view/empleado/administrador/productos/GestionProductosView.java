package com.serveat.view.empleado.administrador.productos;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.service.menu.CategoriaService;
import com.serveat.service.menu.ProductoService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

@PageTitle("Gestión de productos | Admin")
@Route(value = "empleado/admin/productos/gestion", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class GestionProductosView extends VerticalLayout {

    private final transient ProductoService productoService;
    private final transient CategoriaService categoriaService;

    private final Grid<Producto> grid = new Grid<>(Producto.class, false);
    private final TextField filtroNombre = new TextField("Buscar por nombre");

    public GestionProductosView(ProductoService productoService,
                                CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setPadding(true);
        setSpacing(true);

        add(new H2("Gestión de productos"));

        configurarToolbar();
        configurarGrid();

        refrescar();
    }

    private void configurarToolbar() {
        filtroNombre.setPlaceholder("Escribe para filtrar...");
        filtroNombre.setClearButtonVisible(true);
        filtroNombre.addValueChangeListener(e -> refrescar());

        Button nuevo = new Button("➕ Nuevo", e -> abrirDialogo(null));

        Button editar = new Button("✏️ Editar", e -> {
            Producto sel = grid.asSingleSelect().getValue();
            if (sel == null) {
                Notification.show("Selecciona un producto", 2500, Notification.Position.MIDDLE);
                return;
            }
            abrirDialogo(sel);
        });

        Button eliminar = new Button("🗑 Eliminar", e -> {
            Producto sel = grid.asSingleSelect().getValue();
            if (sel == null) {
                Notification.show("Selecciona un producto", 2500, Notification.Position.MIDDLE);
                return;
            }
            try {
                productoService.eliminarProducto(sel.getCodigo());
                Notification.show("Producto eliminado");
                refrescar();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        HorizontalLayout toolbar = new HorizontalLayout(filtroNombre, nuevo, editar, eliminar);
        toolbar.setAlignItems(Alignment.END);
        add(toolbar);
    }

    private void configurarGrid() {
        grid.addColumn(Producto::getCodigo).setHeader("Código").setAutoWidth(true);
        grid.addColumn(Producto::getNombre).setHeader("Nombre").setFlexGrow(1);
        grid.addColumn(p -> p.getPrecio() == null ? "" : p.getPrecio().toPlainString()).setHeader("Precio").setAutoWidth(true);
        grid.addColumn(p -> p.getCategoria() != null ? p.getCategoria().getNombre() : "").setHeader("Categoría").setAutoWidth(true);
        grid.addColumn(Producto::getImagenUrl).setHeader("Imagen").setFlexGrow(1);

        grid.setWidthFull();
        add(grid);
    }

    private void refrescar() {
        String q = filtroNombre.getValue();
        if (q == null || q.isBlank()) {
            grid.setItems(productoService.listarProductos());
        } else {
            grid.setItems(productoService.buscarPorNombreParcial(q));
        }
    }

    private void abrirDialogo(Producto base) {
        boolean editando = base != null;

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(editando ? "Editar producto" : "Nuevo producto");

        TextField nombre = new TextField("Nombre");
        TextArea descripcion = new TextArea("Descripción");

        NumberField precio = new NumberField("Precio (€)");
        precio.setStep(0.10);

        ComboBox<String> categoria = new ComboBox<>("Categoría");
        categoria.setItems(
                categoriaService.listarCategorias().stream().map(c -> c.getNombre()).toList()
        );

        MultiSelectComboBox<String> ingredientes = new MultiSelectComboBox<>("Ingredientes");
        ingredientes.setItems(productoService.listarNombresIngredientes());

        MemoryBuffer buffer = new MemoryBuffer();
        Upload imagen = new Upload(buffer);
        imagen.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        imagen.setMaxFiles(1);
        imagen.setAutoUpload(true);

        if (editando) {
            Producto p = productoService.obtenerConIngredientesPorCodigo(base.getCodigo());

            nombre.setValue(p.getNombre() == null ? "" : p.getNombre());
            descripcion.setValue(p.getDescripcion() == null ? "" : p.getDescripcion());
            if (p.getPrecio() != null) precio.setValue(p.getPrecio().doubleValue());
            if (p.getCategoria() != null) categoria.setValue(p.getCategoria().getNombre());

            Set<String> seleccion = p.getIngredientes().stream()
                    .map(ProductoIngrediente::getIngrediente)
                    .filter(i -> i != null && i.getNombre() != null)
                    .map(i -> i.getNombre())
                    .collect(Collectors.toSet());

            ingredientes.select(seleccion);
        }

        Button guardar = new Button("Guardar", e -> {
            try {
                BigDecimal precioBD = precio.getValue() == null ? null : BigDecimal.valueOf(precio.getValue());

                byte[] imagenBytes = null;
                String nombreArchivo = null;

                try (InputStream is = buffer.getInputStream()) {
                    if (is != null && buffer.getFileName() != null && !buffer.getFileName().isBlank()) {
                        imagenBytes = is.readAllBytes();
                        nombreArchivo = buffer.getFileName();
                    }
                } catch (Exception ignore) {}

                if (!editando) {
                    productoService.crearProductoConIngredientes(
                            nombre.getValue(),
                            descripcion.getValue(),
                            precioBD,
                            categoria.getValue(),
                            ingredientes.getSelectedItems(),
                            imagenBytes,
                            nombreArchivo
                    );
                    Notification.show("Producto creado");
                } else {
                    productoService.actualizarProductoConIngredientes(
                            base.getCodigo(),
                            nombre.getValue(),
                            descripcion.getValue(),
                            precioBD,
                            categoria.getValue(),
                            ingredientes.getSelectedItems(),
                            imagenBytes,
                            nombreArchivo
                    );
                    Notification.show("Producto actualizado");
                }

                dialog.close();
                refrescar();

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        Button cancelar = new Button("Cancelar", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(guardar, cancelar);

        VerticalLayout content = new VerticalLayout(nombre, descripcion, precio, categoria, ingredientes, imagen, actions);
        content.setPadding(false);
        content.setSpacing(true);

        dialog.add(content);
        dialog.open();
    }
}