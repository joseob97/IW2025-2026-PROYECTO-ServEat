package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.compartida.panel.PanelBaseView;
import com.serveat.view.empleado.administrador.estadisticas.CierreCajaView;
import com.serveat.view.empleado.administrador.estadisticas.EstadisticasAdminView;
import com.serveat.view.empleado.administrador.productos.GestionIngredientesView;
import com.serveat.view.empleado.administrador.productos.GestionProductosView;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Route(value = "empleado/admin", layout = MainLayout.class)
@Secured("ROLE_ADMIN")
public class PanelAdminView extends PanelBaseView {

    private final FeatureService featureService;

    public PanelAdminView(FeatureService featureService) {
        this.featureService = featureService;

        H2 titulo = new H2("Panel de administración");
        add(titulo);

        List<Component> cardsOrdenadas = new ArrayList<>();

        // Bloques basicos
        cardsOrdenadas.add(Cards.cardAccionProConIcono(
                VaadinIcon.USERS.create(),
                "Gestión de empleados",
                "Alta, baja y gestión de empleados del sistema.",
                GestionEmpleadosView.class,
                true,
                "Abrir"
        ));

        cardsOrdenadas.add(Cards.cardAccionProConIcono(
                VaadinIcon.USER.create(),
                "Gestión de usuarios",
                "Administración de clientes registrados.",
                GestionClientesView.class,
                true,
                "Abrir"
        ));

        cardsOrdenadas.add(Cards.cardAccionProConIcono(
                VaadinIcon.BUILDING.create(),
                "Información del local",
                "Datos del establecimiento y configuración.",
                GestionDatosLocalView.class,
                true,
                "Abrir"
        ));

        cardsOrdenadas.add(Cards.cardAccionProConIcono(
                VaadinIcon.CREDIT_CARD.create(),
                "Suscripción / Plan",
                "Gestión de funcionalidades premium.",
                SuscripcionAdminView.class,
                true,
                "Abrir"
        ));

        cardsOrdenadas.add(Cards.cardAccionProConIcono(
                VaadinIcon.CUBE.create(),
                "Productos",
                "Gestión de productos del catálogo.",
                GestionProductosView.class,
                true,
                "Abrir"
        ));

        cardsOrdenadas.add(Cards.cardAccionProConIcono(
                VaadinIcon.CASH.create(),
                "Gestión de Caja",
                "Apertura y cierre manual de caja y arqueo.",
                CierreCajaView.class,
                true,
                "Abrir"
        ));

        // Bloques premium
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
                new PremiumCard(
                        Feature.EXPORTAR_DATOS,
                        VaadinIcon.DOWNLOAD.create(),
                        "Exportar datos",
                        "Exportación CSV/Excel/PDF para gestoría.",
                        ExportarDatosView.class
                )
        );

        List<PremiumCard> premiumOrdenadas = premium.stream()
                .sorted(Comparator
                        .comparing((PremiumCard c) -> !featureService.tieneFeature(c.feature))
                        .thenComparing(c -> c.titulo))
                .toList();

        for (PremiumCard c : premiumOrdenadas) {
            boolean habilitado = featureService.tieneFeature(c.feature);
            cardsOrdenadas.add(Cards.cardAccionProConIcono(
                    c.icono,
                    c.titulo,
                    c.descripcion,
                    c.destino,
                    habilitado,
                    habilitado ? "Abrir" : "Requiere PRO"
            ));
        }

        add(Cards.renderEnFilasDeDos(cardsOrdenadas, "520px"));
    }

    private static class PremiumCard {
        private final Feature feature;
        private final com.vaadin.flow.component.icon.Icon icono;
        private final String titulo;
        private final String descripcion;
        private final Class<? extends Component> destino;

        private PremiumCard(Feature feature,
                            com.vaadin.flow.component.icon.Icon icono,
                            String titulo,
                            String descripcion,
                            Class<? extends Component> destino) {
            this.feature = feature;
            this.icono = icono;
            this.titulo = titulo;
            this.descripcion = descripcion;
            this.destino = destino;
        }
    }
}