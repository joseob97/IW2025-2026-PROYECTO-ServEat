package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.*;
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

    private void validarCarrito(Pedido carrito) {
        if (carrito == null || carrito.getLineaPedidos() == null || carrito.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }
    }

    private Cliente cargarCliente(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }
        return clienteRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    private ReservaMesa cargarOMesaAbierta(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) {
            throw new IllegalArgumentException("Número de mesa inválido");
        }
        return reservaMesaRepo
                .findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA)
                .orElseGet(() -> reservaMesaRepo.save(new ReservaMesa(numeroMesa)));
    }

    // ------------------- Empleados / backoffice -------------------

    @Override
    public Pedido crearPedidoMesa(Integer numeroMesa) {

        ReservaMesa mesa = cargarOMesaAbierta(numeroMesa);

        Pedido p = new Pedido();
        p.setCodigo(generarCodigo());
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        p.setReservaMesa(mesa);
        p.setTipoPedido(TipoPedidoCliente.MESA);
        p.setEstadoReparto(EstadoReparto.NO_APLICA);
        p.setDireccionEntrega(null);

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
    public List<Pedido> listarTodosOrdenadosPorFecha() {
        return pedidoRepo.findAllByOrderByFechaCreacionDesc();
    }

    @Override
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        return pedidoRepo.findByEstado(estado);
    }

    @Override
    public List<Pedido> obtenerPedidosPorEstado(EstadoCocina estado) {
        return pedidoRepo.findByEstadoCocina(estado);
    }

    @Override
    public List<Pedido> obtenerPedidosPorMesa(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) {
            throw new IllegalArgumentException("Número de mesa inválido");
        }
        return pedidoRepo.findByReservaMesa_NumeroMesaOrderByFechaCreacionDesc(numeroMesa);
    }

    @Override
    public List<Pedido> obtenerPedidosPorEstadoYMesa(EstadoCocina estado, Integer numeroMesa) {
        if (estado == null) throw new IllegalArgumentException("El estado no puede ser nulo");
        if (numeroMesa == null || numeroMesa <= 0) throw new IllegalArgumentException("Número de mesa inválido");
        return pedidoRepo.findByEstadoCocinaAndReservaMesa_NumeroMesaOrderByFechaCreacionDesc(estado, numeroMesa);
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

    // ------------------- Carrito (memoria) -------------------

    @Override
    public Pedido agregarProductoEnMemoria(Pedido pedido, Producto producto, int cantidad) {
        if (pedido == null) throw new IllegalArgumentException("Pedido inválido");
        if (producto == null) throw new IllegalArgumentException("Producto inválido");
        if (cantidad <= 0) throw new IllegalArgumentException("Cantidad inválida");

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
        if (pedido == null) throw new IllegalArgumentException("Pedido inválido");
        if (codigoProducto == null || codigoProducto.isBlank()) throw new IllegalArgumentException("Producto inválido");

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
        if (pedido == null) throw new IllegalArgumentException("Pedido inválido");
        if (codigoProducto == null || codigoProducto.isBlank()) throw new IllegalArgumentException("Producto inválido");

        boolean removed = pedido.getLineaPedidos()
                .removeIf(l -> l.getProducto().getCodigo().equals(codigoProducto));

        if (!removed) throw new IllegalArgumentException("Producto no está en el pedido");
        return pedido;
    }

    // ------------------- Confirmaciones -------------------

    @Override
    public Pedido confirmarPedido(String codigoPedido) {
        Pedido pedido = cargarDetalle(codigoPedido);

        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("No se puede confirmar un pedido vacío");
        }

        pedido.setEstado(EstadoPedido.EN_COCINA);
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario) {

        if (pedidoEditado == null) throw new IllegalArgumentException("Pedido inválido");
        if (pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty())
            throw new IllegalArgumentException("El pedido no puede quedar vacío");

        boolean modificable =
                pedidoEditado.getEstado() == EstadoPedido.EN_CURSO
                        || (pedidoEditado.getEstado() == EstadoPedido.EN_COCINA
                        && pedidoEditado.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION);

        if (!modificable) throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");

        pedidoEditado.marcarModificado(usuario);

        pedidoRepo.save(pedidoEditado);
        return cargarDetalle(pedidoEditado.getCodigo());
    }

    @Override
    public Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username) {

        if (pedidoEditado == null || pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }

        Pedido actual = cargarDetalleCliente(pedidoEditado.getCodigo(), username);

        if (actual.getEstado() == EstadoPedido.ANULADO) throw new IllegalArgumentException("Pedido anulado");
        if (actual.getEstadoCocina() != EstadoCocina.PENDIENTE_ACEPTACION)
            throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");

        actual.getLineaPedidos().clear();
        for (LineaPedido lp : pedidoEditado.getLineaPedidos()) {
            actual.getLineaPedidos().add(new LineaPedido(actual, lp.getProducto(), lp.getCantidad()));
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

        if (!cancelable) throw new IllegalArgumentException("No se puede cancelar el pedido");

        pedido.setEstado(EstadoPedido.ANULADO);
        pedido.setEstadoCocina(EstadoCocina.CANCELADO);
        pedido.setCanceladoPor(camareroUsername);
        pedido.setMotivoCancelacion(motivo);
        pedido.setFechaCancelacion(LocalDateTime.now());

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public boolean puedeModificarCliente(Pedido pedido) {
        if (pedido == null) return false;
        if (pedido.getEstado() == EstadoPedido.ANULADO) return false;
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
    public List<Pedido> listarPedidosCliente(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario inválido");
        return pedidoRepo.findByCliente_UsernameOrderByFechaCreacionDesc(username);
    }

    // ------------------- Cliente: creación clara -------------------

    @Override
    public Pedido crearPedidoClienteRecoger(Pedido carrito, String username) {
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.RECOGER, null, null, false);
    }

    @Override
    public Pedido crearPedidoClienteDomicilio(Pedido carrito, String username, String direccionEntrega) {
        if (direccionEntrega == null || direccionEntrega.trim().isBlank()) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria");
        }
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.DOMICILIO, direccionEntrega.trim(), null, false);
    }

    @Override
    public Pedido crearPedidoClienteMesa(Pedido carrito, String username, Integer numeroMesa) {
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.MESA, null, numeroMesa, true);
    }

    private Pedido crearPedidoClienteBase(Pedido carrito,
                                          String username,
                                          TipoPedidoCliente tipo,
                                          String direccionEntrega,
                                          Integer numeroMesa,
                                          boolean enviarDirectoACocina) {

        validarCarrito(carrito);
        Cliente cliente = cargarCliente(username);

        Pedido nuevo = new Pedido();
        nuevo.setCodigo(generarCodigo());
        nuevo.setCliente(cliente);
        nuevo.setTipoPedido(tipo);

        if (tipo == TipoPedidoCliente.DOMICILIO) {
            nuevo.setDireccionEntrega(direccionEntrega);
            // reparto aplicará; lo pondrá cocina al marcar LISTO (tu lógica actual)
        } else {
            nuevo.setDireccionEntrega(null);
            nuevo.setEstadoReparto(EstadoReparto.NO_APLICA);
        }

        if (tipo == TipoPedidoCliente.MESA) {
            ReservaMesa mesa = cargarOMesaAbierta(numeroMesa);
            nuevo.setReservaMesa(mesa);
        }

        // Estados iniciales
        if (enviarDirectoACocina) {
            nuevo.setEstado(EstadoPedido.EN_COCINA);
        } else {
            // recoger/domicilio: se crea y pasa a cocina al confirmar pago
            nuevo.setEstado(EstadoPedido.EN_CURSO);
        }
        nuevo.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);

        for (LineaPedido lp : carrito.getLineaPedidos()) {
            nuevo.getLineaPedidos().add(new LineaPedido(nuevo, lp.getProducto(), lp.getCantidad()));
        }

        pedidoRepo.save(nuevo);
        return cargarDetalle(nuevo.getCodigo());
    }

    // ------------------- Pago online -------------------

    @Override
    public Pago iniciarPagoOnline(Pedido carrito, String username, MetodoPago metodo) {
        if (metodo == null) throw new IllegalArgumentException("Método de pago inválido");
        // Si quieres soportar domicilio, la pasarela llamará al método correcto antes de pago
        Pedido pedidoCreado = crearPedidoClienteRecoger(carrito, username);
        return pagoService.iniciarPago(pedidoCreado, metodo);
    }

    @Override
    public Pago obtenerPagoCliente(Long pagoId, String username) {
        if (pagoId == null) throw new IllegalArgumentException("Pago inválido");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario inválido");

        Pago pago = pagoRepo.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        Pedido pedido = pago.getPedido();
        if (pedido == null || pedido.getCliente() == null || !username.equals(pedido.getCliente().getUsername())) {
            throw new IllegalArgumentException("Pago no pertenece al cliente");
        }
        return pago;
    }

    @Override
    public Pedido confirmarPagoOnline(Long pagoId, String username, String referencia) {
        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) throw new IllegalArgumentException("El pago ya está confirmado");
        if (pago.getEstado() == EstadoPago.FALLIDO) throw new IllegalArgumentException("El pago está marcado como fallido");

        Pago confirmado = pagoService.confirmarPago(pago.getId(), referencia);

        Pedido pedido = confirmado.getPedido();
        if (pedido.getEstado() == EstadoPedido.ANULADO) throw new IllegalArgumentException("Pedido anulado");
        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty())
            throw new IllegalArgumentException("El pedido no puede estar vacío");

        pedido.setEstado(EstadoPedido.EN_COCINA);
        pedidoRepo.save(pedido);

        return cargarDetalleCliente(pedido.getCodigo(), username);
    }

    @Override
    public Pedido marcarPagoOnlineFallido(Long pagoId, String username, String motivo) {
        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) throw new IllegalArgumentException("El pago ya está confirmado");

        String m = (motivo == null || motivo.isBlank()) ? "Cancelado por el cliente" : motivo.trim();
        pagoService.marcarPagoFallido(pago.getId(), m);

        Pedido pedido = pago.getPedido();
        return cargarDetalleCliente(pedido.getCodigo(), username);
    }

    // ------------------- Cocina -------------------

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPedidoPorId(UUID id) {
        return pedidoRepo.findWithDetalleById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + id));
    }

    @Override
    public Pedido cambiarEstadoCocina(UUID id, EstadoCocina nuevoEstado) {
        Pedido pedido = obtenerPedidoPorId(id);

        if (nuevoEstado == null) {
            throw new IllegalArgumentException("Estado de cocina inválido");
        }

        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("No se puede modificar un pedido anulado");
        }

        if (pedido.getEstadoCocina() == nuevoEstado) {
            return pedido;
        }

        /* La cocina siempre trabaja sobre pedidos en flujo de cocina */
        if (pedido.getEstado() != EstadoPedido.EN_COCINA) {
            pedido.setEstado(EstadoPedido.EN_COCINA);
        }

        pedido.setEstadoCocina(nuevoEstado);

        /* Al marcar LISTO: domicilio pasa a reparto, resto NO_APLICA */
        if (nuevoEstado == EstadoCocina.LISTO) {
            if (pedido.getTipoPedido() == TipoPedidoCliente.DOMICILIO) {
                pedido.setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);
            } else {
                pedido.setEstadoReparto(EstadoReparto.NO_APLICA);
            }
        }

        /* Si se cancela desde cocina, se anula el pedido completo */
        if (nuevoEstado == EstadoCocina.CANCELADO) {
            pedido.setEstado(EstadoPedido.ANULADO);
            pedido.setEstadoReparto(EstadoReparto.NO_APLICA);
        }

        pedido.marcarModificado("COCINERO");
        return pedidoRepo.save(pedido);
    }
}