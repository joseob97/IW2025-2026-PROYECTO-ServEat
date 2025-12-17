package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pedido.EstadoCocina;
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

    @Override
    public Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername) {
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("Debes indicar un motivo de cancelación");
        }
        if (camareroUsername == null || camareroUsername.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al camarero");
        }

        Pedido pedido = cargarDetalle(codigoPedido);

        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("El pedido ya está anulado");
        }

        boolean cancelable =
                pedido.getEstado() == EstadoPedido.EN_CURSO
                        || (pedido.getEstado() == EstadoPedido.EN_COCINA
                        && pedido.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION);

        if (!cancelable) {
            throw new IllegalArgumentException("No se puede cancelar: cocina ya ha aceptado o el pedido ya está en preparación");
        }

        pedido.setEstado(EstadoPedido.ANULADO);
        pedido.setCanceladoPor(camareroUsername);
        pedido.setMotivoCancelacion(motivo);
        pedido.setFechaCancelacion(java.time.LocalDateTime.now());

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public List<Pedido> listarPedidosModificables() {
        return pedidoRepo.findByEstadoOrEstadoAndEstadoCocina(
                EstadoPedido.EN_CURSO,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    @Override
    public List<Pedido> listarPedidosModificablesPorMesa(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) {
            throw new IllegalArgumentException("Número de mesa inválido");
        }

        return pedidoRepo.findByReservaMesa_NumeroMesaAndEstadoOrReservaMesa_NumeroMesaAndEstadoAndEstadoCocina(
                numeroMesa,
                EstadoPedido.EN_CURSO,
                numeroMesa,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    @Override
    public Pedido reemplazarPedido(String codigoPedidoOriginal,
                                   Pedido pedidoModificado,
                                   String usuario) {

        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("No se pudo identificar al usuario");
        }

        Pedido original = cargarDetalle(codigoPedidoOriginal);

        // Validar que se puede modificar
        boolean modificable =
                original.getEstado() == EstadoPedido.EN_CURSO
                        || (original.getEstado() == EstadoPedido.EN_COCINA
                        && original.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION);

        if (!modificable) {
            throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");
        }

        // Anular pedido original
        original.setEstado(EstadoPedido.ANULADO);
        original.setCanceladoPor(usuario);
        original.setMotivoCancelacion("Pedido modificado");
        original.setFechaCancelacion(java.time.LocalDateTime.now());
        pedidoRepo.save(original);

        // Crear pedido nuevo
        Pedido nuevo = new Pedido();
        nuevo.setCodigo(generarCodigo());
        nuevo.setEstado(EstadoPedido.EN_CURSO);
        nuevo.setReservaMesa(original.getReservaMesa());

        for (LineaPedido lp : pedidoModificado.getLineaPedidos()) {
            nuevo.getLineaPedidos().add(
                    new LineaPedido(
                            nuevo,
                            lp.getProducto(),
                            lp.getCantidad()
                    )
            );
        }

        if (nuevo.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }

        nuevo.marcarModificado(usuario);

        pedidoRepo.save(nuevo);
        return cargarDetalle(nuevo.getCodigo());
    }


}