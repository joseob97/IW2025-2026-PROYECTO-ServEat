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
}