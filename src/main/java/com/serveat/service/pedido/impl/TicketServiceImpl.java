package com.serveat.service.pedido.impl;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.seguridad.Feature;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.TicketService;
import com.serveat.service.seguridad.FeatureService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
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

    // Fuente
    private static final PDType1Font FONT = PDType1Font.HELVETICA;
    private static final PDType1Font FONT_BOLD = PDType1Font.HELVETICA_BOLD;

    private static final float FONT_SIZE = 11f;
    private static final float FONT_SIZE_ING = 10f;

    // Columnas (X fijas)
    // A4 width ~ 595. Ajusta si quieres.
    private static final float COL_PROD = 50f;
    private static final float COL_CANT = 380f;   // alineación derecha
    private static final float COL_UD   = 445f;   // alineación derecha
    private static final float COL_SUB  = 545f;   // alineación derecha

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

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // Título
                y = escribirTitulo(cs, margin, y, "TICKET / FACTURA");
                y -= 6;

                // Cabecera
                y = escribirLinea(cs, margin, y, 14f, "Pedido: " + safe(pedido.getCodigo()));
                y = escribirLinea(cs, margin, y, 14f, "Fecha: " + (pedido.getFechaCreacion() != null ? pedido.getFechaCreacion().format(FECHA_FMT) : "-"));
                y = escribirLinea(cs, margin, y, 14f, "Emisor: " + emisor);

                if (pedido.getReservaMesa() != null) {
                    y = escribirLinea(cs, margin, y, 14f, "Mesa: " + pedido.getReservaMesa().getNumeroMesa());
                }
                if (pedido.getCliente() != null && pedido.getCliente().getUsername() != null) {
                    y = escribirLinea(cs, margin, y, 14f, "Cliente: " + pedido.getCliente().getUsername());
                }

                y -= 10;
                y = escribirSeparador(cs, margin, y, page.getMediaBox().getWidth() - margin);
                y -= 12;

                // Cabecera tabla (con columnas reales)
                y = escribirCabeceraTabla(cs, y);
                y -= 6;
                y = escribirSeparador(cs, margin, y, page.getMediaBox().getWidth() - margin);
                y -= 12;

                // Líneas
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

                    y = escribirFilaProducto(cs, y, nombre, cantidad, unit, subtotal);

                    // Ingredientes (indentado fijo)
                    List<String> detallesIng = construirDetalleIngredientes(lp.getIngredientes());
                    for (String det : detallesIng) {
                        drawText(cs, FONT, FONT_SIZE_ING, COL_PROD + 18, y, det);
                        y -= 13f;

                        if (y < 80f) {
                            throw new IllegalStateException("Ticket demasiado largo para una sola página");
                        }
                    }

                    y -= 6;
                }

                y = escribirSeparador(cs, margin, y, page.getMediaBox().getWidth() - margin);
                y -= 16;

                drawText(cs, FONT_BOLD, 12f, margin, y, "TOTAL: " + eur(total) + " €");
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
        drawText(cs, FONT_BOLD, 18f, x, y, titulo);
        return y - 24f;
    }

    private float escribirLinea(PDPageContentStream cs, float x, float y, float lineH, String txt) throws Exception {
        drawText(cs, FONT, 11f, x, y, txt);
        return y - lineH;
    }

    private float escribirSeparador(PDPageContentStream cs, float x1, float y, float x2) throws Exception {
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
        return y;
    }

    private float escribirCabeceraTabla(PDPageContentStream cs, float y) throws Exception {
        drawText(cs, FONT_BOLD, FONT_SIZE, COL_PROD, y, "Producto");
        drawTextRight(cs, FONT_BOLD, FONT_SIZE, COL_CANT, y, "Cant");
        drawTextRight(cs, FONT_BOLD, FONT_SIZE, COL_UD, y, "Ud");
        drawTextRight(cs, FONT_BOLD, FONT_SIZE, COL_SUB, y, "Subtotal");
        return y - 16f;
    }

    private float escribirFilaProducto(PDPageContentStream cs,
                                       float y,
                                       String nombre,
                                       int cantidad,
                                       BigDecimal unit,
                                       BigDecimal subtotal) throws Exception {

        float maxNombre = (COL_CANT - 12f) - COL_PROD;
        String prod = truncToWidth(FONT, FONT_SIZE, (nombre == null ? "-" : nombre), maxNombre);

        drawText(cs, FONT, FONT_SIZE, COL_PROD, y, prod);
        drawTextRight(cs, FONT, FONT_SIZE, COL_CANT, y, String.valueOf(Math.max(cantidad, 0)));
        drawTextRight(cs, FONT, FONT_SIZE, COL_UD, y, eur(unit));
        drawTextRight(cs, FONT, FONT_SIZE, COL_SUB, y, eur(subtotal));

        return y - 16f;
    }

    private float textWidth(PDType1Font font, float size, String text) throws Exception {
        if (text == null) return 0f;
        return font.getStringWidth(text) / 1000f * size;
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text) throws Exception {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text == null ? "" : text);
        cs.endText();
    }

    private void drawTextRight(PDPageContentStream cs, PDType1Font font, float size, float rightX, float y, String text) throws Exception {
        String t = text == null ? "" : text;
        float w = textWidth(font, size, t);
        drawText(cs, font, size, rightX - w, y, t);
    }

    private String truncToWidth(PDType1Font font, float size, String text, float maxWidth) throws Exception {
        if (text == null) return "-";
        if (textWidth(font, size, text) <= maxWidth) return text;

        String ell = "…";
        float ellW = textWidth(font, size, ell);

        String s = text;
        while (!s.isEmpty() && (textWidth(font, size, s) + ellW) > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s.isEmpty() ? ell : s + ell;
    }

    // ====== INGREDIENTES ======

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

            if (!li.isIncluido()) res.add("- Sin " + n);
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
}