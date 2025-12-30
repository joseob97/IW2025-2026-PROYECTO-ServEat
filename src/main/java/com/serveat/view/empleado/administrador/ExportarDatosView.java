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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.annotation.Secured;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "empleado/admin/exportar", layout = MainLayout.class)
@PageTitle("Exportar datos | Admin")
@Secured("ROLE_ADMIN")
public class ExportarDatosView extends VerticalLayout {

    private static final Logger logger = LoggerFactory.getLogger(ExportarDatosView.class);
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

        Paragraph descripcion = new Paragraph("Descarga el listado completo de pedidos en el formato que necesites.");
        
        // Botón CSV
        Button botonCsv = new Button("Descargar CSV");
        botonCsv.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        StreamResource resourceCsv = new StreamResource("pedidos.csv", this::generarCsvPedidos);
        resourceCsv.setContentType("text/csv");
        resourceCsv.setCacheTime(0);

        Anchor linkCsv = new Anchor(resourceCsv, "");
        linkCsv.getElement().setAttribute("download", true);
        linkCsv.add(botonCsv);

        // Botón PDF
        Button botonPdf = new Button("Descargar PDF");
        botonPdf.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        
        StreamResource resourcePdf = new StreamResource("pedidos.pdf", this::generarPdfPedidos);
        resourcePdf.setContentType("application/pdf");
        resourcePdf.setCacheTime(0);

        Anchor linkPdf = new Anchor(resourcePdf, "");
        linkPdf.getElement().setAttribute("download", true);
        linkPdf.add(botonPdf);

        HorizontalLayout botones = new HorizontalLayout(linkCsv, linkPdf);
        botones.setSpacing(true);

        add(titulo, descripcion, botones);
    }

    private java.io.InputStream generarCsvPedidos() {
        List<Pedido> pedidos = pedidoService.listarTodosOrdenadosPorFecha();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        StringBuilder csv = new StringBuilder();
        csv.append("Codigo;Fecha;Estado;Tipo;Cliente;Total\n");

        for (Pedido p : pedidos) {
            String codigo = p.getCodigo();
            String fecha = p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "";
            String estado = p.getEstado() != null ? p.getEstado().name() : "";
            String tipo = p.getTipoPedido() != null ? p.getTipoPedido().name() : "";
            String cliente = p.getCliente() != null ? p.getCliente().getUsername() : "Anonimo";
            String total = p.calcularPrecioTotal() != null ? p.calcularPrecioTotal().toString() : "0.00";

            csv.append(String.format("%s;%s;%s;%s;%s;%s\n",
                    codigo, fecha, estado, tipo, cliente, total));
        }

        return new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private java.io.InputStream generarPdfPedidos() {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("REPORTE DE PEDIDOS - SERVEAT");
                contentStream.endText();

                contentStream.setFont(PDType1Font.HELVETICA, 10);
                int y = 720;
                int margin = 50;
                int lineHeight = 15;

                List<Pedido> pedidos = pedidoService.listarTodosOrdenadosPorFecha();
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                for (Pedido p : pedidos) {
                    if (y < 50) { 
                        contentStream.close();
                        PDPage newPage = new PDPage();
                        document.addPage(newPage);
                        break; 
                    }

                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, y);
                    
                    String linea = String.format("Pedido: %s | Fecha: %s | Total: %s EUR",
                            p.getCodigo(),
                            p.getFechaCreacion() != null ? p.getFechaCreacion().format(fmt) : "-",
                            p.calcularPrecioTotal());
                    
                    contentStream.showText(linea);
                    contentStream.endText();
                    y -= lineHeight;
                    
                    contentStream.beginText();
                    contentStream.newLineAtOffset(margin, y);
                    contentStream.showText("Cliente: " + (p.getCliente() != null ? p.getCliente().getUsername() : "Anonimo") + 
                                         " | Estado: " + p.getEstado());
                    contentStream.endText();
                    y -= (lineHeight + 10);
                }
            }

            document.save(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            logger.error("Error al generar el PDF de pedidos", e);
            return new ByteArrayInputStream("Error al generar PDF".getBytes(StandardCharsets.UTF_8));
        }
    }
}
