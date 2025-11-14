package com.serveat.view.empleado.administrador;

import com.serveat.service.menu.ProductoService;
import com.serveat.service.menu.CategoriaService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;

@PageTitle("Productos | Admin")
@Route(value = "admin/productos", layout = MainLayout.class)
public class ProductoAdminView extends VerticalLayout {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoAdminView(ProductoService productoService,
                             CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;

        setPadding(true);
        setSpacing(true);

        TextField nombre = new TextField("Nombre");
        TextArea descripcion = new TextArea("Descripción");

        NumberField precio = new NumberField("Precio (€)");
        precio.setStep(0.10);

        ComboBox<String> categoria = new ComboBox<>("Categoría");
        categoria.setItems(
                categoriaService.listarCategorias()
                        .stream()
                        .map(c -> c.getNombre())
                        .toList()
        );

        Button crear = new Button("Crear producto", e -> {
            try {
                BigDecimal precioBD = precio.getValue() != null
                        ? BigDecimal.valueOf(precio.getValue())
                        : null;

                productoService.crearProducto(
                        nombre.getValue(),
                        descripcion.getValue(),
                        precioBD,
                        categoria.getValue()
                );

                Notification.show("Producto creado correctamente");
                nombre.clear();
                descripcion.clear();
                precio.clear();
                categoria.clear();

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 4000,
                        Notification.Position.MIDDLE);
            }
        });

        FormLayout form = new FormLayout(nombre, descripcion, precio, categoria);
        add(form, crear);
    }
}