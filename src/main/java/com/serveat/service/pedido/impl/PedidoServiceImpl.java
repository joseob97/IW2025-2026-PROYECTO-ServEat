package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.reserva.EstadoReservaMesa;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.repository.reserva.ReservaMesaRepository;
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
    private final ReservaMesaRepository reservaMesaRepo;

    public PedidoServiceImpl(PedidoRepository pedidoRepo,
                             ProductoRepository productoRepo,
                             ReservaMesaRepository reservaMesaRepo) {
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
        this.reservaMesaRepo = reservaMesaRepo;
    }

    private String generarCodigo() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Pedido cargarDetalle(String codigo) {
        return pedidoRepo.findWithDetalleByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + codigo));
    }

    @Override
    public Pedido crearPedido() {
        Pedido p = new Pedido();
        p.setCodigo(generarCodigo());
        p.setEstado(EstadoPedido.EN_CURSO);
        pedidoRepo.save(p);
        return cargarDetalle(p.getCodigo());
    }

    @Override
    public Pedido crearPedidoMesa(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) {
            throw new IllegalArgumentException("Número de mesa inválido");
        }

        ReservaMesa mesa = reservaMesaRepo
                .findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA)
                .orElseGet(() -> reservaMesaRepo.save(new ReservaMesa(numeroMesa)));

        Pedido p = new Pedido();
        p.setCodigo(generarCodigo());
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setReservaMesa(mesa);

        pedidoRepo.save(p);
        return cargarDetalle(p.getCodigo());
    }

    @Override
    public Pedido obtenerPorCodigo(String codigo) {
        return cargarDetalle(codigo);
    }

    @Override
    public Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad) {
        if (cantidad <= 0) throw new IllegalArgumentException("La cantidad debe ser > 0");

        Pedido pedido = cargarDetalle(codigoPedido);

        Producto producto = productoRepo.findByCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + codigoProducto));

        // Si ya existe línea para ese producto => incrementa
        LineaPedido existente = pedido.getLineaPedidos().stream()
                .filter(lp -> lp.getProducto().getCodigo().equals(producto.getCodigo()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            // necesitas setter en LineaPedido:
            // public void setCantidad(int c) { this.cantidad=c; }
            existente.setCantidad(existente.getCantidad() + cantidad);
        } else {
            pedido.getLineaPedidos().add(new LineaPedido(pedido, producto, cantidad));
        }

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad) {
        Pedido pedido = cargarDetalle(codigoPedido);

        LineaPedido lp = pedido.getLineaPedidos().stream()
                .filter(l -> l.getProducto().getCodigo().equals(codigoProducto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ese producto no está en el pedido"));

        if (nuevaCantidad <= 0) {
            // orphanRemoval=true => se borra en BD
            pedido.getLineaPedidos().remove(lp);
        } else {
            lp.setCantidad(nuevaCantidad);
        }

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido eliminarProducto(String codigoPedido, String codigoProducto) {
        Pedido pedido = cargarDetalle(codigoPedido);
        boolean removed = pedido.getLineaPedidos().removeIf(l -> l.getProducto().getCodigo().equals(codigoProducto));

        if (!removed) {
            throw new IllegalArgumentException("Ese producto no está en el pedido");
        }

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido confirmarPedido(String codigoPedido) {
        Pedido pedido = cargarDetalle(codigoPedido);

        if (pedido.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("No se puede confirmar un pedido vacío");
        }

        pedido.setEstado(EstadoPedido.EN_COCINA);

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        return pedidoRepo.findByEstado(estado);
    }

    @Override
    public Pedido cambiarEstado(String codigoPedido, EstadoPedido nuevoEstado) {
        Pedido pedido = cargarDetalle(codigoPedido);
        pedido.setEstado(nuevoEstado);
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public void eliminarPedido(String codigoPedido) {
        Pedido pedido = pedidoRepo.findByCodigo(codigoPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + codigoPedido));
        pedidoRepo.delete(pedido);
    }

    @Override
    public List<Pedido> listarPedidos() {
        return pedidoRepo.findAll();
    }
}