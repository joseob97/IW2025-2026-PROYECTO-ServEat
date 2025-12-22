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

    // Crea un pedido asociado a una mesa abierta.
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
        p.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        p.setReservaMesa(mesa);

        pedidoRepo.save(p);
        return cargarDetalle(p.getCodigo());
    }

    // Devuelve un pedido por su código con detalle.
    @Override
    public Pedido obtenerPorCodigo(String codigo) {
        return cargarDetalle(codigo);
    }

    // Devuelve todos los pedidos.
    @Override
    public List<Pedido> listarPedidos() {
        return pedidoRepo.findAll();
    }

    // Devuelve pedidos filtrados por estado.
    @Override
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        return pedidoRepo.findByEstado(estado);
    }

    // Añade un producto a un pedido persistido.
    @Override
    public Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad) {
        Pedido pedido = cargarDetalle(codigoPedido);
        Producto producto = productoRepo.findByCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        agregarProductoEnMemoria(pedido, producto, cantidad);

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    // Actualiza cantidad de un producto en un pedido persistido.
    @Override
    public Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad) {
        Pedido pedido = cargarDetalle(codigoPedido);
        actualizarCantidadEnMemoria(pedido, codigoProducto, nuevaCantidad);
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    // Elimina un producto de un pedido persistido.
    @Override
    public Pedido eliminarProducto(String codigoPedido, String codigoProducto) {
        Pedido pedido = cargarDetalle(codigoPedido);
        eliminarProductoEnMemoria(pedido, codigoProducto);
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    // Añade un producto a un pedido en memoria (carrito).
    @Override
    public Pedido agregarProductoEnMemoria(Pedido pedido, Producto producto, int cantidad) {
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (producto == null) {
            throw new IllegalArgumentException("Producto inválido");
        }
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

    // Actualiza cantidad en un pedido en memoria (carrito).
    @Override
    public Pedido actualizarCantidadEnMemoria(Pedido pedido, String codigoProducto, int nuevaCantidad) {
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (codigoProducto == null || codigoProducto.isBlank()) {
            throw new IllegalArgumentException("Producto inválido");
        }

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

    // Elimina un producto de un pedido en memoria (carrito).
    @Override
    public Pedido eliminarProductoEnMemoria(Pedido pedido, String codigoProducto) {
        if (pedido == null) {
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (codigoProducto == null || codigoProducto.isBlank()) {
            throw new IllegalArgumentException("Producto inválido");
        }

        boolean removed = pedido.getLineaPedidos()
                .removeIf(l -> l.getProducto().getCodigo().equals(codigoProducto));

        if (!removed) {
            throw new IllegalArgumentException("Producto no está en el pedido");
        }

        return pedido;
    }

    // Confirma un pedido para enviarlo a cocina.
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

    // Persiste cambios en un pedido (camarero).
    @Override
    public Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario) {

        if (pedidoEditado == null) {
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
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

    // Persiste cambios en un pedido del cliente si es modificable.
    @Override
    public Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username) {

        if (pedidoEditado == null || pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
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

    // Carga el pedido de un cliente validando pertenencia.
    @Override
    public Pedido cargarDetalleCliente(String codigo, String username) {
        return pedidoRepo.findWithDetalleByCodigoAndCliente_Username(codigo, username)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado o no pertenece al cliente"));
    }

    // Cancela un pedido si el estado lo permite.
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
        pedido.setEstadoCocina(EstadoCocina.CANCELADO);
        pedido.setCanceladoPor(camareroUsername);
        pedido.setMotivoCancelacion(motivo);
        pedido.setFechaCancelacion(LocalDateTime.now());

        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    // Indica si un pedido es modificable por el cliente.
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

    // Lista pedidos modificables por regla de negocio.
    @Override
    public List<Pedido> listarPedidosModificables() {
        return pedidoRepo.findByEstadoOrEstadoAndEstadoCocina(
                EstadoPedido.EN_CURSO,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    // Lista pedidos modificables filtrando por mesa.
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

    // Crea un pedido desde el carrito del cliente (sin pago).
    @Override
    public Pedido crearPedidoDesdeCliente(Pedido pedidoEnMemoria, String username) {

        if (pedidoEnMemoria == null || pedidoEnMemoria.getLineaPedidos() == null || pedidoEnMemoria.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }

        Cliente cliente = clienteRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Pedido nuevo = new Pedido();
        nuevo.setCodigo(generarCodigo());
        nuevo.setEstado(EstadoPedido.EN_CURSO);
        nuevo.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        nuevo.setCliente(cliente);

        for (LineaPedido lp : pedidoEnMemoria.getLineaPedidos()) {
            nuevo.getLineaPedidos().add(
                    new LineaPedido(nuevo, lp.getProducto(), lp.getCantidad())
            );
        }

        pedidoRepo.save(nuevo);
        return cargarDetalle(nuevo.getCodigo());
    }

    // Lista pedidos de un cliente ordenados por fecha.
    @Override
    public List<Pedido> listarPedidosCliente(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }
        return pedidoRepo.findByCliente_UsernameOrderByFechaCreacionDesc(username);
    }

    // Crea un pedido en mesa desde el carrito del cliente.
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
        nuevo.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
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

    // Inicia un pago pendiente creando el pedido del carrito.
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

    // Devuelve un pago validando que pertenece al cliente.
    @Override
    public Pago obtenerPagoCliente(Long pagoId, String username) {
        if (pagoId == null) {
            throw new IllegalArgumentException("Pago inválido");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        Pago pago = pagoRepo.findById(pagoId)
                .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado"));

        Pedido pedido = pago.getPedido();
        if (pedido == null || pedido.getCliente() == null || !username.equals(pedido.getCliente().getUsername())) {
            throw new IllegalArgumentException("Pago no pertenece al cliente");
        }

        return pago;
    }

    // Confirma el pago y envía el pedido a cocina.
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

    // Marca el pago como fallido (sin confirmar pedido).
    @Override
    public Pedido marcarPagoOnlineFallido(Long pagoId, String username, String motivo) {
        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) {
            throw new IllegalArgumentException("El pago ya está confirmado");
        }

        String m = (motivo == null || motivo.isBlank()) ? "Cancelado por el cliente" : motivo.trim();
        pagoService.marcarPagoFallido(pago.getId(), m);

        Pedido pedido = pago.getPedido();
        return cargarDetalleCliente(pedido.getCodigo(), username);
    }
}