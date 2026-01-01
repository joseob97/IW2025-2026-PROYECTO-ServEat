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
import com.vaadin.flow.component.html.H3;
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

        // 🔹 Listado de menús existentes (para que el admin vea lo creado)
        H3 tituloListado = new H3("Menús creados");
        VerticalLayout listadoMenus = new VerticalLayout();
        listadoMenus.setPadding(false);
        listadoMenus.setSpacing(false);
        listadoMenus.getStyle().set("gap", "10px");

        Runnable recargarListado = () -> {
            listadoMenus.removeAll();
            List<Menu> menus = menuService.obtenerMenusActivos();

            if (menus.isEmpty()) {
                listadoMenus.add(new Paragraph("Aún no hay menús creados."));
                return;
            }

            for (Menu m : menus) {
                VerticalLayout card = new VerticalLayout();
                card.setPadding(true);
                card.setSpacing(false);
                card.getStyle().set("gap", "6px");
                card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
                card.getStyle().set("border-radius", "12px");
                card.getStyle().set("background", "var(--lumo-base-color)");

                card.add(
                        new Paragraph("Nombre: " + m.getNombre()),
                        new Paragraph("Precio fijo: " + m.getPrecioFijo() + " €")
                );

                listadoMenus.add(card);
            }
        };

        // Cargar listado al entrar
        recargarListado.run();

        Button crearMenu = new Button("Crear menú", event -> {
            try {
                Menu menu = new Menu();
                menu.setNombre(nombre.getValue());
                menu.setDescripcion(descripcion.getValue());
                menu.setProductos(productos.getValue().stream().toList());

                if (precio.getValue() == null) {
                    throw new IllegalArgumentException("El precio fijo es obligatorio");
                }
                menu.setPrecioFijo(BigDecimal.valueOf(precio.getValue()));

                menuService.crearMenu(menu);

                Notification.show("Menú creado correctamente");
                nombre.clear();
                descripcion.clear();
                productos.clear();
                precio.clear();

                // Recargar listado tras crear
                recargarListado.run();

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

        add(titulo, formulario, tituloListado, listadoMenus);
    }
}
