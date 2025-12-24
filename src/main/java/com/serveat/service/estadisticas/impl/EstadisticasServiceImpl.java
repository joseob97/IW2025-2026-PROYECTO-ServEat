package com.serveat.service.estadisticas.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.*;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.estadisticas.EstadisticasService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
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

    /* kpi cacheados */

    @Override
    @Cacheable("kpi_total_pedidos")
    public long totalPedidos() {
        return pedidoRepository.count();
    }

    @Override
    @Cacheable("kpi_pedidos_confirmados")
    public long pedidosConfirmados() {
        return pedidoRepository.countByEstado(EstadoPedido.EN_COCINA);
    }

    @Override
    @Cacheable("kpi_pedidos_cancelados")
    public long pedidosCancelados() {
        return pedidoRepository.countByEstado(EstadoPedido.ANULADO);
    }

    @Override
    @Cacheable("kpi_pagos_confirmados")
    public long pagosConfirmados() {
        return pagoRepository.countByEstado(EstadoPago.CONFIRMADO);
    }

    @Override
    @Cacheable("kpi_total_facturado")
    public BigDecimal totalFacturado() {
        return pagoRepository.findByEstado(EstadoPago.CONFIRMADO).stream()
                .map(Pago::getImporte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /* Filtros UI */

    @Override
    public List<Integer> añosDisponibles() {
        return pedidoRepository.findAllByOrderByFechaCreacionDesc().stream()
                .map(Pedido::getFechaCreacion)
                .filter(Objects::nonNull)
                .map(d -> d.getYear())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    @Override
    public List<Month> mesesDisponibles() {
        return List.of(Month.values());
    }

    @Override
    public String etiquetaMes(Month m) {
        return m == null ? "-" : m.getDisplayName(java.time.format.TextStyle.FULL, new Locale("es"));
    }

    @Override
    public String etiquetaMetodoPago(MetodoPago m) {
        return m == null ? "-" : m.name();
    }

    @Override
    public String etiquetaTipoPedido(TipoPedidoCliente t) {
        return t == null ? "-" : t.name();
    }

    @Override
    public String etiquetaEstadoPedido(EstadoPedido e) {
        return e == null ? "-" : e.name();
    }

    @Override
    public String etiquetaEstadoCocina(EstadoCocina e) {
        return e == null ? "-" : e.name();
    }

    @Override
    public String mensajeSinResultados() {
        return "No hay resultados, selecciona otros filtros.";
    }

    /* El autocompletado de productos, tambien es cacheable */
    @Override
    @Cacheable(value = "autocomplete_productos", key = "{#prefix,#year,#month,#tipoPedido,#metodoPago,#estadoPedido,#estadoCocina}")
    public List<String> sugerirProductos(String prefix,
                                         Integer year, Month month,
                                         TipoPedidoCliente tipoPedido,
                                         MetodoPago metodoPago,
                                         EstadoPedido estadoPedido,
                                         EstadoCocina estadoCocina,
                                         int limit) {

        String f = prefix == null ? "" : prefix.toLowerCase();

        return cargarPedidosFiltrados(year, month, tipoPedido, estadoPedido, estadoCocina).stream()
                .flatMap(p -> p.getLineaPedidos().stream())
                .map(lp -> lp.getProducto().getNombre())
                .filter(n -> n.toLowerCase().contains(f))
                .distinct()
                .limit(limit)
                .toList();
    }

    /* Tops se implementan con Lazy y Cache */

    @Override
    @Cacheable("top_unidades_count")
    public long topProductosPorUnidadesCount(Integer year, Month month,
                                             TipoPedidoCliente tipoPedido,
                                             MetodoPago metodoPago,
                                             EstadoPedido estadoPedido,
                                             EstadoCocina estadoCocina,
                                             String productoExactoOrNull) {
        return construirMapaUnidades(year, month, tipoPedido, metodoPago, estadoPedido, estadoCocina, productoExactoOrNull).size();
    }

    @Override
    public List<Map<String, Object>> topProductosPorUnidadesPage(Integer year, Month month,
                                                                 TipoPedidoCliente tipoPedido,
                                                                 MetodoPago metodoPago,
                                                                 EstadoPedido estadoPedido,
                                                                 EstadoCocina estadoCocina,
                                                                 String productoExactoOrNull,
                                                                 int offset, int limit) {

        return construirMapaUnidades(year, month, tipoPedido, metodoPago, estadoPedido, estadoCocina, productoExactoOrNull)
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .skip(offset)
                .limit(limit)
                .map(e -> Map.of("producto", e.getKey(), "unidades", e.getValue()))
                .toList();
    }

    /*  Operaciones pesadas con Async
    * Tareas en segundo plano de forma asincrona */

    @Async
    @CacheEvict(cacheNames = {
            "kpi_total_pedidos",
            "kpi_pedidos_confirmados",
            "kpi_pedidos_cancelados",
            "kpi_pagos_confirmados",
            "kpi_total_facturado",
            "top_unidades_count",
            "autocomplete_productos"
    }, allEntries = true)
    public void recalcularEstadisticasAsync() {
        // Se ejecuta en background, no bloquea UI
    }

    /* Se incluye el filtrado base */

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
        } else if (year != null) {
            desde = LocalDate.of(year, 1, 1);
            hasta = LocalDate.of(year, 12, 31);
        }

        LocalDate d = desde;
        LocalDate h = hasta;

        return pedidos.stream()
                .filter(p -> {
                    if (d == null || h == null) return true;
                    LocalDate f = p.getFechaCreacion().toLocalDate();
                    return !f.isBefore(d) && !f.isAfter(h);
                })
                .filter(p -> tipoPedido == null || p.getTipoPedido() == tipoPedido)
                .filter(p -> estadoPedido == null || p.getEstado() == estadoPedido)
                .filter(p -> estadoCocina == null || p.getEstadoCocina() == estadoCocina)
                .toList();
    }

    private Map<String, Long> construirMapaUnidades(Integer year, Month month,
                                                    TipoPedidoCliente tipoPedido,
                                                    MetodoPago metodoPago,
                                                    EstadoPedido estadoPedido,
                                                    EstadoCocina estadoCocina,
                                                    String productoExacto) {

        Map<String, Long> res = new HashMap<>();

        for (Pedido p : cargarPedidosFiltrados(year, month, tipoPedido, estadoPedido, estadoCocina)) {
            for (LineaPedido lp : p.getLineaPedidos()) {
                String nombre = lp.getProducto().getNombre();
                if (productoExacto != null && !productoExacto.equalsIgnoreCase(nombre)) continue;
                res.merge(nombre, (long) lp.getCantidad(), Long::sum);
            }
        }
        return res;
    }
}