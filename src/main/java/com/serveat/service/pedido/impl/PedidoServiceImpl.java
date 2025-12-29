package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
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
import com.serveat.service.caja.EstadoCajaService;
import com.serveat.service.pago.AjustePagoDTO;
import com.serveat.service.pago.AjustePagoService;
import com.serveat.service.pago.PagoService;
import com.serveat.service.pedido.PedidoCalculoService;
import com.serveat.service.pedido.PedidoCarritoService;
import com.serveat.service.pedido.PedidoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

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
    private final EstadoCajaService estadoCajaService;
    private final PedidoCalculoService pedidoCalculoService;
    private final AjustePagoService ajustePagoService;

    public PedidoServiceImpl(PedidoRepository pedidoRepo,
                             ProductoRepository productoRepo,
                             ReservaMesaRepository reservaMesaRepo,
                             ClienteRepository clienteRepo,
                             PagoService pagoService,
                             PagoRepository pagoRepo,
                             PedidoCarritoService carritoService,
                             EstadoCajaService estadoCajaService,
                             PedidoCalculoService pedidoCalculoService,
                             AjustePagoService ajustePagoService) {
        this.pedidoRepo = pedidoRepo;
        this.productoRepo = productoRepo;
        this.reservaMesaRepo = reservaMesaRepo;
        this.clienteRepo = clienteRepo;
        this.pagoService = pagoService;
        this.pagoRepo = pagoRepo;
        this.carritoService = carritoService;
        this.estadoCajaService = estadoCajaService;
        this.pedidoCalculoService = pedidoCalculoService;
        this.ajustePagoService = ajustePagoService;
    }

    /* Helpers */

    private void validarCajaAbierta() {
        // CAMBIO: La lógica ahora pregunta si la caja NO está abierta
        if (!estadoCajaService.isCajaAbierta()) {
            throw new IllegalStateException("No se pueden realizar pedidos: la caja está cerrada.");
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
        validarCajaAbierta();
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
        validarCajaAbierta();
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
        validarCajaAbierta();
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
        validarCajaAbierta();
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
        validarCajaAbierta();
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
        validarCajaAbierta();
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
        validarCajaAbierta();
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

    @Override
    public Pedido cancelarPedidoCliente(String codigoPedido, String motivo, String username) {
        validarCajaAbierta();

        if (codigoPedido == null || codigoPedido.isBlank()) {
            throw new IllegalArgumentException("Código de pedido inválido");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }


        Pedido pCliente = cargarDetalleCliente(codigoPedido, username);
        String m = (motivo == null || motivo.isBlank()) ? "Cancelado por cliente" : motivo.trim();
        return cancelarPedido(pCliente.getCodigo(), m, username);
    }

    /* Cliente: creación */

    @Override
    public Pedido crearPedidoClienteRecoger(Pedido carrito, String username) {
        validarCajaAbierta();
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.RECOGER, null, null, false);
    }

    @Override
    public Pedido crearPedidoClienteDomicilio(Pedido carrito, String username, String direccionEntrega) {
        validarCajaAbierta();
        if (direccionEntrega == null || direccionEntrega.trim().isBlank()) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria");
        }
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.DOMICILIO, direccionEntrega.trim(), null, false);
    }

    @Override
    public Pedido crearPedidoClienteMesa(Pedido carrito, String username, Integer numeroMesa) {
        validarCajaAbierta();
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
        validarCajaAbierta();
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
        validarCajaAbierta();
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
        validarCajaAbierta();
        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) throw new IllegalArgumentException("El pago ya está confirmado");
        if (pago.getEstado() == EstadoPago.FALLIDO)
            throw new IllegalArgumentException("El pago está marcado como fallido");

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
        validarCajaAbierta();
        Pedido pedido = obtenerPedidoPorId(id);

        if (nuevoEstado == null) throw new IllegalArgumentException("Estado de cocina inválido");
        if (pedido.getEstado() == EstadoPedido.ANULADO)
            throw new IllegalArgumentException("No se puede modificar un pedido anulado");
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

    @Override
    @Transactional(readOnly = true)
    public Page<Pedido> buscarPedidosFiltrados(LocalDateTime desde,
                                               LocalDateTime hasta,
                                               EstadoPedido estadoPedido,
                                               EstadoCocina estadoCocina,
                                               Integer mesa,
                                               Pageable pageable) {
        return pedidoRepo.buscarPedidosFiltrados(desde, hasta, estadoPedido, estadoCocina, mesa, pageable);
    }

    public boolean puedeEditarOCancelarCamarero(Pedido pedido) {
        return pedido != null
                && pedido.getEstado() != EstadoPedido.ANULADO
                && pedido.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION;
    }

    public Pedido cancelarPedidoCamarero(String codigoPedido, String motivo) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String m = (motivo == null || motivo.isBlank()) ? "Cancelado por camarero" : motivo.trim();
        return cancelarPedido(codigoPedido, m, username);
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido cargarPedidoEditableCamarero(String codigoPedido, String username) {
        if (codigoPedido == null || codigoPedido.isBlank()) throw new IllegalArgumentException("Código inválido");
        if (username == null || username.isBlank()) throw new IllegalArgumentException("Usuario inválido");

        Pedido p = cargarDetalle(codigoPedido); // tu helper ya existente

        if (!puedeEditarOCancelarCamarero(p)) {
            throw new IllegalArgumentException("Este pedido no se puede editar (cocina ya lo aceptó o está anulado).");
        }
        return p;
    }

    @Override
    public List<LineaPedido> ordenarLineasParaVista(Set<LineaPedido> lineas) {
        if (lineas == null || lineas.isEmpty()) return List.of();
        List<LineaPedido> res = new ArrayList<>(lineas);
        res.sort(Comparator.comparing(LineaPedido::getCodigo, Comparator.nullsLast(String::compareToIgnoreCase)));
        return res;
    }


    @Override
    @Transactional(readOnly = true)
    public List<Ingrediente> obtenerIngredientesDisponiblesLinea(LineaPedido lp) {
        if (lp == null) return List.of();
        if (lp.getProducto() == null || lp.getProducto().getCodigo() == null) return List.of();

        // siempre recargar producto con ingredientes dentro de transacción
        Producto producto = productoRepo.findWithIngredientesByCodigo(lp.getProducto().getCodigo())
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        if (producto.getIngredientes() != null && !producto.getIngredientes().isEmpty()) {
            return producto.getIngredientes().stream()
                    .filter(Objects::nonNull)
                    .map(ProductoIngrediente::getIngrediente)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.comparing(i -> i.getNombre() == null ? "" : i.getNombre(),
                            String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        if (lp.getIngredientes() == null || lp.getIngredientes().isEmpty()) return List.of();

        return lp.getIngredientes().stream()
                .filter(Objects::nonNull)
                .map(LineaPedidoIngrediente::getIngrediente)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(i -> i.getNombre() == null ? "" : i.getNombre(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public void aplicarCantidadLinea(Pedido pedido, String codigoLinea, int nuevaCantidad) {
        if (pedido == null) throw new IllegalArgumentException("Pedido inválido");
        if (codigoLinea == null || codigoLinea.isBlank()) throw new IllegalArgumentException("Código línea inválido");
        if (nuevaCantidad <= 0) throw new IllegalArgumentException("Cantidad inválida");
        if (pedido.getLineaPedidos() == null) throw new IllegalArgumentException("Líneas no disponibles");

        LineaPedido lp = pedido.getLineaPedidos().stream()
                .filter(x -> x != null && codigoLinea.equals(x.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Línea no encontrada"));

        lp.setCantidad(nuevaCantidad);
    }

    @Override
    public void eliminarLinea(Pedido pedido, String codigoLinea) {
        if (pedido == null) throw new IllegalArgumentException("Pedido inválido");
        if (codigoLinea == null || codigoLinea.isBlank()) throw new IllegalArgumentException("Código línea inválido");
        if (pedido.getLineaPedidos() == null) return;

        boolean removed = pedido.getLineaPedidos().removeIf(lp -> lp != null && codigoLinea.equals(lp.getCodigo()));
        if (!removed) throw new IllegalArgumentException("Línea no encontrada");
    }

    @Override
    public LineaPedidoIngrediente obtenerSeleccionIngrediente(LineaPedido lp, UUID ingredienteId) {
        if (lp == null || ingredienteId == null) return null;
        if (lp.getIngredientes() == null || lp.getIngredientes().isEmpty()) return null;

        return lp.getIngredientes().stream()
                .filter(li -> li != null
                        && li.getIngrediente() != null
                        && ingredienteId.equals(li.getIngrediente().getId()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void aplicarSeleccionIngrediente(LineaPedido lp, Ingrediente ingrediente, boolean incluido, int extraCantidad) {
        if (lp == null) throw new IllegalArgumentException("Línea inválida");
        if (ingrediente == null || ingrediente.getId() == null)
            throw new IllegalArgumentException("Ingrediente inválido");

        int extra = Math.max(0, extraCantidad);

        if (lp.getIngredientes() == null) {
            lp.setIngredientes(new HashSet<>());
        }

        LineaPedidoIngrediente existente = obtenerSeleccionIngrediente(lp, ingrediente.getId());

        if (existente == null) {
            LineaPedidoIngrediente nuevo = new LineaPedidoIngrediente(
                    lp,
                    ingrediente,
                    incluido,
                    extra,
                    ingrediente.getPrecioExtra() == null ? java.math.BigDecimal.ZERO : ingrediente.getPrecioExtra()
            );
            lp.getIngredientes().add(nuevo);
            return;
        }

        existente.setIncluido(incluido);
        existente.setExtraCantidad(extra);

        if (existente.getPrecioExtra() == null) {
            existente.setPrecioExtra(ingrediente.getPrecioExtra() == null ? java.math.BigDecimal.ZERO : ingrediente.getPrecioExtra());
        }
    }


    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ProductoIngrediente> obtenerRecetaPorIngrediente(LineaPedido lp) {

        if (lp == null || lp.getProducto() == null) {
            return Map.of();
        }

        String codigoProducto = lp.getProducto().getCodigo();
        if (codigoProducto == null || codigoProducto.isBlank()) {
            return Map.of();
        }

        List<ProductoIngrediente> receta =
                productoRepo.findByProductoCodigoFetchIngrediente(codigoProducto);

        if (receta == null || receta.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ProductoIngrediente> resultado = new LinkedHashMap<>();

        for (ProductoIngrediente pi : receta) {
            if (pi == null) continue;
            if (pi.getIngrediente() == null) continue;
            if (pi.getIngrediente().getId() == null) continue;

            resultado.put(pi.getIngrediente().getId(), pi);
        }

        return resultado;
    }

    @Override
    public AjustePagoDTO confirmarCambiosPedidoClienteConAjuste(Pedido pedidoEditado, String username) {
        validarCajaAbierta();

        if (pedidoEditado == null || pedidoEditado.getCodigo() == null || pedidoEditado.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        Pedido actual = cargarDetalleCliente(pedidoEditado.getCodigo(), username);

        if (actual.getEstado() == EstadoPedido.ANULADO) {
            throw new IllegalArgumentException("Pedido anulado");
        }
        if (actual.getEstadoCocina() != EstadoCocina.PENDIENTE_ACEPTACION) {
            throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");
        }

        BigDecimal totalAnterior = pedidoCalculoService.calcularTotalPedido(actual);

        actual.getLineaPedidos().clear();

        for (LineaPedido lp : pedidoEditado.getLineaPedidos()) {
            if (lp == null || lp.getProducto() == null || lp.getProducto().getCodigo() == null) continue;

            Producto producto = productoRepo.findByCodigo(lp.getProducto().getCodigo())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + lp.getProducto().getCodigo()));

            LineaPedido nueva = new LineaPedido(actual, producto, lp.getCantidad());

            if (lp.getIngredientes() != null && !lp.getIngredientes().isEmpty()) {
                for (LineaPedidoIngrediente sel : lp.getIngredientes()) {
                    if (sel == null || sel.getIngrediente() == null) continue;

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

        BigDecimal totalNuevo = pedidoCalculoService.calcularTotalPedido(actual);

        Pago pagoOriginal = pagoRepo.findByPedido_Codigo(actual.getCodigo()).orElse(null);

        return ajustePagoService.calcularYCrearOActualizarAjuste(
                actual, pagoOriginal, totalAnterior, totalNuevo
        );
    }

    @Override
    @Transactional
    public AjustePagoDTO prepararAjusteCambiosCliente(Pedido pedidoEditado, String username) {

        if (pedidoEditado == null) throw new IllegalArgumentException("Pedido inválido");
        if (pedidoEditado.getCodigo() == null || pedidoEditado.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Falta código del pedido");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        // 1) Cargar pedido real (BD) para:
        //    - validar que es del cliente
        //    - obtener el totalAnterior
        //    - obtener el pagoOriginal (si aplica)
        Pedido pedidoBd = cargarDetalleCliente(pedidoEditado.getCodigo(), username);

        // 2) Calcular totales
        BigDecimal totalAnterior = pedidoCalculoService.calcularTotalPedido(pedidoBd);
        BigDecimal totalNuevo = pedidoCalculoService.calcularTotalPedido(pedidoEditado);

        // 3) Obtener pago original (CONFIRMADO) si existe
        Pago pagoOriginal = null;
        if (pedidoBd.getPago() != null) {
            pagoOriginal = pedidoBd.getPago();
        }

        // 4) Crear/Actualizar ajuste pendiente (esto SÍ persiste AjustePago, pero NO el pedido)
        AjustePagoDTO dto = ajustePagoService.calcularYCrearOActualizarAjuste(
                pedidoBd,
                pagoOriginal,
                totalAnterior,
                totalNuevo
        );

        // dto puede venir con:
        // - accion NINGUNA
        // - codigoAjuste null (si no aplica o si efectivo)
        return dto;
    }

}
