package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.domain.reserva.EstadoReservaMesa;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.domain.usuario.Cliente;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.repository.reserva.ReservaMesaRepository;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.service.caja.CierreCajaService;
import com.serveat.service.pago.PagoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
    private final PedidoCarritoService carritoService;
    private final CierreCajaService cierreCajaService; // NUEVO

    public PedidoServiceImpl(PedidoRepository pedidoRepo,
                             ProductoRepository productoRepo,
                             ReservaMesaRepository reservaMesaRepo,
                             ClienteRepository clienteRepo,
                             PagoService pagoService,
                             PagoRepository pagoRepo,
                             PedidoCarritoService carritoService,
                             CierreCajaService cierreCajaService) { // NUEVO
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
        this.reservaMesaRepo = reservaMesaRepo;
        this.clienteRepo = clienteRepo;
        this.pagoService = pagoService;
        this.pagoRepo = pagoRepo;
        this.carritoService = carritoService;
        this.cierreCajaService = cierreCajaService; // NUEVO
    }

    /* Helpers */

    private void validarCajaAbierta() {
        if (cierreCajaService.isCajaCerrada(LocalDate.now())) {
            throw new IllegalStateException("No se pueden realizar pedidos: la caja del día está cerrada.");
        }
    }

    private String generarCodigo() {
        return "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private Pedido cargarDetalle(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("Código de pedido inválido");
        return pedidoRepo.findWithDetalleByCodigo(codigo)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado: " + codigo));
    }

    private void validarCarritoNoVacio(Pedido carrito) {
        if (carrito == null || carrito.getLineaPedidos() == null || carrito.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }
    }

    private Cliente cargarCliente(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario inválido");
        return clienteRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
    }

    private ReservaMesa cargarOMesaAbierta(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) throw new IllegalArgumentException("Número de mesa inválido");

        return reservaMesaRepo
                .findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA)
                .orElseGet(() -> reservaMesaRepo.save(new ReservaMesa(numeroMesa)));
    }

    private void marcarModificado(Pedido pedido, String username) {
        if (pedido == null) return;
        pedido.setModificadoPor(username);
        pedido.setFechaUltimaModificacion(LocalDateTime.now());
    }

    /* Empleados / backoffice */

    @Override
    public Pedido crearPedidoMesa(Integer numeroMesa) {
        validarCajaAbierta(); // NUEVO
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
    @Transactional(readOnly = true)
    public Pedido obtenerPorCodigo(String codigo) {
        return cargarDetalle(codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidos() {
        return pedidoRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarTodosOrdenadosPorFecha() {
        return pedidoRepo.findAllByOrderByFechaCreacionDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        if (estado == null) throw new IllegalArgumentException("Estado inválido");
        return pedidoRepo.findByEstado(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorEstado(EstadoCocina estado) {
        if (estado == null) throw new IllegalArgumentException("Estado de cocina inválido");
        return pedidoRepo.findByEstadoCocina(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorMesa(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) throw new IllegalArgumentException("Número de mesa inválido");
        return pedidoRepo.findByReservaMesa_NumeroMesaOrderByFechaCreacionDesc(numeroMesa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorEstadoYMesa(EstadoCocina estado, Integer numeroMesa) {
        if (estado == null) throw new IllegalArgumentException("El estado no puede ser nulo");
        if (numeroMesa == null || numeroMesa <= 0) throw new IllegalArgumentException("Número de mesa inválido");
        return pedidoRepo.findByEstadoCocinaAndReservaMesa_NumeroMesaOrderByFechaCreacionDesc(estado, numeroMesa);
    }

    /* Persistencia simple (por producto). Para personalizaciones, usar codigoLinea. */

    @Override
    public Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad) {
        validarCajaAbierta(); // NUEVO
        if (codigoProducto == null || codigoProducto.isBlank()) throw new IllegalArgumentException("Producto inválido");
        if (cantidad <= 0) throw new IllegalArgumentException("Cantidad inválida");

        Pedido pedido = cargarDetalle(codigoPedido);
        Producto producto = productoRepo.findByCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        carritoService.agregarProducto(pedido, producto, cantidad);

        marcarModificado(pedido, "SISTEMA");
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad) {
        validarCajaAbierta(); // NUEVO
        if (codigoProducto == null || codigoProducto.isBlank()) throw new IllegalArgumentException("Producto inválido");

        Pedido pedido = cargarDetalle(codigoPedido);

        LineaPedido lp = pedido.getLineaPedidos().stream()
                .filter(l -> l.getProducto() != null
                        && l.getProducto().getCodigo() != null
                        && l.getProducto().getCodigo().equals(codigoProducto))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Producto no está en el pedido"));

        if (nuevaCantidad <= 0) {
            pedido.getLineaPedidos().remove(lp);
        } else {
            lp.setCantidad(nuevaCantidad);
        }

        marcarModificado(pedido, "SISTEMA");
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido eliminarProducto(String codigoPedido, String codigoProducto) {
        validarCajaAbierta(); // NUEVO
        if (codigoProducto == null || codigoProducto.isBlank()) throw new IllegalArgumentException("Producto inválido");

        Pedido pedido = cargarDetalle(codigoPedido);

        boolean removed = pedido.getLineaPedidos().removeIf(l ->
                l.getProducto() != null
                        && l.getProducto().getCodigo() != null
                        && l.getProducto().getCodigo().equals(codigoProducto)
        );

        if (!removed) throw new IllegalArgumentException("Producto no está en el pedido");

        marcarModificado(pedido, "SISTEMA");
        pedidoRepo.save(pedido);
        return cargarDetalle(codigoPedido);
    }

    /* Confirmaciones / edición */

    @Override
    public Pedido confirmarPedido(String codigoPedido) {
        validarCajaAbierta(); // NUEVO
        Pedido pedido = cargarDetalle(codigoPedido);

        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("No se puede confirmar un pedido vacío");
        }

        pedido.setEstado(EstadoPedido.EN_COCINA);
        marcarModificado(pedido, "SISTEMA");
        pedidoRepo.save(pedido);

        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario) {
        validarCajaAbierta(); // NUEVO
        if (pedidoEditado == null) throw new IllegalArgumentException("Pedido inválido");
        if (pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }

        boolean modificable =
                pedidoEditado.getEstado() == EstadoPedido.EN_CURSO
                        || (pedidoEditado.getEstado() == EstadoPedido.EN_COCINA
                        && pedidoEditado.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION);

        if (!modificable) throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");

        marcarModificado(pedidoEditado, usuario);
        pedidoRepo.save(pedidoEditado);
        return cargarDetalle(pedidoEditado.getCodigo());
    }

    @Override
    public Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username) {
        validarCajaAbierta(); // NUEVO
        if (pedidoEditado == null || pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }

        Pedido actual = cargarDetalleCliente(pedidoEditado.getCodigo(), username);

        if (actual.getEstado() == EstadoPedido.ANULADO) throw new IllegalArgumentException("Pedido anulado");
        if (actual.getEstadoCocina() != EstadoCocina.PENDIENTE_ACEPTACION) {
            throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");
        }

        actual.getLineaPedidos().clear();

        for (LineaPedido lp : pedidoEditado.getLineaPedidos()) {
            if (lp.getProducto() == null || lp.getProducto().getCodigo() == null) continue;

            Producto producto = productoRepo.findByCodigo(lp.getProducto().getCodigo())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + lp.getProducto().getCodigo()));

            LineaPedido nueva = new LineaPedido(actual, producto, lp.getCantidad());

            if (lp.getIngredientes() != null && !lp.getIngredientes().isEmpty()) {
                for (LineaPedidoIngrediente sel : lp.getIngredientes()) {
                    if (sel.getIngrediente() == null) continue;

                    nueva.getIngredientes().add(new LineaPedidoIngrediente(
                            nueva,
                            sel.getIngrediente(),
                            sel.isIncluido(),
                            sel.getExtraCantidad(),
                            sel.getPrecioExtra()
                    ));
                }
            }

            actual.getLineaPedidos().add(nueva);
        }

        marcarModificado(actual, username);
        pedidoRepo.save(actual);
        return cargarDetalle(actual.getCodigo());
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido cargarDetalleCliente(String codigo, String username) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("Código inválido");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario inválido");

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

        marcarModificado(pedido, camareroUsername);
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
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosModificables() {
        return pedidoRepo.findByEstadoOrEstadoAndEstadoCocina(
                EstadoPedido.EN_CURSO,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosModificablesPorMesa(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) throw new IllegalArgumentException("Número de mesa inválido");

        return pedidoRepo.findByReservaMesa_NumeroMesaAndEstadoOrReservaMesa_NumeroMesaAndEstadoAndEstadoCocina(
                numeroMesa,
                EstadoPedido.EN_CURSO,
                numeroMesa,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosCliente(String username) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario inválido");
        return pedidoRepo.findByCliente_UsernameOrderByFechaCreacionDesc(username);
    }

    /* Cliente: creación */

    @Override
    public Pedido crearPedidoClienteRecoger(Pedido carrito, String username) {
        validarCajaAbierta(); // NUEVO
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.RECOGER, null, null, false);
    }

    @Override
    public Pedido crearPedidoClienteDomicilio(Pedido carrito, String username, String direccionEntrega) {
        validarCajaAbierta(); // NUEVO
        if (direccionEntrega == null || direccionEntrega.trim().isBlank()) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria");
        }
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.DOMICILIO, direccionEntrega.trim(), null, false);
    }

    @Override
    public Pedido crearPedidoClienteMesa(Pedido carrito, String username, Integer numeroMesa) {
        validarCajaAbierta(); // NUEVO
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.MESA, null, numeroMesa, true);
    }

    private Pedido crearPedidoClienteBase(Pedido carrito,
                                          String username,
                                          TipoPedidoCliente tipo,
                                          String direccionEntrega,
                                          Integer numeroMesa,
                                          boolean enviarDirectoACocina) {

        validarCarritoNoVacio(carrito);
        Cliente cliente = cargarCliente(username);

        Pedido nuevo = new Pedido();
        nuevo.setCodigo(generarCodigo());
        nuevo.setCliente(cliente);
        nuevo.setTipoPedido(tipo);

        if (tipo == TipoPedidoCliente.DOMICILIO) {
            nuevo.setDireccionEntrega(direccionEntrega);
            nuevo.setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);
        } else {
            nuevo.setDireccionEntrega(null);
            nuevo.setEstadoReparto(EstadoReparto.NO_APLICA);
        }

        if (tipo == TipoPedidoCliente.MESA) {
            ReservaMesa mesa = cargarOMesaAbierta(numeroMesa);
            nuevo.setReservaMesa(mesa);
        }

        nuevo.setEstado(enviarDirectoACocina ? EstadoPedido.EN_COCINA : EstadoPedido.EN_CURSO);
        nuevo.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);

        for (LineaPedido lp : carrito.getLineaPedidos()) {
            if (lp.getProducto() == null || lp.getProducto().getCodigo() == null) continue;

            Producto producto = productoRepo.findByCodigo(lp.getProducto().getCodigo())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + lp.getProducto().getCodigo()));

            LineaPedido nueva = new LineaPedido(nuevo, producto, lp.getCantidad());

            if (lp.getIngredientes() != null && !lp.getIngredientes().isEmpty()) {
                for (LineaPedidoIngrediente li : lp.getIngredientes()) {
                    if (li.getIngrediente() == null) continue;

                    nueva.getIngredientes().add(new LineaPedidoIngrediente(
                            nueva,
                            li.getIngrediente(),
                            li.isIncluido(),
                            li.getExtraCantidad(),
                            li.getPrecioExtra()
                    ));
                }
            }

            nuevo.getLineaPedidos().add(nueva);
        }

        pedidoRepo.save(nuevo);
        return cargarDetalle(nuevo.getCodigo());
    }

    /* Carrito -> pedido persistido */

    @Override
    public void volcarCarritoEnPedido(String codigoPedido, Pedido carrito) {
        carritoService.volcarCarritoEnPedido(codigoPedido, carrito);
    }

    @Override
    public Pedido agregarLineaPersonalizada(String codigoPedido, LineaPedido lineaPersonalizada) {
        validarCajaAbierta(); // NUEVO
        if (codigoPedido == null || codigoPedido.isBlank()) throw new IllegalArgumentException("Código inválido");
        if (lineaPersonalizada == null) throw new IllegalArgumentException("Línea inválida");
        if (lineaPersonalizada.getProducto() == null || lineaPersonalizada.getProducto().getCodigo() == null) {
            throw new IllegalArgumentException("Producto inválido");
        }
        if (lineaPersonalizada.getCantidad() <= 0) throw new IllegalArgumentException("Cantidad inválida");

        Pedido pedido = cargarDetalle(codigoPedido);

        Producto producto = productoRepo.findByCodigo(lineaPersonalizada.getProducto().getCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        LineaPedido nueva = new LineaPedido(pedido, producto, lineaPersonalizada.getCantidad());

        if (lineaPersonalizada.getIngredientes() != null && !lineaPersonalizada.getIngredientes().isEmpty()) {
            for (LineaPedidoIngrediente sel : lineaPersonalizada.getIngredientes()) {
                if (sel.getIngrediente() == null) continue;

                nueva.getIngredientes().add(new LineaPedidoIngrediente(
                        nueva,
                        sel.getIngrediente(),
                        sel.isIncluido(),
                        sel.getExtraCantidad(),
                        sel.getPrecioExtra()
                ));
            }
        }

        pedido.getLineaPedidos().add(nueva);
        marcarModificado(pedido, "SISTEMA");
        pedidoRepo.save(pedido);

        return cargarDetalle(codigoPedido);
    }

    /* Pago online */

    @Override
    public Pago iniciarPagoOnline(Pedido carrito, String username, MetodoPago metodo) {
        validarCajaAbierta(); // NUEVO
        if (metodo == null) throw new IllegalArgumentException("Método de pago inválido");
        Pedido pedidoCreado = crearPedidoClienteRecoger(carrito, username);
        return pagoService.iniciarPago(pedidoCreado, metodo);
    }

    @Override
    @Transactional(readOnly = true)
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
        validarCajaAbierta(); // NUEVO
        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) throw new IllegalArgumentException("El pago ya está confirmado");
        if (pago.getEstado() == EstadoPago.FALLIDO) throw new IllegalArgumentException("El pago está marcado como fallido");

        Pago confirmado = pagoService.confirmarPago(pago.getId(), referencia);

        Pedido pedido = confirmado.getPedido();
        if (pedido.getEstado() == EstadoPedido.ANULADO) throw new IllegalArgumentException("Pedido anulado");
        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }

        pedido.setEstado(EstadoPedido.EN_COCINA);
        marcarModificado(pedido, username);
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

    /* Cocina */

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPedidoPorId(UUID id) {
        if (id == null) throw new IllegalArgumentException("ID inválido");
        return pedidoRepo.findWithDetalleById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado con ID: " + id));
    }

    @Override
    public Pedido cambiarEstadoCocina(UUID id, EstadoCocina nuevoEstado) {
        validarCajaAbierta(); // NUEVO
        Pedido pedido = obtenerPedidoPorId(id);

        if (nuevoEstado == null) throw new IllegalArgumentException("Estado de cocina inválido");
        if (pedido.getEstado() == EstadoPedido.ANULADO) throw new IllegalArgumentException("No se puede modificar un pedido anulado");
        if (pedido.getEstadoCocina() == nuevoEstado) return pedido;

        if (pedido.getEstado() != EstadoPedido.EN_COCINA) {
            pedido.setEstado(EstadoPedido.EN_COCINA);
        }

        pedido.setEstadoCocina(nuevoEstado);

        if (nuevoEstado == EstadoCocina.LISTO) {
            if (pedido.getTipoPedido() == TipoPedidoCliente.DOMICILIO) {
                pedido.setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);
            } else {
                pedido.setEstadoReparto(EstadoReparto.NO_APLICA);
            }
        }

        if (nuevoEstado == EstadoCocina.CANCELADO) {
            pedido.setEstado(EstadoPedido.ANULADO);
            pedido.setEstadoReparto(EstadoReparto.NO_APLICA);
        }

        marcarModificado(pedido, "COCINERO");
        return pedidoRepo.save(pedido);
    }
}
