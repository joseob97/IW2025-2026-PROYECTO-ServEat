package com.serveat.service.estadisticas.impl;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.estadisticas.EstadisticasService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    @Override
    public long totalPedidos() {
        return pedidoRepository.count();
    }

    @Override
    public long pedidosConfirmados() {
        return pedidoRepository.countByEstado(EstadoPedido.EN_COCINA);
    }

    @Override
    public long pedidosCancelados() {
        return pedidoRepository.countByEstado(EstadoPedido.ANULADO);
    }

    @Override
    public long pagosConfirmados() {
        return pagoRepository.countByEstado(EstadoPago.CONFIRMADO);
    }

    @Override
    public BigDecimal totalFacturado() {
        List<Pago> pagos = pagoRepository.findByEstado(EstadoPago.CONFIRMADO);
        return pagos.stream()
                .map(Pago::getImporte)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public List<Map<String, Object>> topProductosPorUnidades(int limit) {
        int lim = normalizarLimit(limit);

        List<Pedido> pedidos = pedidoRepository.findByEstado(EstadoPedido.EN_COCINA);

        Map<String, Long> unidadesPorProducto = new HashMap<>();

        for (Pedido p : pedidos) {
            if (p.getLineaPedidos() == null) continue;
            for (LineaPedido lp : p.getLineaPedidos()) {
                if (lp == null || lp.getProducto() == null) continue;
                String nombre = lp.getProducto().getNombre();
                long cant = lp.getCantidad();
                unidadesPorProducto.merge(nombre, cant, Long::sum);
            }
        }

        return unidadesPorProducto.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(lim)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("producto", e.getKey());
                    row.put("unidades", e.getValue());
                    return row;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> topProductosPorFacturacion(int limit) {
        int lim = normalizarLimit(limit);

        List<Pedido> pedidos = pedidoRepository.findByEstado(EstadoPedido.EN_COCINA);

        Map<String, BigDecimal> totalPorProducto = new HashMap<>();

        for (Pedido p : pedidos) {
            if (p.getLineaPedidos() == null) continue;
            for (LineaPedido lp : p.getLineaPedidos()) {
                if (lp == null || lp.getProducto() == null) continue;
                String nombre = lp.getProducto().getNombre();

                BigDecimal precio = lp.getProducto().getPrecio();
                if (precio == null) precio = BigDecimal.ZERO;

                BigDecimal totalLinea = precio.multiply(BigDecimal.valueOf(lp.getCantidad()));
                totalPorProducto.merge(nombre, totalLinea, BigDecimal::add);
            }
        }

        return totalPorProducto.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(lim)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("producto", e.getKey());
                    row.put("total", e.getValue().setScale(2, RoundingMode.HALF_UP));
                    return row;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> resumenEstadosCocina() {
        Map<String, Long> res = new LinkedHashMap<>();
        for (EstadoCocina ec : EstadoCocina.values()) {
            res.put(ec.name(), pedidoRepository.countByEstadoCocina(ec));
        }
        return res;
    }

    private int normalizarLimit(int limit) {
        if (limit <= 0) return 10;
        return Math.min(limit, 50);
    }
}