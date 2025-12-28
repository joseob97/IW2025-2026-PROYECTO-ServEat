package com.serveat.view.empleado.administrador;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.pedido.PedidoService;
import com.serveat.service.seguridad.FeatureService;
import com.serveat.view.layout.MainLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.springframework.security.access.annotation.Secured;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "empleado/admin/exportar", layout = MainLayout.class)
@PageTitle("Exportar datos | Admin")
@Secured("ROLE_ADMIN")
public class ExportarDatosView extends VerticalLayout {

    private final PedidoService pedidoService;

    public ExportarDatosView(FeatureService featureService, PedidoService pedidoService) {
        this.pedidoService = pedidoService;

        setPadding(true);
        setSpacing(true);
        setWidthFull();
        getStyle().set("max-width", "800px");
        getStyle().set("margin", "0 auto");

        H2 titulo = new H2("Exportar datos");

        if (!featureService.tieneFeature(Feature.EXPORTAR_DATOS)) {
            add(titulo,
                    new Paragraph("La exportación avanzada requiere el plan PRO."),
                    new Paragraph("Ve a “Suscripción / Plan” para activarla."));
            return;
        }

        Paragraph descripcion = new Paragraph("Descarga un archivo CSV con el listado completo de pedidos para su gestión externa.");
        
        // Configurar descarga
        Button botonDescarga = new Button("Descargar Pedidos (.csv)");
        botonDescarga.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        StreamResource resource = new StreamResource("pedidos.csv", this::generarCsvPedidos);
        resource.setContentType("text/csv");
        resource.setCacheTime(0);

        Anchor downloadLink = new Anchor(resource, "");
        downloadLink.getElement().setAttribute("download", true);
        downloadLink.add(botonDescarga);

        add(titulo, descripcion, downloadLink);
    }

    private java.io.InputStream generarCsvPedidos() {
        List<Pedido> pedidos = pedidoService.listarTodosOrdenadosPorFecha();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        // Cabecera
        csv.append("Codigo;Fecha;Estado;Tipo;Cliente;Total\n");

        for (Pedido p : pedidos) {
            String codigo = p.getCodigo();
            String fecha = p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "";
            String estado = p.getEstado() != null ? p.getEstado().name() : "";
            String tipo = p.getTipoPedido() != null ? p.getTipoPedido().name() : "";
            String cliente = p.getCliente() != null ? p.getCliente().getUsername() : "Anonimo";
            String total = p.calcularPrecioTotal() != null ? p.calcularPrecioTotal().toString() : "0.00";

            // Escapar punto y coma si fuera necesario (simple)
            csv.append(String.format("%s;%s;%s;%s;%s;%s\n",
                    codigo, fecha, estado, tipo, cliente, total));
        }

        return new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
