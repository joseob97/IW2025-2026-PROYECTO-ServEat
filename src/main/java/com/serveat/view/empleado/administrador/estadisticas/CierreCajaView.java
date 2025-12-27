package com.serveat.view.empleado.administrador.estadisticas;

import com.serveat.service.estadisticas.EstadisticasService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;

import java.math.BigDecimal;
import java.util.Map;

@Route(value = "empleado/admin/cierre-caja", layout = MainLayout.class)
@PageTitle("Cierre de Caja | Admin")
@Secured("ROLE_ADMIN")
public class CierreCajaView extends VerticalLayout {

    private final EstadisticasService estadisticasService;

    private final VerticalLayout resultadosLayout = new VerticalLayout();

    public CierreCajaView(EstadisticasService estadisticasService) {
        this.estadisticasService = estadisticasService;

        // Estilos para centrar y limitar el ancho
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        getStyle().set("max-width", "800px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Cierre de Caja");
        
        // Card para la acción
        VerticalLayout cardAccion = new VerticalLayout();
        cardAccion.setWidthFull();
        cardAccion.setPadding(true);
        cardAccion.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        cardAccion.getStyle().set("border-radius", "12px");

        Button cerrarCajaButton = new Button("Generar Cierre de Caja del Día");
        cerrarCajaButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cerrarCajaButton.setWidthFull();
        cerrarCajaButton.addClickListener(e -> confirmarCierreCaja());
        
        cardAccion.add(cerrarCajaButton);

        // Layout para los resultados
        resultadosLayout.setPadding(true);
        resultadosLayout.setVisible(false);
        resultadosLayout.setWidthFull();
        resultadosLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        resultadosLayout.getStyle().set("border-radius", "12px");
        resultadosLayout.getStyle().set("margin-top", "20px");

        add(titulo, cardAccion, resultadosLayout);
    }

    private void confirmarCierreCaja() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar Cierre de Caja Manual");
        dialog.setText("Usted está cerrando la caja manualmente, a partir de este momento no se atenderán pedidos hasta que vuelva a abrir la caja manualmente o espere a la apertura automática del siguiente día.");
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        
        dialog.setConfirmText("Confirmar Cierre");
        dialog.setConfirmButtonTheme("error primary"); // Botón de confirmación rojo para dar énfasis

        dialog.addConfirmListener(event -> realizarCierreCaja());
        
        dialog.open();
    }

    private void realizarCierreCaja() {
        try {
            Map<String, Object> resultados = estadisticasService.generarCierreCajaDiario();
            mostrarResultados(resultados);
            resultadosLayout.setVisible(true);
        } catch (Exception e) {
            Notification.show("Error al generar el cierre de caja: " + e.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private void mostrarResultados(Map<String, Object> resultados) {
        resultadosLayout.removeAll();

        BigDecimal total = (BigDecimal) resultados.get("total");
        BigDecimal paypal = (BigDecimal) resultados.get("paypal");
        BigDecimal efectivo = (BigDecimal) resultados.get("efectivo");
        BigDecimal tarjeta = (BigDecimal) resultados.get("tarjeta");

        H3 tituloResultados = new H3("Resultados del Cierre");
        
        Span totalSpan = new Span("Total General: " + total + " €");
        totalSpan.getStyle().set("font-weight", "bold");
        totalSpan.getStyle().set("font-size", "1.5em");
        totalSpan.getStyle().set("color", "var(--lumo-primary-text-color)");

        VerticalLayout desglose = new VerticalLayout(
            new Span("Total PayPal: " + paypal + " €"),
            new Span("Total Efectivo: " + efectivo + " €"),
            new Span("Total Tarjeta: " + tarjeta + " €")
        );
        desglose.setPadding(false);
        desglose.setSpacing(false);
        desglose.getStyle().set("margin-top", "10px");
        desglose.getStyle().set("border-left", "3px solid var(--lumo-contrast-20pct)");
        desglose.getStyle().set("padding-left", "15px");

        resultadosLayout.add(tituloResultados, totalSpan, desglose);
    }
}
