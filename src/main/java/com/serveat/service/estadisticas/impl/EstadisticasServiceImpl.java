package com.serveat.service.estadisticas.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.estadisticas.EstadisticasService;
import com.serveat.service.estadisticas.EstadisticasSnapshot;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class EstadisticasServiceImpl implements EstadisticasService {

    private final PedidoRepository pedidoRepository;
    private final PagoRepository pagoRepository;

    public EstadisticasServiceImpl(PedidoRepository pedidoRepository, PagoRepository pagoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.pagoRepository = pagoRepository;
    }

    /* Obtiene un resumen agregado de KPIs para el rango indicado (con caché por rango). */
    @Override
    @Cacheable(cacheNames = "snapshot_rango", key = "{#desde,#hasta}")
    public EstadisticasSnapshot snapshotRango(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        List<Pedido> pedidos = cargarPedidosRango(desde, hasta);
        long totalP = pedidos.size();

        if (totalP == 0) {
            return EstadisticasSnapshot.vacio(desde, hasta);
        }

        long confirmados = pedidos.stream()
                .filter(p -> p.getEstado() == EstadoPedido.EN_COCINA)
                .count();

        long anulados = pedidos.stream()
                .filter(p -> p.getEstado() == EstadoPedido.ANULADO)
                .count();

        List<Pago> pagosConf = pagosConfirmadosEnRango(desde, hasta);
        long pagosConfirmados = pagosConf.size();

        BigDecimal fact = pagosConf.stream()
                .map(Pago::getImporte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new EstadisticasSnapshot(
                desde,
                hasta,
                true,
                totalP,
                confirmados,
                anulados,
                pagosConfirmados,
                fact
        );
    }

    /* Devuelve el ranking de productos por unidades vendidas en el rango (limitado). */
    @Override
    @Cacheable(cacheNames = "top_unidades_rango", key = "{#desde,#hasta,#limit}")
    public List<Map<String, Object>> topProductosPorUnidades(LocalDate desde, LocalDate hasta, int limit) {
        Validaciones.validarLimit(limit);

        Map<String, Long> mapa = construirMapaUnidades(desde, hasta);

        return mapa.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(limit)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("producto", e.getKey());
                    row.put("unidades", e.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    /* Devuelve el ranking de productos por facturación en el rango (limitado). */
    @Override
    @Cacheable(cacheNames = "top_facturacion_rango", key = "{#desde,#hasta,#limit}")
    public List<Map<String, Object>> topProductosPorFacturacion(LocalDate desde, LocalDate hasta, int limit) {
        Validaciones.validarLimit(limit);

        Map<String, BigDecimal> mapa = construirMapaFacturacion(desde, hasta);

        return mapa.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("producto", e.getKey());
                    row.put("total", (e.getValue() == null ? BigDecimal.ZERO : e.getValue()).setScale(2, RoundingMode.HALF_UP));
                    return row;
                })
                .collect(Collectors.toList());
    }

    /* Construye una serie mensual lista para representación: mes, valor y marca del máximo. */
    @Override
    @Cacheable(cacheNames = "serie_mensual_vista", key = "{#yearDesde,#yearHasta,#tipo}")
    public List<Map<String, Object>> serieMensualVista(int yearDesde, int yearHasta, String tipo) {
        if (yearDesde > yearHasta) throw new IllegalArgumentException("Rango de años inválido");
        if (tipo == null) tipo = "Unidades";

        final boolean esUnidades = "Unidades".equalsIgnoreCase(tipo);

        Map<YearMonth, BigDecimal> serie = esUnidades
                ? convertirUnidadesABigDecimal(serieMensualUnidades(yearDesde, yearHasta))
                : serieMensualFacturacion(yearDesde, yearHasta);

        if (serie.isEmpty()) return List.of();

        BigDecimal max = serie.values().stream()
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<YearMonth, BigDecimal> e : serie.entrySet()) {
            BigDecimal v = e.getValue() == null ? BigDecimal.ZERO : e.getValue();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("mes", etiquetaMes(e.getKey()));
            row.put("valor", v.setScale(2, RoundingMode.HALF_UP));
            row.put("max", v.compareTo(max) == 0 && max.compareTo(BigDecimal.ZERO) > 0);
            rows.add(row);
        }
        return rows;
    }

    /* Calcula la serie mensual de facturación (YearMonth -> total), inicializando meses sin datos a cero. */
    @Override
    @Cacheable(cacheNames = "serie_mensual_facturacion", key = "{#yearDesde,#yearHasta}")
    public Map<YearMonth, BigDecimal> serieMensualFacturacion(int yearDesde, int yearHasta) {
        if (yearDesde > yearHasta) throw new IllegalArgumentException("Rango de años inválido");

        Map<YearMonth, BigDecimal> serie = inicializarSerieFacturacion(yearDesde, yearHasta);

        LocalDate desde = LocalDate.of(yearDesde, 1, 1);
        LocalDate hasta = LocalDate.of(yearHasta, 12, 31);

        List<Pedido> pedidos = cargarPedidosRango(desde, hasta);

        for (Pedido p : pedidos) {
            if (p.getFechaCreacion() == null) continue;

            YearMonth ym = YearMonth.from(p.getFechaCreacion().toLocalDate());
            List<LineaPedido> lineas = p.getLineaPedidos();
            if (lineas == null) continue;

            BigDecimal totalMes = BigDecimal.ZERO;

            for (LineaPedido lp : lineas) {
                if (lp == null) continue;

                Producto prod = lp.getProducto();
                if (prod == null || prod.getNombre() == null) continue;

                BigDecimal precio = prod.getPrecio() == null ? BigDecimal.ZERO : prod.getPrecio();
                long qty = lp.getCantidad();
                if (qty <= 0) continue;

                totalMes = totalMes.add(precio.multiply(BigDecimal.valueOf(qty)));
            }

            serie.merge(ym, totalMes, BigDecimal::add);
        }

        serie.replaceAll((k, v) -> (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP));
        return serie;
    }

    /* Calcula la serie mensual de unidades (YearMonth -> unidades), inicializando meses sin datos a cero. */
    @Override
    @Cacheable(cacheNames = "serie_mensual_unidades", key = "{#yearDesde,#yearHasta}")
    public Map<YearMonth, Long> serieMensualUnidades(int yearDesde, int yearHasta) {
        if (yearDesde > yearHasta) throw new IllegalArgumentException("Rango de años inválido");

        Map<YearMonth, Long> serie = inicializarSerieUnidades(yearDesde, yearHasta);

        LocalDate desde = LocalDate.of(yearDesde, 1, 1);
        LocalDate hasta = LocalDate.of(yearHasta, 12, 31);

        List<Pedido> pedidos = cargarPedidosRango(desde, hasta);

        for (Pedido p : pedidos) {
            if (p.getFechaCreacion() == null) continue;

            YearMonth ym = YearMonth.from(p.getFechaCreacion().toLocalDate());
            List<LineaPedido> lineas = p.getLineaPedidos();
            if (lineas == null) continue;

            long unidadesMes = 0L;
            for (LineaPedido lp : lineas) {
                if (lp == null) continue;
                long qty = lp.getCantidad();
                if (qty <= 0) continue;
                unidadesMes += qty;
            }

            serie.merge(ym, unidadesMes, Long::sum);
        }

        return serie;
    }

    /* Devuelve la lista de años disponibles a partir de la fecha de creación de los pedidos. */
    @Override
    @Cacheable(cacheNames = "años_disponibles", key = "'all'")
    public List<Integer> añosDisponibles() {
        List<Pedido> pedidos = pedidoRepository.findAllByOrderByFechaCreacionDesc();
        if (pedidos == null || pedidos.isEmpty()) return List.of(LocalDate.now().getYear());

        TreeSet<Integer> years = new TreeSet<>();
        for (Pedido p : pedidos) {
            if (p.getFechaCreacion() == null) continue;
            years.add(p.getFechaCreacion().toLocalDate().getYear());
        }

        if (years.isEmpty()) years.add(LocalDate.now().getYear());
        return new ArrayList<>(years);
    }

    /* Mensaje estándar cuando no hay resultados para el criterio consultado. */
    @Override
    public String mensajeSinResultados() {
        return "No hay resultados.";
    }

    /* Invalida las cachés de estadísticas para forzar recálculo bajo demanda. */
    @Override
    @Async
    @CacheEvict(
            cacheNames = {
                    "snapshot_rango",
                    "top_unidades_rango",
                    "top_facturacion_rango",
                    "serie_mensual_vista",
                    "serie_mensual_facturacion",
                    "serie_mensual_unidades",
                    "años_disponibles"
            },
            allEntries = true
    )
    public void recalcularEstadisticasAsync() {
        /* Evicción asíncrona de caché. */
    }

    /* Valida la coherencia del rango de fechas. */
    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'Desde' no puede ser posterior a 'Hasta'.");
        }
    }

    /* Carga pedidos y aplica filtro por rango de fechas sobre la fecha de creación. */
    private List<Pedido> cargarPedidosRango(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        List<Pedido> pedidos = pedidoRepository.findAllByOrderByFechaCreacionDesc();

        return pedidos.stream()
                .filter(p -> p.getFechaCreacion() != null)
                .filter(p -> {
                    LocalDate f = p.getFechaCreacion().toLocalDate();
                    if (desde != null && f.isBefore(desde)) return false;
                    if (hasta != null && f.isAfter(hasta)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    /* Obtiene pagos confirmados y aplica filtro por rango usando la fecha del pedido asociado. */
    private List<Pago> pagosConfirmadosEnRango(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        List<Pago> pagos = pagoRepository.findByEstado(EstadoPago.CONFIRMADO);

        return pagos.stream()
                .filter(p -> p.getPedido() != null && p.getPedido().getFechaCreacion() != null)
                .filter(p -> {
                    LocalDate f = p.getPedido().getFechaCreacion().toLocalDate();
                    if (desde != null && f.isBefore(desde)) return false;
                    if (hasta != null && f.isAfter(hasta)) return false;
                    return true;
                })
                .collect(Collectors.toList());
    }

    /* Agrega unidades por producto en el rango indicado. */
    private Map<String, Long> construirMapaUnidades(LocalDate desde, LocalDate hasta) {
        List<Pedido> pedidos = cargarPedidosRango(desde, hasta);

        Map<String, Long> res = new HashMap<>();

        for (Pedido p : pedidos) {
            List<LineaPedido> lineas = p.getLineaPedidos();
            if (lineas == null) continue;

            for (LineaPedido lp : lineas) {
                if (lp == null) continue;

                Producto prod = lp.getProducto();
                if (prod == null || prod.getNombre() == null) continue;

                long qty = lp.getCantidad();
                if (qty <= 0) continue;

                res.merge(prod.getNombre(), qty, Long::sum);
            }
        }

        return res;
    }

    /* Agrega facturación por producto en el rango indicado. */
    private Map<String, BigDecimal> construirMapaFacturacion(LocalDate desde, LocalDate hasta) {
        List<Pedido> pedidos = cargarPedidosRango(desde, hasta);

        Map<String, BigDecimal> res = new HashMap<>();

        for (Pedido p : pedidos) {
            List<LineaPedido> lineas = p.getLineaPedidos();
            if (lineas == null) continue;

            for (LineaPedido lp : lineas) {
                if (lp == null) continue;

                Producto prod = lp.getProducto();
                if (prod == null || prod.getNombre() == null) continue;

                long qty = lp.getCantidad();
                if (qty <= 0) continue;

                BigDecimal precio = prod.getPrecio() == null ? BigDecimal.ZERO : prod.getPrecio();
                BigDecimal linea = precio.multiply(BigDecimal.valueOf(qty));

                res.merge(prod.getNombre(), linea, BigDecimal::add);
            }
        }

        return res;
    }

    /* Inicializa una serie mensual de facturación con meses del rango [yearDesde..yearHasta] a cero. */
    private Map<YearMonth, BigDecimal> inicializarSerieFacturacion(int yearDesde, int yearHasta) {
        Map<YearMonth, BigDecimal> serie = new LinkedHashMap<>();
        YearMonth it = YearMonth.of(yearDesde, 1);
        YearMonth end = YearMonth.of(yearHasta, 12);

        while (!it.isAfter(end)) {
            serie.put(it, BigDecimal.ZERO);
            it = it.plusMonths(1);
        }
        return serie;
    }

    /* Inicializa una serie mensual de unidades con meses del rango [yearDesde..yearHasta] a cero. */
    private Map<YearMonth, Long> inicializarSerieUnidades(int yearDesde, int yearHasta) {
        Map<YearMonth, Long> serie = new LinkedHashMap<>();
        YearMonth it = YearMonth.of(yearDesde, 1);
        YearMonth end = YearMonth.of(yearHasta, 12);

        while (!it.isAfter(end)) {
            serie.put(it, 0L);
            it = it.plusMonths(1);
        }
        return serie;
    }

    /* Convierte una serie mensual de unidades a BigDecimal para representación homogénea. */
    private Map<YearMonth, BigDecimal> convertirUnidadesABigDecimal(Map<YearMonth, Long> unidades) {
        Map<YearMonth, BigDecimal> res = new LinkedHashMap<>();
        for (Map.Entry<YearMonth, Long> e : unidades.entrySet()) {
            long v = e.getValue() == null ? 0L : e.getValue();
            res.put(e.getKey(), BigDecimal.valueOf(v));
        }
        return res;
    }

    /* Genera una etiqueta de mes en formato corto y localización española. */
    private String etiquetaMes(YearMonth ym) {
        Locale es = new Locale("es", "ES");
        String mes = ym.getMonth().getDisplayName(TextStyle.SHORT, es);
        return mes + " " + ym.getYear();
    }

    /* Validaciones de parámetros de entrada. */
    private static class Validaciones {
        static void validarLimit(int limit) {
            if (limit <= 0) throw new IllegalArgumentException("Limit inválido");
            if (limit > 200) throw new IllegalArgumentException("Limit demasiado alto (máx 200)");
        }
    }
}