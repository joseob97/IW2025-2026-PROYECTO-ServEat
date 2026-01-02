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
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

    private final MenuService menuService;
    private final ProductoRepository productoRepository;
    private final FeatureService featureService;

    private final VerticalLayout listadoMenus = new VerticalLayout();

    public GestionMenusView(MenuService menuService,
                            ProductoRepository productoRepository,
                            FeatureService featureService) {

        this.menuService = menuService;
        this.productoRepository = productoRepository;
        this.featureService = featureService;

        setPadding(true);
        setSpacing(true);
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Gestión de menús y ofertas");

        // 🔒 Control de feature premium
        if (!featureService.tieneFeature(Feature.MENUS_OFERTAS)) {
            add(
                    titulo,
                    new Paragraph("Esta funcionalidad requiere un plan premium."),
                    new Paragraph("Los clientes no verán menús ni ofertas si el módulo no está activo.")
            );
            return;
        }

        // =======================
        // FORMULARIO CREAR MENÚ
        // =======================

        TextField nombre = new TextField("Nombre del menú");
        nombre.setRequired(true);
        nombre.setHelperText("Nombre visible para los clientes");

        TextField descripcion = new TextField("Descripción");
        descripcion.setHelperText("Descripción opcional del menú");

        CheckboxGroup<Producto> productos = new CheckboxGroup<>();
        productos.setLabel("Productos incluidos");
        productos.setItems(productoRepository.findAll());
        productos.setItemLabelGenerator(Producto::getNombre);
        productos.setHelperText("Selecciona los productos que formarán el menú");

        NumberField precio = new NumberField("Precio fijo (€)");
        precio.setMin(0);
        precio.setStep(0.5);
        precio.setRequiredIndicatorVisible(true);
        precio.setHelperText("Precio total del menú completo");

        Button crearMenu = new Button("Crear menú", event -> {
            try {
                if (nombre.isEmpty() || precio.isEmpty()) {
                    Notification.show(
                            "El nombre y el precio son obligatorios",
                            3000,
                            Notification.Position.MIDDLE
                    );
                    return;
                }

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

                cargarListadoMenus();

            } catch (Exception e) {
                Notification.show(
                        e.getMessage(),
                        4000,
                        Notification.Position.MIDDLE
                );
            }
        });

        FormLayout formulario = new FormLayout(
                nombre,
                descripcion,
                productos,
                precio,
                crearMenu
        );
        formulario.setColspan(productos, 2);

        // =======================
        // LISTADO DE MENÚS
        // =======================

        H3 subtituloListado = new H3("Menús creados");
        listadoMenus.setSpacing(true);

        cargarListadoMenus();

        add(
                titulo,
                formulario,
                subtituloListado,
                listadoMenus
        );
    }

    // =======================
    // CARGA LISTADO MENÚS
    // =======================

    private void cargarListadoMenus() {
        listadoMenus.removeAll();

        List<Menu> menus = menuService.obtenerMenusActivos();

        if (menus.isEmpty()) {
            listadoMenus.add(new Paragraph("Aún no hay menús creados."));
            return;
        }

        for (Menu menu : menus) {
            VerticalLayout card = new VerticalLayout();
            card.setPadding(true);
            card.setSpacing(false);
            card.getStyle().set("gap", "8px");
            card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
            card.getStyle().set("border-radius", "12px");
            card.getStyle().set("background", "var(--lumo-base-color)");

            Paragraph info = new Paragraph(
                    "Nombre: " + menu.getNombre()
                            + " | Precio fijo: " + menu.getPrecioFijo() + " €"
            );

            Button eliminar = new Button("Eliminar");
            eliminar.getStyle().set("color", "var(--lumo-error-text-color)");

            eliminar.addClickListener(e -> {
                ConfirmDialog dialog = new ConfirmDialog();
                dialog.setHeader("Eliminar menú");
                dialog.setText(
                        "¿Seguro que deseas eliminar el menú \"" + menu.getNombre() + "\"?"
                );
                dialog.setConfirmText("Eliminar");
                dialog.setCancelable(true);

                dialog.addConfirmListener(ev -> {
                    menu.setActivo(false); // eliminación lógica
                    menuService.crearMenu(menu);
                    Notification.show("Menú eliminado correctamente");
                    cargarListadoMenus();
                });

                dialog.open();
            });

            HorizontalLayout acciones = new HorizontalLayout(eliminar);
            card.add(info, acciones);
            listadoMenus.add(card);
        }
    }
}
