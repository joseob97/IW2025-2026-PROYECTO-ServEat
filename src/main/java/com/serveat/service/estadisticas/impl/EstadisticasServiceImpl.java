package com.serveat.service.estadisticas.impl;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.estadisticas.EstadisticasService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
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
                .filter(i -> i != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}