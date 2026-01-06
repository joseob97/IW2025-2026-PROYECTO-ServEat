package com.serveat.view.empleado.administrador;

import com.serveat.domain.seguridad.Feature;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.service.seguridad.FeatureUnlockService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.UI;
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
        add(resumen, listado);

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
        desc.getStyle().set("opacity", "0.8");

        VerticalLayout texto = new VerticalLayout(titulo, desc);
        texto.setPadding(false);
        texto.setSpacing(false);

        HorizontalLayout row = new HorizontalLayout(icono, texto, crearAccion(feature, activas));
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        row.getStyle().set("border-radius", "12px");

        return row;
    }

    /* ===================== ACCIÓN ===================== */

    private Button crearAccion(Feature feature, Set<Feature> activas) {

        if (activas.contains(feature)) {
            Button activa = new Button("Activa");
            activa.setEnabled(false);
            activa.getStyle().set("color", "var(--lumo-success-text-color)");
            return activa;
        }

        if (featureUnlockService.isFeaturePagada(feature)) {
            Button activar = new Button("Activar");
            activar.addClickListener(e -> mostrarDialogCodigo(feature));
            return activar;
        }

        BigDecimal precio = featureUnlockService.obtenerPrecioFeature(feature);
        Button pagar = new Button("Pagar (" + precio + " €)");
        pagar.getStyle().set("color", "var(--lumo-success-text-color)");
        pagar.addClickListener(e -> mostrarDialogPago(feature, precio));
        return pagar;
    }

    /* ===================== DIALOGS ===================== */

    private void mostrarDialogPago(Feature feature, BigDecimal precio) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Confirmar compra");

        Paragraph texto = new Paragraph("Precio: " + precio + " €");

        Button cancelar = new Button("Cancelar", e -> dialog.close());
        Button confirmar = new Button("Pagar", e -> {
            try {
                featureUnlockService.simularPagoYObtenerCodigo(feature);
                dialog.close();
                Notification.show("Pago registrado correctamente", 2500, Notification.Position.MIDDLE);
                renderizar();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        dialog.add(texto);
        dialog.getFooter().add(cancelar, confirmar);
        dialog.open();
    }

    private void mostrarDialogCodigo(Feature feature) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Introducir código");

        var campo = new com.vaadin.flow.component.textfield.TextField("Código");

        Button cancelar = new Button("Cancelar", e -> dialog.close());
        Button activar = new Button("Activar", e -> {
            try {
                featureUnlockService.validarCodigoYActivar(feature, campo.getValue());
                dialog.close();
                UI.getCurrent().getPage().reload();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
            }
        });

        dialog.add(campo);
        dialog.getFooter().add(cancelar, activar);
        dialog.open();
    }

    /* ===================== TEXTOS ===================== */

    private String etiqueta(Feature f) {
        return f.name().replace("_", " ");
    }

    private String descripcion(Feature f) {
        return "";
    }
}
