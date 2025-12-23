package com.serveat.service.estadisticas.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.estadisticas.EstadisticasService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EstadisticasServiceImpl implements EstadisticasService {

    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;

    public EstadisticasServiceImpl(PedidoRepository pedidoRepository,
                                   PagoRepository pagoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pagoRepository = pagoRepository;
    }

    /* Devuelve el número total de pedidos registrados */
    @Override
    public long totalPedidos() {
        return pedidoRepository.count();
    }

    /* Devuelve el número de pedidos confirmados según la regla de negocio */
    @Override
    public long pedidosConfirmados() {
        return pedidoRepository.countByEstado(EstadoPedido.EN_COCINA);
    }

    /* Devuelve el número de pedidos cancelados */
    @Override
    public long pedidosCancelados() {
        return pedidoRepository.countByEstado(EstadoPedido.ANULADO);
    }

    /* Devuelve el número de pagos confirmados */
    @Override
    public long pagosConfirmados() {
        return pagoRepository.countByEstado(EstadoPago.CONFIRMADO);
    }

    /* Devuelve el importe total facturado sumando pagos confirmados */
    @Override
    public BigDecimal totalFacturado() {
        List<Pago> pagos = pagoRepository.findByEstado(EstadoPago.CONFIRMADO);
        return pagos.stream()
                .map(Pago::getImporte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /* Devuelve los años disponibles en función de los pedidos existentes */
    @Override
    public List<Integer> añosDisponibles() {
        List<Pedido> pedidos = pedidoRepository.findAllByOrderByFechaCreacionDesc();
        return pedidos.stream()
                .map(Pedido::getFechaCreacion)
                .filter(Objects::nonNull)
                .map(dt -> dt.getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /* Devuelve todos los meses disponibles para aplicar filtros */
    @Override
    public List<Month> mesesDisponibles() {
        return List.of(Month.values());
    }

    /* Devuelve el nombre del mes en español */
    @Override
    public String etiquetaMes(Month m) {
        if (m == null) return "-";
        return switch (m) {
            case JANUARY -> "Enero";
            case FEBRUARY -> "Febrero";
            case MARCH -> "Marzo";
            case APRIL -> "Abril";
            case MAY -> "Mayo";
            case JUNE -> "Junio";
            case JULY -> "Julio";
            case AUGUST -> "Agosto";
            case SEPTEMBER -> "Septiembre";
            case OCTOBER -> "Octubre";
            case NOVEMBER -> "Noviembre";
            case DECEMBER -> "Diciembre";
        };
    }

    /* Devuelve la etiqueta legible de un método de pago */
    @Override
    public String etiquetaMetodoPago(MetodoPago m) {
        if (m == null) return "-";
        return switch (m) {
            case TARJETA -> "Tarjeta";
            case PAYPAL -> "PayPal";
            case EFECTIVO -> "Efectivo";
        };
    }

    /* Devuelve la etiqueta legible del tipo de pedido */
    @Override
    public String etiquetaTipoPedido(TipoPedidoCliente t) {
        if (t == null) return "-";
        return switch (t) {
            case RECOGER -> "Recoger";
            case DOMICILIO -> "Domicilio";
        };
    }

    /* Devuelve la etiqueta legible del estado del pedido */
    @Override
    public String etiquetaEstadoPedido(EstadoPedido e) {
        if (e == null) return "-";
        return switch (e) {
            case EN_CURSO -> "En curso";
            case EN_COCINA -> "En cocina";
            case ANULADO -> "Anulado";
        };
    }

    /* Devuelve la etiqueta legible del estado de cocina */
    @Override
    public String etiquetaEstadoCocina(EstadoCocina e) {
        if (e == null) return "-";
        return switch (e) {
            case PENDIENTE_ACEPTACION -> "Pendiente aceptación";
            case ACEPTADO -> "Aceptado";
            case EN_PREPARACION -> "En preparación";
            case LISTO -> "Listo";
            case CANCELADO -> "Cancelado";
        };
    }

    /* Mensaje estándar a mostrar cuando no hay resultados */
    @Override
    public String mensajeSinResultados() {
        return "No hay resultados, selecciona otros filtros.";
    }

    /* Devuelve sugerencias de productos según prefijo y filtros aplicados */
    @Override
    public List<String> sugerirProductos(String prefix,
                                         Integer year, Month month,
                                         TipoPedidoCliente tipoPedido,
                                         MetodoPago metodoPago,
                                         EstadoPedido estadoPedido,
                                         EstadoCocina estadoCocina,
                                         int limit) {

        int lim = normalizarLimit(limit, 5, 25);
        String s = normalizar(prefix);

        List<Pedido> pedidos = cargarPedidosFiltrados(year, month, tipoPedido, estadoPedido, estadoCocina);

        Set<String> nombres = new HashSet<>();

        // Cache local para no repetir consultas por pedido
        Map<String, Optional<Pago>> pagoPorPedido = new HashMap<>();

        for (Pedido p : pedidos) {
            if (!cumpleMetodoPago(p, metodoPago, pagoPorPedido)) continue;

            if (p.getLineaPedidos() == null) continue;
            for (LineaPedido lp : p.getLineaPedidos()) {
                if (lp == null) continue;
                Producto prod = lp.getProducto();
                if (prod == null || prod.getNombre() == null) continue;

                String nombre = prod.getNombre();
                if (!s.isBlank() && !nombre.toLowerCase().contains(s)) continue;

                nombres.add(nombre);
            }
        }

        return nombres.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .limit(lim)
                .toList();
    }

    /* Devuelve el total de productos distintos en el ranking por unidades */
    @Override
    public long topProductosPorUnidadesCount(Integer year, Month month,
                                             TipoPedidoCliente tipoPedido,
                                             MetodoPago metodoPago,
                                             EstadoPedido estadoPedido,
                                             EstadoCocina estadoCocina,
                                             String productoExactoOrNull) {

        return construirMapaUnidades(year, month, tipoPedido, metodoPago, estadoPedido, estadoCocina, productoExactoOrNull).size();
    }

    /* Devuelve una página del ranking de productos por unidades vendidas */
    @Override
    public List<Map<String, Object>> topProductosPorUnidadesPage(Integer year, Month month,
                                                                 TipoPedidoCliente tipoPedido,
                                                                 MetodoPago metodoPago,
                                                                 EstadoPedido estadoPedido,
                                                                 EstadoCocina estadoCocina,
                                                                 String productoExactoOrNull,
                                                                 int offset, int limit) {

        Validaciones.validarPaginacion(offset, limit);

        Map<String, Long> unidades = construirMapaUnidades(
                year, month, tipoPedido, metodoPago, estadoPedido, estadoCocina, productoExactoOrNull
        );

        return unidades.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .skip(offset)
                .limit(limit)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("producto", e.getKey());
                    row.put("unidades", e.getValue());
                    return row;
                })
                .toList();
    }

    private Map<String, Long> construirMapaUnidades(Integer year, Month month,
                                                    TipoPedidoCliente tipoPedido,
                                                    MetodoPago metodoPago,
                                                    EstadoPedido estadoPedido,
                                                    EstadoCocina estadoCocina,
                                                    String productoExactoOrNull) {

        List<Pedido> pedidos = cargarPedidosFiltrados(year, month, tipoPedido, estadoPedido, estadoCocina);

        Map<String, Long> unidades = new HashMap<>();
        Map<String, Optional<Pago>> pagoPorPedido = new HashMap<>();

        String productoExacto = normalizar(productoExactoOrNull);

        for (Pedido p : pedidos) {
            if (!cumpleMetodoPago(p, metodoPago, pagoPorPedido)) continue;

            if (p.getLineaPedidos() == null) continue;
            for (LineaPedido lp : p.getLineaPedidos()) {
                if (lp == null) continue;

                Producto prod = lp.getProducto();
                if (prod == null || prod.getNombre() == null) continue;

                String nombre = prod.getNombre();

                // Filtro por producto exacto (si viene informado)
                if (!productoExacto.isBlank() && !nombre.equalsIgnoreCase(productoExacto)) continue;

                long qty = lp.getCantidad();
                if (qty <= 0) continue;

                unidades.merge(nombre, qty, Long::sum);
            }
        }

        return unidades;
    }

    /* Devuelve el total de productos distintos en el ranking por facturación */
    @Override
    public long topProductosPorFacturacionCount(Integer year, Month month,
                                                TipoPedidoCliente tipoPedido,
                                                MetodoPago metodoPago,
                                                EstadoPedido estadoPedido,
                                                EstadoCocina estadoCocina,
                                                String productoExactoOrNull) {

        return construirMapaFacturacion(year, month, tipoPedido, metodoPago, estadoPedido, estadoCocina, productoExactoOrNull).size();
    }

    /* Devuelve una página del ranking de productos por facturación */
    @Override
    public List<Map<String, Object>> topProductosPorFacturacionPage(Integer year, Month month,
                                                                    TipoPedidoCliente tipoPedido,
                                                                    MetodoPago metodoPago,
                                                                    EstadoPedido estadoPedido,
                                                                    EstadoCocina estadoCocina,
                                                                    String productoExactoOrNull,
                                                                    int offset, int limit) {

        Validaciones.validarPaginacion(offset, limit);

        Map<String, BigDecimal> total = construirMapaFacturacion(
                year, month, tipoPedido, metodoPago, estadoPedido, estadoCocina, productoExactoOrNull
        );

        return total.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .skip(offset)
                .limit(limit)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("producto", e.getKey());
                    row.put("total", e.getValue().setScale(2, RoundingMode.HALF_UP));
                    return row;
                })
                .toList();
    }

    private Map<String, BigDecimal> construirMapaFacturacion(Integer year, Month month,
                                                             TipoPedidoCliente tipoPedido,
                                                             MetodoPago metodoPago,
                                                             EstadoPedido estadoPedido,
                                                             EstadoCocina estadoCocina,
                                                             String productoExactoOrNull) {

        List<Pedido> pedidos = cargarPedidosFiltrados(year, month, tipoPedido, estadoPedido, estadoCocina);

        Map<String, BigDecimal> total = new HashMap<>();
        Map<String, Optional<Pago>> pagoPorPedido = new HashMap<>();

        String productoExacto = normalizar(productoExactoOrNull);

        for (Pedido p : pedidos) {
            if (!cumpleMetodoPago(p, metodoPago, pagoPorPedido)) continue;

            if (p.getLineaPedidos() == null) continue;
            for (LineaPedido lp : p.getLineaPedidos()) {
                if (lp == null) continue;

                Producto prod = lp.getProducto();
                if (prod == null || prod.getNombre() == null) continue;

                String nombre = prod.getNombre();

                // Filtro por producto exacto (si viene informado)
                if (!productoExacto.isBlank() && !nombre.equalsIgnoreCase(productoExacto)) continue;

                BigDecimal precio = prod.getPrecio() != null ? prod.getPrecio() : BigDecimal.ZERO;

                long qty = lp.getCantidad();
                if (qty <= 0) continue;

                BigDecimal linea = precio.multiply(BigDecimal.valueOf(qty));
                total.merge(nombre, linea, BigDecimal::add);
            }
        }

        return total;
    }

    /* Devuelve el conteo de pedidos agrupados por estado de cocina, aplicando filtros */
    @Override
    public Map<String, Long> resumenEstadosCocina(Integer year, Month month,
                                                  TipoPedidoCliente tipoPedido,
                                                  EstadoPedido estadoPedido) {

        List<Pedido> pedidos = cargarPedidosFiltrados(year, month, tipoPedido, estadoPedido, null);

        Map<String, Long> res = new LinkedHashMap<>();
        // Siempre devolvemos todos los estados (aunque sea 0) para que la vista no falle
        for (EstadoCocina ec : EstadoCocina.values()) {
            res.put(etiquetaEstadoCocina(ec), 0L);
        }

        Map<EstadoCocina, Long> conteo = pedidos.stream()
                .map(Pedido::getEstadoCocina)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(e -> e, Collectors.counting()));

        for (Map.Entry<EstadoCocina, Long> e : conteo.entrySet()) {
            res.put(etiquetaEstadoCocina(e.getKey()), e.getValue());
        }

        return res;
    }

    private List<Pedido> cargarPedidosFiltrados(Integer year, Month month,
                                                TipoPedidoCliente tipoPedido,
                                                EstadoPedido estadoPedido,
                                                EstadoCocina estadoCocina) {

        List<Pedido> pedidos = pedidoRepository.findAllByOrderByFechaCreacionDesc();

        LocalDate desde = null;
        LocalDate hasta = null;

        if (year != null && month != null) {
            YearMonth ym = YearMonth.of(year, month);
            desde = ym.atDay(1);
            hasta = ym.atEndOfMonth();
        }

        LocalDate finalDesde = desde;
        LocalDate finalHasta = hasta;

        return pedidos.stream()
                .filter(p -> {
                    if (finalDesde == null || finalHasta == null) return true;
                    if (p.getFechaCreacion() == null) return false;
                    LocalDate f = p.getFechaCreacion().toLocalDate();
                    return (!f.isBefore(finalDesde) && !f.isAfter(finalHasta));
                })
                .filter(p -> tipoPedido == null || p.getTipoPedido() == tipoPedido)
                .filter(p -> estadoPedido == null || p.getEstado() == estadoPedido)
                .filter(p -> estadoCocina == null || p.getEstadoCocina() == estadoCocina)
                .toList();
    }

    private boolean cumpleMetodoPago(Pedido pedido,
                                     MetodoPago metodoPago,
                                     Map<String, Optional<Pago>> cache) {
        if (metodoPago == null) return true;

        String codigo = pedido.getCodigo();
        Optional<Pago> pagoOpt = cache.computeIfAbsent(codigo, pagoRepository::findByPedido_Codigo);

        return pagoOpt.map(p -> p.getMetodo() == metodoPago).orElse(false);
    }

    private String normalizar(String s) {
        if (s == null) return "";
        String x = s.trim();
        return x.isBlank() ? "" : x.toLowerCase();
    }

    private int normalizarLimit(int limit, int min, int max) {
        if (limit <= 0) return min;
        if (limit < min) return min;
        return Math.min(limit, max);
    }

    private static class Validaciones {
        static void validarPaginacion(int offset, int limit) {
            if (offset < 0) throw new IllegalArgumentException("Offset inválido");
            if (limit <= 0) throw new IllegalArgumentException("Limit inválido");
            if (limit > 200) throw new IllegalArgumentException("Limit demasiado alto (máx 200)");
        }
    }
}