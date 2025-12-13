package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Route(value = "empleado/admin/suscripcion", layout = MainLayout.class)
@PageTitle("Suscripción | Admin")
@Secured("ROLE_ADMIN")
public class SuscripcionAdminView extends VerticalLayout {

    private final FeatureService featureService;

    private final Paragraph resumen = new Paragraph();

    private final Map<Feature, Checkbox> checks = new EnumMap<>(Feature.class);

    public SuscripcionAdminView(FeatureService featureService) {
        this.featureService = featureService;

        setPadding(true);
        setSpacing(true);

        add(new H2("Módulos extra del establecimiento"));
        add(new Paragraph("Activa/desactiva funcionalidades premium por módulo."));
        add(resumen);

        for (Feature f : Feature.values()) {
            add(crearFilaFeature(f));
        }

        refrescar();
    }

    private HorizontalLayout crearFilaFeature(Feature feature) {
        Icon lock = VaadinIcon.LOCK.create();
        lock.getStyle().set("margin-right", "8px");

        Checkbox check = new Checkbox(etiqueta(feature));
        check.setValue(false);
        checks.put(feature, check);

        Paragraph desc = new Paragraph(descripcion(feature));
        desc.getStyle().set("margin", "0");
        desc.getStyle().set("opacity", "0.8");

        VerticalLayout text = new VerticalLayout(check, desc);
        text.setPadding(false);
        text.setSpacing(false);

        HorizontalLayout row = new HorizontalLayout(lock, text);
        row.setWidthFull();
        row.setPadding(true);
        row.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        row.getStyle().set("border-radius", "12px");

        // Acción: activar/desactivar SOLO llamando al servicio
        check.addValueChangeListener(e -> {
            try {
                if (Boolean.TRUE.equals(e.getValue())) {
                    featureService.activarFeature(feature);
                    Notification.show("Activado: " + etiqueta(feature), 2000, Notification.Position.MIDDLE);
                } else {
                    featureService.desactivarFeature(feature);
                    Notification.show("Desactivado: " + etiqueta(feature), 2000, Notification.Position.MIDDLE);
                }
                refrescar();
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 4000, Notification.Position.MIDDLE);
                refrescar(); // volver al estado real de BD
            }
        });

        return row;
    }

    private void refrescar() {
        Set<Feature> activos = featureService.listarFeaturesActivos();

        if (activos.isEmpty()) {
            resumen.setText("Módulos activos: (ninguno). Core funcionando: pago en efectivo, pedidos, carta, etc.");
        } else {
            resumen.setText("Módulos activos: " + activos);
        }

        for (Feature f : Feature.values()) {
            Checkbox c = checks.get(f);
            if (c != null) {
                boolean shouldBe = activos.contains(f);
                if (c.getValue() != shouldBe) {
                    c.setValue(shouldBe);
                }
            }
        }
    }

    private String etiqueta(Feature f) {
        return switch (f) {
            case PROMOCIONES -> "Promociones";
            case MENUS_OFERTAS -> "Menús / Ofertas";
            case PAGO_TARJETA -> "Pago con tarjeta (TPV)";
            case PAGO_ONLINE -> "Pago online (pasarela)";
            case FACTURACION_TICKET -> "Ticket / Factura (PDF)";
            case ESTADISTICAS -> "Estadísticas de ventas";
            case EXPORTAR_DATOS -> "Exportar datos (CSV/Excel/PDF)";
            case CIERRE_CAJA -> "Cierre de caja del día";
            case INGREDIENTES -> "Gestión de ingredientes";
            case NOTIFICACIONES -> "Notificaciones (cliente/empleados)";
        };
    }

    private String descripcion(Feature f) {
        return switch (f) {
            case PROMOCIONES -> "Cupones, descuentos, 2x1, campañas visibles en la web.";
            case MENUS_OFERTAS -> "Combos/menús configurables (ej: menú mediodía).";
            case PAGO_TARJETA -> "Permite pagar con tarjeta en local (además de efectivo).";
            case PAGO_ONLINE -> "Permite pagar online antes de confirmar el pedido.";
            case FACTURACION_TICKET -> "Generación de ticket/factura con numeración y PDF.";
            case ESTADISTICAS -> "Ventas por fecha, top productos, KPIs básicos.";
            case EXPORTAR_DATOS -> "Descarga para gestoría (CSV/Excel/PDF).";
            case CIERRE_CAJA -> "Arqueo/cierre del día con totales por método de pago.";
            case INGREDIENTES -> "Ingredientes por producto, alérgenos y base para stock.";
            case NOTIFICACIONES -> "Avisos a cocina/reparto/cliente cuando cambia el estado.";
        };
    }
}