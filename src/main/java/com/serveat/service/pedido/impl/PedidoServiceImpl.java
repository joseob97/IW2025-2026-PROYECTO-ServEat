package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.pedido.PedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;

    public PedidoServiceImpl(PedidoRepository pedidoRepo,
                             ProductoRepository productoRepo) {
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
    }

    private String generarCodigo() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public Pedido crearPedido() {
        Pedido p = new Pedido();
        p.setCodigo(generarCodigo());
        p.setEstado("CREADO");

        return pedidoRepo.save(p);
    }

    @Override
    public Pedido obtenerPorCodigo(String codigo) {
        return pedidoRepo.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
    }

    @Override
    public Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad) {

        Pedido pedido = obtenerPorCodigo(codigoPedido);

        Producto producto = productoRepo.findByCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        pedido.crearLineaPedido(producto, cantidad);

        return pedidoRepo.save(pedido);
    }

    @Override
    public List<Pedido> buscarPorEstado(String estado) {
        return pedidoRepo.findByEstado(estado);
    }

    @Override
    public Pedido cambiarEstado(String codigoPedido, String nuevoEstado) {
        Pedido pedido = obtenerPorCodigo(codigoPedido);
        pedido.setEstado(nuevoEstado);
        return pedidoRepo.save(pedido);
    }

    @Override
    public void eliminarPedido(String codigoPedido) {
        Pedido pedido = obtenerPorCodigo(codigoPedido);
        pedidoRepo.delete(pedido);
    }
}