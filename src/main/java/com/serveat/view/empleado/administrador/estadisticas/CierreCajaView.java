package com.serveat.view.empleado.administrador.estadisticas;

import com.serveat.domain.caja.CierreCaja;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.caja.CierreCajaService;
import com.serveat.service.caja.EstadoCajaService;
import com.serveat.service.administrador.estadisticas.EstadisticasService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Route(value = "empleado/admin/cierre-caja", layout = MainLayout.class)
@PageTitle("Cierre de Caja | Admin")
@Secured("ROLE_ADMIN")
public class CierreCajaView extends VerticalLayout {

    private final EstadisticasService estadisticasService;
    private final CierreCajaService cierreCajaService;
    private final EstadoCajaService estadoCajaService;
    private final FeatureService featureService;

    private final VerticalLayout resultadosLayout = new VerticalLayout();
    private final Button cerrarCajaButton = new Button("Cerrar Caja");
    private final Button abrirCajaButton = new Button("Abrir Caja");

    public CierreCajaView(EstadisticasService estadisticasService,
                          CierreCajaService cierreCajaService,
                          EstadoCajaService estadoCajaService,
                          FeatureService featureService) {
        this.estadisticasService = estadisticasService;
        this.cierreCajaService = cierreCajaService;
        this.estadoCajaService = estadoCajaService;
        this.featureService = featureService;

        // Estilos para centrar y limitar el ancho
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        getStyle().set("max-width", "900px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Gestión de Caja");
        
        // Card para la acción
        VerticalLayout cardAccion = new VerticalLayout();
        cardAccion.setWidthFull();
        cardAccion.setPadding(true);
        cardAccion.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)");
        cardAccion.getStyle().set("border-radius", "12px");

        cerrarCajaButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        cerrarCajaButton.setWidthFull();
        cerrarCajaButton.addClickListener(e -> confirmarCierreCaja());

        abrirCajaButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        abrirCajaButton.setWidthFull();
        abrirCajaButton.setVisible(false);
        abrirCajaButton.addClickListener(e -> confirmarAperturaCaja());
        
        cardAccion.add(cerrarCajaButton, abrirCajaButton);

        // Layout para los resultados del turno
        resultadosLayout.setPadding(true);
        resultadosLayout.setVisible(false);
        resultadosLayout.setWidthFull();
        resultadosLayout.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        resultadosLayout.getStyle().set("border-radius", "12px");
        resultadosLayout.getStyle().set("margin-top", "20px");

        add(titulo, cardAccion, resultadosLayout);

        // BLOQUE PREMIUM: Histórico y Acumulado
        if (featureService.tieneFeature(Feature.CIERRE_CAJA)) {
            add(construirBloquePremium());
        }
        
