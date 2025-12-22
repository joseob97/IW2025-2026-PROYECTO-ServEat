package com.serveat.view.cliente.inicio;

import com.serveat.domain.menu.Producto;
import com.serveat.service.menu.ProductoService;
import com.serveat.view.cliente.pedido.PanelPedidoClienteView;
import com.serveat.view.layout.MainLayout;
import com.serveat.view.publico.carta.CartaView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import com.vaadin.flow.component.Component;

import java.util.List;

@PageTitle("Inicio | Cliente")
@Route(value = "cliente/inicio", layout = MainLayout.class)
@Secured("ROLE_CLIENTE")
public class InicioClienteView extends VerticalLayout {

    public InicioClienteView(ProductoService productoService) {

        setPadding(true);
        setSpacing(false);
        setWidthFull();
        getStyle().set("max-width", "1100px");
        getStyle().set("margin", "0 auto");
        getStyle().set("gap", "20px");

        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();

        H2 saludo = new H2("Hola, " + username + " 👋");
        Span subtitulo = new Span("¿Qué te apetece hoy?");
        subtitulo.getStyle().set("color", "var(--lumo-secondary-text-color)");

        add(saludo, subtitulo);

        // ACCIONES PRINCIPALES
        HorizontalLayout acciones = new HorizontalLayout(
                crearBoton("🛍️ Hacer pedido", PanelPedidoClienteView.class),
                crearBoton("📖 Ver carta", CartaView.class)
        );
        acciones.setWidthFull();
        acciones.getStyle().set("gap", "14px");

        add(acciones);

        // PRODUCTOS DESTACADOS
        H3 destacadosTitulo = new H3("Productos destacados");
        add(destacadosTitulo);

        HorizontalLayout destacados = new HorizontalLayout();
        destacados.setWidthFull();
        destacados.getStyle().set("gap", "14px");

        List<Producto> productos = productoService.buscarPorNombreParcial("")
                .stream().limit(4).toList();

        for (Producto p : productos) {
            destacados.add(crearCardProducto(p));
        }

        add(destacados);
    }



    private Button crearBoton(String texto, Class<? extends Component> destino) {
        Button b = new Button(texto, e ->
                getUI().ifPresent(ui -> ui.navigate(destino))
        );
        b.setWidth("260px");
        b.getStyle().set("font-weight", "600");
        return b;
    }

    private VerticalLayout crearCardProducto(Producto p) {

        Image img = new Image(
                p.getImagenUrl() != null ? p.getImagenUrl() : "/images/producto-default.jpg",
                p.getNombre()
        );
        img.setWidthFull();
        img.setHeight("140px");
        img.getStyle().set("object-fit", "cover");
        img.getStyle().set("border-radius", "10px");

        H4 nombre = new H4(p.getNombre());
        Span precio = new Span(p.getPrecio() + " €");
        precio.getStyle().set("font-weight", "600");

        VerticalLayout card = new VerticalLayout(img, nombre, precio);
        card.setPadding(true);
        card.setSpacing(false);
        card.getStyle().set("gap", "6px");
        card.getStyle().set("background", "var(--lumo-base-color)");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "14px");
        card.getStyle().set("box-shadow", "0 6px 18px rgba(0,0,0,0.06)");
        card.setWidth("240px");

        return card;
    }
}