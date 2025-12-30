package com.serveat.view.empleado.administrador;

import com.serveat.domain.menu.Menu;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.seguridad.Feature;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.service.menu.MenuService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.util.List;

@Route(value = "empleado/admin/menus", layout = MainLayout.class)
@PageTitle("Menús y Ofertas | Admin")
@Secured("ROLE_ADMIN")
public class GestionMenusView extends VerticalLayout {

    public GestionMenusView(MenuService menuService,
                            ProductoRepository productoRepository,
                            FeatureService featureService) {

        setPadding(true);
        setSpacing(true);

        H2 titulo = new H2("Gestión de menús y ofertas");

        // 🔒 Control de feature premium (igual que Promociones)
        if (!featureService.tieneFeature(Feature.MENUS_OFERTAS)) {
            add(
                    titulo,
                    new Paragraph("Esta funcionalidad requiere un plan premium."),
                    new Paragraph("Los clientes no verán menús ni ofertas si el módulo no está activo.")
            );
            return;
        }

        TextField nombre = new TextField("Nombre del menú");
        TextField descripcion = new TextField("Descripción");

        CheckboxGroup<Producto> productos = new CheckboxGroup<>();
        productos.setLabel("Productos incluidos");
        productos.setItems(productoRepository.findAll());
        productos.setItemLabelGenerator(Producto::getNombre);

        NumberField precio = new NumberField("Precio fijo");
        precio.setMin(0);
        precio.setStep(0.5);

        Button crearMenu = new Button("Crear menú", event -> {
            try {
                Menu menu = new Menu();
                menu.setNombre(nombre.getValue());
                menu.setDescripcion(descripcion.getValue());
                menu.setProductos(productos.getValue().stream().toList());
                menu.setPrecioFijo(BigDecimal.valueOf(precio.getValue()));

                menuService.crearMenu(menu);

                Notification.show("Menú creado correctamente");
                nombre.clear();
                descripcion.clear();
                productos.clear();
                precio.clear();

            } catch (Exception e) {
                Notification.show(e.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        FormLayout formulario = new FormLayout();
        formulario.add(
                nombre,
                descripcion,
                productos,
                precio,
                crearMenu
        );

        add(titulo, formulario);
    }
}
