package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.empleado.administrador.estadisticas.CierreCajaView;
import com.serveat.view.empleado.administrador.estadisticas.EstadisticasAdminView;
import com.serveat.view.empleado.administrador.productos.GestionIngredientesView;
import com.serveat.view.empleado.administrador.productos.GestionProductosView;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Route(value = "empleado/admin", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class PanelAdminView extends VerticalLayout {

    private final FeatureService featureService;

    public PanelAdminView(FeatureService featureService) {
        this.featureService = featureService;

        setSpacing(true);
        setPadding(true);

        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Panel de administración");
        add(titulo);

        List<Component> cardsOrdenadas = new ArrayList<>();

        /* =========================
           1) BLOQUES BÁSICOS (FIJOS ARRIBA)
           ========================= */
        cardsOrdenadas.add(cardBasica(
                VaadinIcon.USERS.create(),
                "Gestión de empleados",
                "Alta, baja y gestión de empleados del sistema.",
                GestionEmpleadosView.class
        ));

        cardsOrdenadas.add(cardBasica(
                VaadinIcon.USER.create(),
                "Gestión de usuarios",
                "Administración de clientes registrados.",
                GestionClientesView.class
        ));

        cardsOrdenadas.add(cardBasica(
                VaadinIcon.BUILDING.create(),
                "Información del local",
                "Datos del establecimiento y configuración.",
                GestionDatosLocalView.class
        ));

        cardsOrdenadas.add(cardBasica(
                VaadinIcon.CREDIT_CARD.create(),
                "Suscripción / Plan",
                "Gestión de funcionalidades premium.",
                SuscripcionAdminView.class
        ));

        cardsOrdenadas.add(cardBasica(
                VaadinIcon.CUBE.create(),
                "Productos",
                "Gestión de productos del catálogo.",
                GestionProductosView.class
        ));

        // NUEVO: Gestión de caja ahora es básica (gratis)
        cardsOrdenadas.add(cardBasica(
                VaadinIcon.CASH.create(),
                "Gestión de Caja",
                "Apertura y cierre manual de caja y arqueo.",
                CierreCajaView.class
        ));

        /* =========================
           2) BLOQUES PREMIUM (ORDENADOS: ACTIVAS -> BLOQUEADAS)
           ========================= */
        List<PremiumCard> premium = List.of(
                new PremiumCard(
                        Feature.NOTIFICACIONES,
                        VaadinIcon.BELL.create(),
                        "Notificaciones",
                        "Avisos del sistema a empleados y clientes.",
                        NotificacionesView.class
                ),
                new PremiumCard(
                        Feature.INGREDIENTES,
                        VaadinIcon.LIST_UL.create(),
                        "Ingredientes",
                        "Ingredientes por producto, alérgenos y base para stock.",
                        GestionIngredientesView.class
                ),
                new PremiumCard(
                        Feature.PROMOCIONES,
                        VaadinIcon.GIFT.create(),
                        "Promociones",
                        "Cupones, descuentos, 2x1, campañas visibles en la web.",
                        GestionPromosView.class
                ),
                new PremiumCard(
                        Feature.MENUS_OFERTAS,
                        VaadinIcon.CLIPBOARD_TEXT.create(),
                        "Menús / Ofertas",
                        "Gestión de menús configurables y combos.",
                        GestionMenusView.class
                ),
                new PremiumCard(
                        Feature.ESTADISTICAS,
                        VaadinIcon.CHART.create(),
                        "Estadísticas",
                        "Ventas por fecha, top productos y KPIs.",
                        EstadisticasAdminView.class
                ),
                // Cierre de caja movido a básico
                new PremiumCard(
                        Feature.EXPORTAR_DATOS,
                        VaadinIcon.DOWNLOAD.create(),
                        "Exportar datos",
                        "Exportación CSV/Excel/PDF para gestoría.",
                        ExportarDatosView.class
                )
        );

        // ACTIVAS primero, luego BLOQUEADAS. Dentro de cada grupo mantenemos un orden estable por título.
        List<PremiumCard> premiumOrdenadas = premium.stream()
                .sorted(Comparator
                        .comparing((PremiumCard c) -> !featureService.tieneFeature(c.feature)) // false(activas) primero
                        .thenComparing(c -> c.titulo))
                .toList();

        for (PremiumCard c : premiumOrdenadas) {
            boolean habilitado = featureService.tieneFeature(c.feature);
            cardsOrdenadas.add(cardPremium(c.icono, c.titulo, c.descripcion, c.destino, habilitado));
        }

        /* =========================
           PINTAR EN FILAS DE 2
           ========================= */
        add(renderEnFilasDeDos(cardsOrdenadas));
    }

    /* =========================
       RENDER: FILAS 2 COLUMNAS
       ========================= */
    private VerticalLayout renderEnFilasDeDos(List<Component> cards) {
        VerticalLayout contenedor = new VerticalLayout();
        contenedor.setPadding(false);
        contenedor.setSpacing(true);
        contenedor.setWidthFull();

        for (int i = 0; i < cards.size(); i += 2) {
            HorizontalLayout fila = new HorizontalLayout();
            fila.setWidthFull();
            fila.setSpacing(true);
            fila.getStyle().set("justify-content", "center"); // como el camarero

            Component izquierda = cards.get(i);
            fila.add(izquierda);

            if (i + 1 < cards.size()) {
                Component derecha = cards.get(i + 1);
                fila.add(derecha);
            } else {
                VerticalLayout spacer = new VerticalLayout();
                spacer.setWidth("520px");
                spacer.setPadding(false);
                spacer.setSpacing(false);
                spacer.getStyle().set("visibility", "hidden");
                fila.add(spacer);
            }

            contenedor.add(fila);
        }

        return contenedor;
    }

    /* =========================
       CARD BÁSICA
       ========================= */
    private VerticalLayout cardBasica(Icon icon, String titulo, String descripcion, Class<? extends Component> destino) {
        return construirCard(icon, titulo, descripcion, destino, true, "Abrir");
    }

    /* =========================
       CARD PREMIUM
       ========================= */
    private VerticalLayout cardPremium(Icon icon, String titulo, String descripcion,
                                       Class<? extends Component> destino, boolean habilitado) {
        return construirCard(icon, titulo, descripcion, destino, habilitado, habilitado ? "Abrir" : "Requiere PRO");
    }

    private VerticalLayout construirCard(Icon icon,
                                         String titulo,
                                         String descripcion,
                                         Class<? extends Component> destino,
                                         boolean habilitado,
                                         String textoBoton) {

        icon.getStyle().set("margin-right", "10px");
        icon.getStyle().set("flex-shrink", "0");

        H3 header = new H3();
        header.getStyle().set("margin", "0");
        header.getStyle().set("display", "flex");
        header.getStyle().set("align-items", "center");
        header.add(icon, new Paragraph(titulo));

        Paragraph desc = new Paragraph(descripcion);
        desc.getStyle().set("opacity", "0.75");
        desc.getStyle().set("margin", "0");

        Button abrir = new Button(textoBoton);
        abrir.setEnabled(habilitado);

        abrir.setWidth("140px");
        abrir.getStyle().set("margin-top", "10px");

        if (habilitado) {
            abrir.addClickListener(e -> UI.getCurrent().navigate(destino));
        } else {
            abrir.addClickListener(e ->
                    Notification.show("Funcionalidad disponible con plan PRO",
                            3000, Notification.Position.MIDDLE)
            );
        }

        VerticalLayout card = new VerticalLayout(header, desc, abrir);
        card.setPadding(true);
        card.setSpacing(true);

        card.setWidth("520px");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        card.getStyle().set("border-radius", "12px");
        card.getStyle().set("box-shadow", "0 4px 12px rgba(0,0,0,0.08)");

        card.getStyle().set("min-height", "150px");
        card.getStyle().set("justify-content", "space-between");

        return card;
    }

    /* =========================
       DTO INTERNO
       ========================= */
    private static class PremiumCard {
        private final Feature feature;
        private final Icon icono;
        private final String titulo;
        private final String descripcion;
        private final Class<? extends Component> destino;

        private PremiumCard(Feature feature, Icon icono, String titulo, String descripcion, Class<? extends Component> destino) {
            this.feature = feature;
            this.icono = icono;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.destino = destino;
        }
    }
}