        actualizarEstadoBotones();
    }

    private VerticalLayout construirBloquePremium() {
        VerticalLayout bloque = new VerticalLayout();
        bloque.setWidthFull();
        bloque.setPadding(true);
        bloque.getStyle().set("margin-top", "30px");
        bloque.getStyle().set("background-color", "var(--lumo-base-color)");
        bloque.getStyle().set("border", "1px solid var(--lumo-primary-color-50pct)");
        bloque.getStyle().set("border-radius", "12px");

        H3 titulo = new H3("📊 Panel de Control Financiero (Premium)");
        titulo.getStyle().set("color", "var(--lumo-primary-text-color)");

        // 1. Total Acumulado del Día
        Map<String, Object> datosDia = estadisticasService.generarCierreCajaDiario();
        BigDecimal totalDia = (BigDecimal) datosDia.get("total");
        
        Span labelTotal = new Span("Facturación Acumulada Hoy (00:00 - Actualidad):");
        Span valorTotal = new Span(totalDia + " €");
        valorTotal.getStyle().set("font-weight", "bold");
        valorTotal.getStyle().set("font-size", "1.5em");
        valorTotal.getStyle().set("color", "var(--lumo-success-text-color)");

        VerticalLayout kpiDia = new VerticalLayout(labelTotal, valorTotal);
        kpiDia.setSpacing(false);
        kpiDia.setPadding(false);

        // 2. Histórico de Cierres
        H3 subTitulo = new H3("Histórico de Cierres (Últimos 7 días)");
        subTitulo.getStyle().set("font-size", "1.1em");
        subTitulo.getStyle().set("margin-top", "20px");

        Grid<CierreCaja> grid = new Grid<>(CierreCaja.class, false);
        grid.addColumn(c -> c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setHeader("Fecha").setAutoWidth(true);
        grid.addColumn(c -> c.getTotalGeneral() + " €").setHeader("Total").setAutoWidth(true);
        grid.addColumn(c -> c.getTotalEfectivo() + " €").setHeader("Efectivo").setAutoWidth(true);
        grid.addColumn(c -> c.getTotalTarjeta() + " €").setHeader("Tarjeta").setAutoWidth(true);
        
        List<CierreCaja> historial = cierreCajaService.obtenerHistorialSemanal();
        grid.setItems(historial);
        grid.setHeight("250px");

        bloque.add(titulo, kpiDia, subTitulo, grid);
        return bloque;
    }

    private void actualizarEstadoBotones() {
        boolean abierta = estadoCajaService.isCajaAbierta();
        cerrarCajaButton.setVisible(abierta);
        abrirCajaButton.setVisible(!abierta);
        resultadosLayout.setVisible(false);
    }

    private void confirmarCierreCaja() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar Cierre de Caja Manual");
        
        String mensaje = "Usted está cerrando la caja manualmente. A partir de este momento no se atenderán pedidos hasta que vuelva a abrir la caja manualmente.";
        
        if (featureService.tieneFeature(Feature.CIERRE_CAJA)) {
            mensaje += "\n\n⚠️ ATENCIÓN: Tiene activa la apertura automática. La caja se abrirá automáticamente a las 13:00 si no la abre antes.";
        }
        
        dialog.setText(mensaje);
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        
        dialog.setConfirmText("Confirmar Cierre");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(event -> realizarCierreCaja());
        
        dialog.open();
    }

    private void confirmarAperturaCaja() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmar Apertura de Caja");
        
        String mensaje = "¿Desea abrir la caja? Se permitirán nuevos pedidos a partir de este momento.";
        
        if (featureService.tieneFeature(Feature.CIERRE_CAJA)) {
            mensaje += "\n\n⚠️ ATENCIÓN: Tiene activo el cierre automático. La caja se cerrará automáticamente a las 00:00.";
        }
        
        dialog.setText(mensaje);
        
        dialog.setCancelable(true);
        dialog.setCancelText("Cancelar");
        
        dialog.setConfirmText("Abrir Caja");
        dialog.setConfirmButtonTheme("success primary");

        dialog.addConfirmListener(event -> realizarAperturaCaja());
        
        dialog.open();
    }

    private void realizarAperturaCaja() {
        try {
            String usuario = SecurityContextHolder.getContext().getAuthentication().getName();
            estadoCajaService.abrirCaja(usuario);
            
            Notification.show("Caja abierta correctamente ✅", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            actualizarEstadoBotones();
        } catch (Exception e) {
            Notification.show("Error al abrir la caja: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void realizarCierreCaja() {
        try {
            String usuario = SecurityContextHolder.getContext().getAuthentication().getName();

            // 1. Obtener datos DEL TURNO
            Map<String, Object> resultados = estadisticasService.generarCierreCajaTurno();
            
            BigDecimal total = (BigDecimal) resultados.get("total");
            BigDecimal paypal = (BigDecimal) resultados.get("paypal");
            BigDecimal efectivo = (BigDecimal) resultados.get("efectivo");
            BigDecimal tarjeta = (BigDecimal) resultados.get("tarjeta");

            // 2. Guardar cierre (informe)
            try {
                cierreCajaService.cerrarCaja(LocalDate.now(), total, efectivo, tarjeta, paypal);
            } catch (IllegalStateException e) {
                // Ignoramos si ya existe un informe para hoy, permitimos cerrar el turno
            }

            // 3. Cambiar estado a CERRADA
            estadoCajaService.cerrarCaja(usuario);

            // 4. Mostrar éxito y resultados
            Notification.show("Caja cerrada correctamente ✅", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            
            mostrarResultados(resultados);
            resultadosLayout.setVisible(true);
            
            // 5. Actualizar botones
            cerrarCajaButton.setVisible(false);
            abrirCajaButton.setVisible(true);

        } catch (Exception e) {
            Notification.show("Error al cerrar la caja: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void mostrarResultados(Map<String, Object> resultados) {
        resultadosLayout.removeAll();

        BigDecimal total = (BigDecimal) resultados.get("total");
        BigDecimal paypal = (BigDecimal) resultados.get("paypal");
        BigDecimal efectivo = (BigDecimal) resultados.get("efectivo");
        BigDecimal tarjeta = (BigDecimal) resultados.get("tarjeta");

        H3 tituloResultados = new H3("Resultados del Cierre (Turno)");
        
        Span totalSpan = new Span("Total Turno: " + total + " €");
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
