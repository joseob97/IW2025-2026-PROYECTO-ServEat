package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.service.seguridad.FeatureUnlockService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
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

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

@Route(value = "empleado/admin/suscripcion", layout = MainLayout.class)
@PageTitle("Suscripción | Admin")
@Secured("ROLE_ADMIN")
public class SuscripcionAdminView extends VerticalLayout {

    private final FeatureService featureService;
    private final FeatureUnlockService featureUnlockService;

    private final Paragraph resumen = new Paragraph();
    private final VerticalLayout listado = new VerticalLayout();

    public SuscripcionAdminView(FeatureService featureService,
                                FeatureUnlockService featureUnlockService) {
        this.featureService = featureService;
        this.featureUnlockService = featureUnlockService;

        setPadding(true);
        setSpacing(true);

        add(new H2("Módulos extra del establecimiento"));
        add(new Paragraph("Gestiona funcionalidades premium del sistema."));
        add(resumen);
        add(listado);

        renderizar();
    }

    /* ===================== RENDER ===================== */

    private void renderizar() {
        listado.removeAll();

        Set<Feature> activas = featureService.listarFeaturesActivos();

        if (activas.isEmpty()) {
            resumen.setText("Módulos activos: (ninguno). El sistema base sigue operativo.");
        } else {
            resumen.setText("Módulos activos: " + activas);
        }

        // Primero activas, luego el resto
        Stream<Feature> ordenadas = Stream.concat(
                activas.stream(),
                Arrays.stream(Feature.values()).filter(f -> !activas.contains(f))
        );

        ordenadas.forEach(f -> listado.add(crearFilaFeature(f, activas)));
    }

    /* ===================== FILA FEATURE ===================== */

    private HorizontalLayout crearFilaFeature(Feature feature, Set<Feature> activas) {

        Icon icono = activas.contains(feature)
                ? VaadinIcon.CHECK.create()
                : VaadinIcon.LOCK.create();

        icono.getStyle().set("margin-right", "8px");

        Paragraph titulo = new Paragraph(etiqueta(feature));
        titulo.getStyle().set("font-weight", "600");

        Paragraph desc = new Paragraph(descripcion(feature));
        desc.getStyle().set("margin", "0");
        desc.getStyle().set("opacity", "0.8");

        VerticalLayout texto = new VerticalLayout(titulo, desc);
        texto.setPadding(false);
        texto.setSpacing(false);

        HorizontalLayout row = new HorizontalLayout(icono, texto, crearAccion(feature, activas));
        row.setWidthFull();
        row.setPadding(true);
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        row.getStyle().set("border-radius", "12px");

        return row;
    }

    /* ===================== ACCIÓN ===================== */

    private Button crearAccion(Feature feature, Set<Feature> activas) {

        // YA ACTIVA
        if (activas.contains(feature)) {
            Button activa = new Button("Activa");
            activa.setEnabled(false);
            activa.getStyle().set("color", "var(--lumo-success-text-color)");
            return activa;
        }

        // PAGADA PERO NO ACTIVADA
        if (featureUnlockService.isFeaturePagada(feature)) {
            Button codigo = new Button("Activar");
            codigo.addClickListener(e -> mostrarDialogCodigo(feature));
            return codigo;
        }

        // NO PAGADA
        BigDecimal precio = featureUnlockService.obtenerPrecioFeature(feature);
        Button pagar = new Button("Pagar (" + precio + " €)");
        pagar.getStyle().set("color", "var(--lumo-success-text-color)");
        pagar.addClickListener(e -> mostrarDialogPago(feature, precio));
        return pagar;
    }

    /* ===================== DIALOG PAGO ===================== */

    private void mostrarDialogPago(Feature feature, BigDecimal precio) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Confirmar compra");

        Paragraph texto = new Paragraph(
                "Esta funcionalidad requiere un pago para su activación.\n" +
                        "Precio: " + precio + " €"
        );

        Button cancelar = new Button("Cancelar", e -> dialog.close());

        Button confirmar = new Button("Pagar", e -> {
            try {
                String codigo = featureUnlockService.simularPagoYObtenerCodigo(feature);
                dialog.close();

                Notification.show(
                        "Pago registrado correctamente",
                        2500,
                        Notification.Position.MIDDLE
                );

                mostrarDialogCodigoGenerado(codigo);
                renderizar();

            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        dialog.add(texto);
        dialog.getFooter().add(cancelar, confirmar);
        dialog.open();
    }

    /* ===================== DIALOG CÓDIGO ===================== */

    private void mostrarDialogCodigo(Feature feature) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Introducir código de desbloqueo");

        com.vaadin.flow.component.textfield.TextField campo =
                new com.vaadin.flow.component.textfield.TextField("Código");

        Button cancelar = new Button("Cancelar", e -> dialog.close());

        Button activar = new Button("Activar", e -> {
            try {
                featureUnlockService.validarCodigoYActivar(feature, campo.getValue());
                dialog.close();
                Notification.show("Funcionalidad activada", 2500, Notification.Position.MIDDLE);
                renderizar();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        dialog.add(campo);
        dialog.getFooter().add(cancelar, activar);
        dialog.open();
    }

    private void mostrarDialogCodigoGenerado(String codigo) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Código de desbloqueo");

        Paragraph texto = new Paragraph(
                "Guarda este código. Lo necesitarás para activar la funcionalidad:\n\n" + codigo
        );
        texto.getStyle().set("font-weight", "600");

        Button cerrar = new Button("Cerrar", e -> dialog.close());

        dialog.add(texto);
        dialog.getFooter().add(cerrar);
        dialog.open();
    }

    /* ===================== TEXTOS ===================== */

    private String etiqueta(Feature f) {
        return switch (f) {
            case PROMOCIONES -> "Promociones";
            case MENUS_OFERTAS -> "Menús / Ofertas";
            case PAGO_TARJETA -> "Pago con tarjeta (TPV)";
            case PAGO_ONLINE -> "Pago online (pasarela)";
            case FACTURACION_TICKET -> "Ticket / Factura (PDF)";
            case ESTADISTICAS -> "Estadísticas de ventas";
            case EXPORTAR_DATOS -> "Exportar datos";
            case CIERRE_CAJA -> "Cierre de caja";
            case INGREDIENTES -> "Gestión de ingredientes";
            case NOTIFICACIONES -> "Notificaciones";
        };
    }

    private String descripcion(Feature f) {
        return switch (f) {
            case PROMOCIONES -> "Cupones, descuentos y campañas.";
            case MENUS_OFERTAS -> "Menús y combos configurables.";
            case PAGO_TARJETA -> "Pago con tarjeta en el local.";
            case PAGO_ONLINE -> "Pago online antes de confirmar.";
            case FACTURACION_TICKET -> "Ticket y factura en PDF.";
            case ESTADISTICAS -> "KPIs y métricas de ventas.";
            case EXPORTAR_DATOS -> "Exportación para gestoría.";
            case CIERRE_CAJA -> "Arqueo y cierre diario.";
            case INGREDIENTES -> "Ingredientes, alérgenos y stock.";
            case NOTIFICACIONES -> "Avisos a clientes y empleados.";
        };
    }
}
