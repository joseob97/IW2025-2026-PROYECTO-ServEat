package com.serveat.service.impl;

import com.serveat.domain.menu.Plato;
import com.serveat.domain.pedido.DetallePedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.DetallePedidoRepository;
import com.serveat.repository.PedidoRepository;
import com.serveat.service.PedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detallePedidoRepository;

    public PedidoServiceImpl(PedidoRepository pedidoRepository,
                             DetallePedidoRepository detallePedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    @Override
    public Pedido crearPedido(Cliente cliente) {
        return new Pedido(cliente);
    }

    @Override
    public Pedido agregarPlato(Pedido pedido, Plato plato, int cantidad) {
        DetallePedido detalle = new DetallePedido(plato, cantidad);
        pedido.addDetalle(detalle);
        return pedido;
    }

    @Override
    public Pedido guardarPedido(Pedido pedido) {
        // Primero guardamos el pedido (con cascade = ALL ya guarda los detalles)
        return pedidoRepository.save(pedido);
    }

    @Override
    public Pedido obtenerPedidoPorId(UUID id) {
        Optional<Pedido> pedidoOpt = pedidoRepository.findById(id);
        return pedidoOpt.orElse(null);
    }

    @Override
    public List<Pedido> obtenerPedidosDeCliente(Cliente cliente) {
        return pedidoRepository.findByCliente(cliente);
    }
}
