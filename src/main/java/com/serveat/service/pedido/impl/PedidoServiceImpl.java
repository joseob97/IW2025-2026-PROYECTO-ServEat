package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.reserva.EstadoReservaMesa;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.repository.reserva.ReservaMesaRepository;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.service.pago.PagoService;
import com.serveat.service.pedido.PedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PedidoServiceImpl implements PedidoService {

    private final PedidoRepository pedidoRepo;
    private final ProductoRepository productoRepo;
    private final ReservaMesaRepository reservaMesaRepo;
    private final ClienteRepository clienteRepo;
    private final PagoService pagoService;
    private final PagoRepository pagoRepo;

    public PedidoServiceImpl(PedidoRepository pedidoRepo,
                             ProductoRepository productoRepo,
                             ReservaMesaRepository reservaMesaRepo,
                             ClienteRepository clienteRepo,
                             PagoService pagoService,
                             PagoRepository pagoRepo) {
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
        this.reservaMesaRepo = reservaMesaRepo;
        this.clienteRepo = clienteRepo;
        this.pagoService = pagoService;
        this.pagoRepo = pagoRepo;
    }

    private String generarCodigo() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Pedido cargarDetalle(String codigo) {
        return pedidoRepo.findWithDetalleByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + codigo));
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
    public List<Pedido> listarPedidos() {
        return pedidoRepo.findAll();
    }

    @Override
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        return pedidoRepo.findByEstado(estado);
    }

    @Override
    public Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad) {
        Pedido pedido = cargarDetalle(codigoPedido);
        Producto producto = productoRepo.findByCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        agregarProductoEnMemoria(pedido, producto, cantidad);

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad) {
        Pedido pedido = cargarDetalle(codigoPedido);
        actualizarCantidadEnMemoria(pedido, codigoProducto, nuevaCantidad);
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido eliminarProducto(String codigoPedido, String codigoProducto) {
        Pedido pedido = cargarDetalle(codigoPedido);
        eliminarProductoEnMemoria(pedido, codigoProducto);
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido agregarProductoEnMemoria(Pedido pedido, Producto producto, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("Cantidad inválida");
        }

        LineaPedido existente = pedido.getLineaPedidos().stream()
                .filter(lp -> lp.getProducto().getCodigo().equals(producto.getCodigo()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + cantidad);
        } else {
            pedido.getLineaPedidos().add(new LineaPedido(pedido, producto, cantidad));
        }

        return pedido;
    }

    @Override
    public Pedido actualizarCantidadEnMemoria(Pedido pedido, String codigoProducto, int nuevaCantidad) {
        LineaPedido lp = pedido.getLineaPedidos().stream()
                .filter(l -> l.getProducto().getCodigo().equals(codigoProducto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Producto no está en el pedido"));

        if (nuevaCantidad <= 0) {
            pedido.getLineaPedidos().remove(lp);
        } else {
            lp.setCantidad(nuevaCantidad);
        }

        return pedido;
    }

    @Override
    public Pedido eliminarProductoEnMemoria(Pedido pedido, String codigoProducto) {
        boolean removed = pedido.getLineaPedidos()
                .removeIf(l -> l.getProducto().getCodigo().equals(codigoProducto));

        if (!removed) {
            throw new IllegalArgumentException("Producto no está en el pedido");
        }

        return pedido;
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
    public Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario) {

        if (pedidoEditado.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }

        boolean modificable =
                pedidoEditado.getEstado() == EstadoPedido.EN_CURSO
                        || (pedidoEditado.getEstado() == EstadoPedido.EN_COCINA
                        && pedidoEditado.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION);

        if (!modificable) {
            throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");
        }

        pedidoEditado.marcarModificado(usuario);

        pedidoRepo.save(pedidoEditado);
        return cargarDetalle(pedidoEditado.getCodigo());
    }

    @Override
    public Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username) {

        if (pedidoEditado == null || pedidoEditado.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }

        Pedido actual = cargarDetalleCliente(pedidoEditado.getCodigo(), username);

        if (actual.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("Pedido anulado");
        }

        if (actual.getEstadoCocina() != EstadoCocina.PENDIENTE_ACEPTACION) {
            throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");
        }

        actual.getLineaPedidos().clear();

        for (LineaPedido lp : pedidoEditado.getLineaPedidos()) {
            actual.getLineaPedidos().add(new LineaPedido(
                    actual,
                    lp.getProducto(),
                    lp.getCantidad()
            ));
        }

        actual.marcarModificado(username);

        pedidoRepo.save(actual);
        return cargarDetalle(actual.getCodigo());
    }

    @Override
    public Pedido cargarDetalleCliente(String codigo, String username) {
        return pedidoRepo.findWithDetalleByCodigoAndCliente_Username(codigo, username)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado o no pertenece al cliente"));
    }

    @Override
    public Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername) {

        Pedido pedido = cargarDetalle(codigoPedido);

        boolean cancelable =
                pedido.getEstado() == EstadoPedido.EN_CURSO
                        || (pedido.getEstado() == EstadoPedido.EN_COCINA
                        && pedido.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION);

        if (!cancelable) {
            throw new IllegalArgumentException("No se puede cancelar el pedido");
        }

        pedido.setEstado(EstadoPedido.ANULADO);
        pedido.setCanceladoPor(camareroUsername);
        pedido.setMotivoCancelacion(motivo);
        pedido.setFechaCancelacion(LocalDateTime.now());

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public boolean puedeModificarCliente(Pedido pedido) {

        if (pedido == null) {
            return false;
        }

        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            return false;
        }

        return pedido.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION;
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
        return pedidoRepo.findByReservaMesa_NumeroMesaAndEstadoOrReservaMesa_NumeroMesaAndEstadoAndEstadoCocina(
                numeroMesa,
                EstadoPedido.EN_CURSO,
                numeroMesa,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    @Override
    public Pedido crearPedidoDesdeCliente(Pedido pedidoEnMemoria, String username) {

        if (pedidoEnMemoria == null || pedidoEnMemoria.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }

        Cliente cliente = clienteRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Pedido nuevo = new Pedido();
        nuevo.setCodigo(generarCodigo());
        nuevo.setEstado(EstadoPedido.EN_CURSO);
        nuevo.setCliente(cliente);

        for (LineaPedido lp : pedidoEnMemoria.getLineaPedidos()) {
            nuevo.getLineaPedidos().add(
                    new LineaPedido(nuevo, lp.getProducto(), lp.getCantidad())
            );
        }

        pedidoRepo.save(nuevo);
        return cargarDetalle(nuevo.getCodigo());
    }

    @Override
    public List<Pedido> listarPedidosCliente(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }
        return pedidoRepo.findByCliente_UsernameOrderByFechaCreacionDesc(username);
    }

    @Override
    public Pedido crearPedidoMesaDesdeCliente(Pedido pedidoEnMemoria, Integer numeroMesa, String username) {

        if (numeroMesa == null || numeroMesa <= 0) {
            throw new IllegalArgumentException("Número de mesa inválido");
        }

        if (pedidoEnMemoria == null || pedidoEnMemoria.getLineaPedidos() == null || pedidoEnMemoria.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        Cliente cliente = clienteRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        ReservaMesa mesa = reservaMesaRepo
                .findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA)
                .orElseGet(() -> reservaMesaRepo.save(new ReservaMesa(numeroMesa)));

        Pedido nuevo = new Pedido();
        nuevo.setCodigo(generarCodigo());
        nuevo.setEstado(EstadoPedido.EN_CURSO);
        nuevo.setReservaMesa(mesa);
        nuevo.setCliente(cliente);

        for (LineaPedido lp : pedidoEnMemoria.getLineaPedidos()) {
            nuevo.getLineaPedidos().add(new LineaPedido(
                    nuevo,
                    lp.getProducto(),
                    lp.getCantidad()
            ));
        }

        pedidoRepo.save(nuevo);
        return cargarDetalle(nuevo.getCodigo());
    }

    @Override
    public Pago iniciarPagoOnline(Pedido carrito, String username, MetodoPago metodo) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }
        if (carrito == null || carrito.getLineaPedidos() == null || carrito.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }
        if (metodo == null) {
            throw new IllegalArgumentException("Método de pago inválido");
        }

        Pedido pedidoCreado = crearPedidoDesdeCliente(carrito, username);
        return pagoService.iniciarPago(pedidoCreado, metodo);
    }

    @Override
    public Pago obtenerPagoCliente(Long pagoId, String username) {
        if (pagoId == null) {
            throw new IllegalArgumentException("Pago inválido");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        Pago pago = pagoRepo.findWithPedidoById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        if (pago.getPedido() == null || pago.getPedido().getCliente() == null
                || !username.equals(pago.getPedido().getCliente().getUsername())) {
            throw new IllegalArgumentException("Pago no pertenece al cliente");
        }

        return pago;
    }

    @Override
    public Pedido confirmarPagoOnline(Long pagoId, String username, String referencia) {
        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) {
            throw new IllegalArgumentException("El pago ya está confirmado");
        }
        if (pago.getEstado() == EstadoPago.FALLIDO) {
            throw new IllegalArgumentException("El pago está marcado como fallido");
        }

        Pago confirmado = pagoService.confirmarPago(pago.getId(), referencia);

        Pedido pedido = confirmado.getPedido();
        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("Pedido anulado");
        }

        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }

        pedido.setEstado(EstadoPedido.EN_COCINA);
        pedidoRepo.save(pedido);

        return cargarDetalleCliente(pedido.getCodigo(), username);
    }

    @Override
    public Pedido marcarPagoOnlineFallido(Long pagoId, String username, String motivo) {
        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) {
            throw new IllegalArgumentException("El pago ya está confirmado");
        }

        pagoService.marcarPagoFallido(pago.getId(), (motivo == null || motivo.isBlank()) ? "Cancelado por el cliente" : motivo.trim());

        Pedido pedido = pago.getPedido();
        return cargarDetalleCliente(pedido.getCodigo(), username);
    }
}