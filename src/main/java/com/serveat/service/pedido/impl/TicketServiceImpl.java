package com.serveat.service.pedido.impl;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.TicketService;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.seguridad.FeatureService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {

    private final PedidoRepository pedidoRepo;
    private final PedidoCalculoService calculoService;
    private final FeatureService featureService;

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat EUR_FMT = new DecimalFormat("0.00");

    public TicketServiceImpl(PedidoRepository pedidoRepo,
                             PedidoCalculoService calculoService,
                             FeatureService featureService) {
        this.pedidoRepo = pedidoRepo;
        this.calculoService = calculoService;
        this.featureService = featureService;
    }

    @Override
    public byte[] generarTicketCliente(String codigoPedido, String username) {
        validarFeatureActiva();

        if (codigoPedido == null || codigoPedido.isBlank()) {
            throw new IllegalArgumentException("Código de pedido inválido");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        Pedido pedido = pedidoRepo.findWithDetalleByCodigoAndCliente_Username(codigoPedido, username)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado o no pertenece al cliente"));

        return generarPdfTicket(pedido, "CLIENTE");
    }

    @Override
    public byte[] generarTicketCamarero(String codigoPedido) {
        validarFeatureActiva();

        if (codigoPedido == null || codigoPedido.isBlank()) {
            throw new IllegalArgumentException("Código de pedido inválido");
        }

        Pedido pedido = pedidoRepo.findWithDetalleByCodigo(codigoPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + codigoPedido));

        return generarPdfTicket(pedido, "CAMARERO");
    }

    private void validarFeatureActiva() {
        if (!featureService.tieneFeature(Feature.FACTURACION_TICKET)) {
            throw new IllegalStateException("Funcionalidad de ticket no disponible");
        }
    }

    private byte[] generarPdfTicket(Pedido pedido, String emisor) {
        if (pedido == null) throw new IllegalArgumentException("Pedido inválido");
        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }

        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            float margin = 50f;
            float y = page.getMediaBox().getHeight() - margin;
            float lineH = 14f;

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                y = escribirTitulo(cs, margin, y, "TICKET / FACTURA");
                y -= 6;

                y = escribirLinea(cs, margin, y, lineH, "Pedido: " + safe(pedido.getCodigo()));
                y = escribirLinea(cs, margin, y, lineH, "Fecha: " + (pedido.getFechaCreacion() != null ? pedido.getFechaCreacion().format(FECHA_FMT) : "-"));
                y = escribirLinea(cs, margin, y, lineH, "Emisor: " + emisor);

                if (pedido.getReservaMesa() != null) {
                    y = escribirLinea(cs, margin, y, lineH, "Mesa: " + pedido.getReservaMesa().getNumeroMesa());
                }
                if (pedido.getCliente() != null && pedido.getCliente().getUsername() != null) {
                    y = escribirLinea(cs, margin, y, lineH, "Cliente: " + pedido.getCliente().getUsername());
                }

                y -= 10;
                y = escribirSeparador(cs, margin, y, page.getMediaBox().getWidth() - margin);
                y -= 8;

                y = escribirCabeceraTabla(cs, margin, y);
                y -= 4;
                y = escribirSeparador(cs, margin, y, page.getMediaBox().getWidth() - margin);
                y -= 10;

                List<LineaPedido> lineas = new ArrayList<>(pedido.getLineaPedidos());
                lineas.sort(Comparator.comparing(LineaPedido::getCodigo, Comparator.nullsLast(String::compareToIgnoreCase)));

                BigDecimal total = BigDecimal.ZERO;

                for (LineaPedido lp : lineas) {
                    if (lp == null) continue;

                    String nombre = (lp.getProducto() != null && lp.getProducto().getNombre() != null)
                            ? lp.getProducto().getNombre()
                            : "-";

                    int cantidad = Math.max(lp.getCantidad(), 0);

                    BigDecimal unit = (lp.getPrecioUnitario() != null)
                            ? lp.getPrecioUnitario()
                            : (lp.getProducto() != null && lp.getProducto().getPrecio() != null ? lp.getProducto().getPrecio() : BigDecimal.ZERO);

                    BigDecimal subtotal = calculoService.calcularPrecioLinea(lp);
                    total = total.add(subtotal);

                    y = escribirFilaProducto(cs, margin, y, nombre, cantidad, unit, subtotal);

                    List<String> detallesIng = construirDetalleIngredientes(lp.getIngredientes());
                    for (String det : detallesIng) {
                        y = escribirLinea(cs, margin + 18, y, 12f, det);
                        if (y < 80f) {
                            cs.endText();
                            throw new IllegalStateException("Ticket demasiado largo para una sola página");
                        }
                    }

                    y -= 6;
                }

                y = escribirSeparador(cs, margin, y, page.getMediaBox().getWidth() - margin);
                y -= 12;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.newLineAtOffset(margin, y);
                cs.showText("TOTAL: " + eur(total) + " €");
                cs.endText();
            }

            doc.save(out);
            return out.toByteArray();

        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo generar el ticket", ex);
        }
    }

    private float escribirTitulo(PDPageContentStream cs, float x, float y, String titulo) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        cs.newLineAtOffset(x, y);
        cs.showText(titulo);
        cs.endText();
        return y - 24;
    }

    private float escribirLinea(PDPageContentStream cs, float x, float y, float lineH, String txt) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(txt);
        cs.endText();
        return y - lineH;
    }

    private float escribirSeparador(PDPageContentStream cs, float x1, float y, float x2) throws Exception {
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
        return y;
    }

    private float escribirCabeceraTabla(PDPageContentStream cs, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(padRight("Producto", 40) + padLeft("Cant", 6) + padLeft("Ud", 10) + padLeft("Subtotal", 12));
        cs.endText();
        return y - 14;
    }

    private float escribirFilaProducto(PDPageContentStream cs,
                                       float x,
                                       float y,
                                       String nombre,
                                       int cantidad,
                                       BigDecimal unit,
                                       BigDecimal subtotal) throws Exception {

        String fila = trunc(nombre, 40);
        String cant = String.valueOf(cantidad);
        String ud = eur(unit);
        String sub = eur(subtotal);

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 11);
        cs.newLineAtOffset(x, y);
        cs.showText(padRight(fila, 40) + padLeft(cant, 6) + padLeft(ud, 10) + padLeft(sub, 12));
        cs.endText();

        return y - 14;
    }

    private List<String> construirDetalleIngredientes(Collection<LineaPedidoIngrediente> ings) {
        if (ings == null || ings.isEmpty()) return List.of();

        List<LineaPedidoIngrediente> lista = new ArrayList<>(ings);
        lista.sort(Comparator.comparing(a -> a.getIngrediente() != null && a.getIngrediente().getNombre() != null
                ? a.getIngrediente().getNombre().toLowerCase(Locale.ROOT)
                : ""));

        List<String> res = new ArrayList<>();

        for (LineaPedidoIngrediente li : lista) {
            if (li == null || li.getIngrediente() == null) continue;
            String n = li.getIngrediente().getNombre();
            if (n == null || n.isBlank()) continue;

            if (!li.isIncluido()) {
                res.add("- Sin " + n);
            }
        }

        for (LineaPedidoIngrediente li : lista) {
            if (li == null || li.getIngrediente() == null) continue;

            int extraCant = Math.max(li.getExtraCantidad(), 0);
            if (extraCant <= 0) continue;

            String n = li.getIngrediente().getNombre();
            if (n == null || n.isBlank()) n = "Ingrediente";

            BigDecimal unit = li.getPrecioExtra() == null ? BigDecimal.ZERO : li.getPrecioExtra();
            BigDecimal plus = unit.multiply(BigDecimal.valueOf(extraCant));

            res.add("- Extra " + n + " x" + extraCant + " (+" + eur(plus) + " €)");
        }

        return res;
    }

    private String eur(BigDecimal v) {
        BigDecimal x = (v == null) ? BigDecimal.ZERO : v;
        return EUR_FMT.format(x);
    }

    private String safe(String s) {
        return (s == null) ? "-" : s;
    }

    private String trunc(String s, int max) {
        if (s == null) return "-";
        if (s.length() <= max) return s;
        return s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String padRight(String s, int len) {
        String x = (s == null) ? "" : s;
        if (x.length() >= len) return x;
        return x + " ".repeat(len - x.length());
    }

    private String padLeft(String s, int len) {
        String x = (s == null) ? "" : s;
        if (x.length() >= len) return x;
        return " ".repeat(len - x.length()) + x;
    }
}