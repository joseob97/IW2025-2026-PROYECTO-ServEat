package com.serveat.view.cliente.menu;

import com.serveat.domain.menu.Menu;
import com.serveat.service.menu.MenuService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.util.List;

@PageTitle("Menús y Ofertas")
@Route(value = "cliente/menus", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class MenusClienteView extends VerticalLayout {

    public MenusClienteView(MenuService menuService) {
        setPadding(true);
        setSpacing(true);
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");

        add(new H2("Menús y ofertas"));

        // ✅ CAMBIO CLAVE: usar método con productos cargados
        List<Menu> menus = menuService.obtenerMenusActivosConProductos();

        if (menus.isEmpty()) {
            add(new Paragraph("No hay menús disponibles actualmente."));
            return;
        }

        for (Menu menu : menus) {
            VerticalLayout card = new VerticalLayout();
            card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
            card.getStyle().set("border-radius", "12px");
            card.getStyle().set("padding", "16px");
            card.getStyle().set("background", "var(--lumo-base-color)");
            card.getStyle().set("margin-bottom", "12px");

            card.add(
                    new H3(menu.getNombre()),
                    new Paragraph(menu.getDescripcion()),
                    new Paragraph("Precio: " + menu.getPrecioFijo() + " €"),
                    new Paragraph("Incluye:")
            );

            menu.getProductos().forEach(p ->
                    card.add(new Paragraph("• " + p.getNombre()))
            );

            add(card);
        }
    }
}
