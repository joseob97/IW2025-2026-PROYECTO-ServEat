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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PedidoServiceImpl.class);

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
        if (!estadoCajaService.isCajaAbierta()) {
            log.warn("Operación bloqueada: caja cerrada");
            throw new IllegalStateException("No se pueden realizar pedidos: la caja está cerrada.");
        }
    }

    private String generarCodigo() {
        String codigo = "PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.debug("Generado código de pedido {}", codigo);
        return codigo;
    }

    private Pedido cargarDetalle(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            log.warn("cargarDetalle: código inválido");
            throw new IllegalArgumentException("Código de pedido inválido");
        }

        log.debug("Cargando detalle del pedido {}", codigo);

        return pedidoRepo.findWithDetalleByCodigo(codigo)
                .orElseThrow(() -> {
                    log.warn("Pedido no encontrado: {}", codigo);
                    return new IllegalArgumentException("Pedido no encontrado: " + codigo);
                });
    }

    private void validarCarritoNoVacio(Pedido carrito) {
        if (carrito == null || carrito.getLineaPedidos() == null || carrito.getLineaPedidos().isEmpty()) {
            log.warn("Validación fallida: el pedido/carrito está vacío");
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }
    }

    private Cliente cargarCliente(String username) {
        if (username == null || username.isBlank()) {
            log.warn("cargarCliente: username inválido");
            throw new IllegalArgumentException("Usuario inválido");
        }

        log.debug("Cargando cliente {}", username);

        return clienteRepo.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("Cliente no encontrado: {}", username);
                    return new IllegalArgumentException("Cliente no encontrado");
                });
    }

    private ReservaMesa cargarOMesaAbierta(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) {
            log.warn("cargarOMesaAbierta: número de mesa inválido {}", numeroMesa);
            throw new IllegalArgumentException("Número de mesa inválido");
        }

        log.debug("Buscando mesa {} con estado ABIERTA", numeroMesa);

        return reservaMesaRepo
                .findByNumeroMesaAndEstado(numeroMesa, EstadoReservaMesa.ABIERTA)
                .orElseGet(() -> {
                    log.info("No existía mesa ABIERTA para {} -> creando nueva", numeroMesa);
                    return reservaMesaRepo.save(new ReservaMesa(numeroMesa));
                });
    }

    private void marcarModificado(Pedido pedido, String username) {
        if (pedido == null) return;

        pedido.setModificadoPor(username);
        pedido.setFechaUltimaModificacion(LocalDateTime.now());

        log.debug("Pedido {} marcado como modificado por {}", pedido.getCodigo(), username);
    }

    /* Empleados / backoffice */

    @Override
    public Pedido crearPedidoMesa(Integer numeroMesa) {
        validarCajaAbierta();
        log.info("Creando pedido de mesa {}", numeroMesa);

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

        log.info("Pedido de mesa creado correctamente: codigo={}", p.getCodigo());
        return cargarDetalle(p.getCodigo());
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPorCodigo(String codigo) {
        log.debug("obtenerPorCodigo: {}", codigo);
        return cargarDetalle(codigo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidos() {
        log.debug("listarPedidos: recuperando todos los pedidos");
        return pedidoRepo.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarTodosOrdenadosPorFecha() {
        log.debug("listarTodosOrdenadosPorFecha");
        return pedidoRepo.findAllByOrderByFechaCreacionDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> buscarPorEstado(EstadoPedido estado) {
        if (estado == null) {
            log.warn("buscarPorEstado: estado nulo");
            throw new IllegalArgumentException("Estado inválido");
        }
        log.debug("buscarPorEstado: {}", estado);
        return pedidoRepo.findByEstado(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorEstado(EstadoCocina estado) {
        if (estado == null) {
            log.warn("obtenerPedidosPorEstado: estado de cocina nulo");
            throw new IllegalArgumentException("Estado de cocina inválido");
        }
        log.debug("obtenerPedidosPorEstado (cocina): {}", estado);
        return pedidoRepo.findByEstadoCocina(estado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorMesa(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) {
            log.warn("obtenerPedidosPorMesa: número de mesa inválido {}", numeroMesa);
            throw new IllegalArgumentException("Número de mesa inválido");
        }
        log.debug("obtenerPedidosPorMesa: {}", numeroMesa);
        return pedidoRepo.findByReservaMesa_NumeroMesaOrderByFechaCreacionDesc(numeroMesa);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> obtenerPedidosPorEstadoYMesa(EstadoCocina estado, Integer numeroMesa) {
        if (estado == null) {
            log.warn("obtenerPedidosPorEstadoYMesa: estado nulo");
            throw new IllegalArgumentException("El estado no puede ser nulo");
        }
        if (numeroMesa == null || numeroMesa <= 0) {
            log.warn("obtenerPedidosPorEstadoYMesa: número de mesa inválido {}", numeroMesa);
            throw new IllegalArgumentException("Número de mesa inválido");
        }
        log.debug("obtenerPedidosPorEstadoYMesa: estado={}, mesa={}", estado, numeroMesa);
        return pedidoRepo.findByEstadoCocinaAndReservaMesa_NumeroMesaOrderByFechaCreacionDesc(estado, numeroMesa);
    }

    /* Persistencia simple (por producto). Para personalizaciones, usar codigoLinea. */

    @Override
    public Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad) {
        validarCajaAbierta();

        if (codigoProducto == null || codigoProducto.isBlank()) {
            log.warn("agregarProducto: codigoProducto inválido");
            throw new IllegalArgumentException("Producto inválido");
        }
        if (cantidad <= 0) {
            log.warn("agregarProducto: cantidad inválida {}", cantidad);
            throw new IllegalArgumentException("Cantidad inválida");
        }

        log.info("Agregar producto al pedido: pedido={}, producto={}, cantidad={}", codigoPedido, codigoProducto, cantidad);

        Pedido pedido = cargarDetalle(codigoPedido);
        Producto producto = productoRepo.findByCodigo(codigoProducto)
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado: {}", codigoProducto);
                    return new IllegalArgumentException("Producto no encontrado");
                });

        carritoService.agregarProducto(pedido, producto, cantidad);

        marcarModificado(pedido, "SISTEMA");
        pedidoRepo.save(pedido);

        log.info("Producto añadido correctamente: pedido={}", codigoPedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad) {
        validarCajaAbierta();

        if (codigoProducto == null || codigoProducto.isBlank()) {
            log.warn("actualizarCantidadProducto: codigoProducto inválido");
            throw new IllegalArgumentException("Producto inválido");
        }

        log.info("Actualizar cantidad producto: pedido={}, producto={}, nuevaCantidad={}",
                codigoPedido, codigoProducto, nuevaCantidad);

        Pedido pedido = cargarDetalle(codigoPedido);

        LineaPedido lp = pedido.getLineaPedidos().stream()
                .filter(l -> l.getProducto() != null
                        && l.getProducto().getCodigo() != null
                        && l.getProducto().getCodigo().equals(codigoProducto))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Producto {} no está en el pedido {}", codigoProducto, codigoPedido);
                    return new IllegalArgumentException("Producto no está en el pedido");
                });

        if (nuevaCantidad <= 0) {
            pedido.getLineaPedidos().remove(lp);
            log.info("Cantidad <= 0 -> línea eliminada. pedido={}, producto={}", codigoPedido, codigoProducto);
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

        if (codigoProducto == null || codigoProducto.isBlank()) {
            log.warn("eliminarProducto: codigoProducto inválido");
            throw new IllegalArgumentException("Producto inválido");
        }

        log.info("Eliminar producto del pedido: pedido={}, producto={}", codigoPedido, codigoProducto);

        Pedido pedido = cargarDetalle(codigoPedido);

        boolean removed = pedido.getLineaPedidos().removeIf(l ->
                l.getProducto() != null
                        && l.getProducto().getCodigo() != null
                        && l.getProducto().getCodigo().equals(codigoProducto)
        );

        if (!removed) {
            log.warn("No se pudo eliminar: producto {} no está en pedido {}", codigoProducto, codigoPedido);
            throw new IllegalArgumentException("Producto no está en el pedido");
        }

        marcarModificado(pedido, "SISTEMA");
        pedidoRepo.save(pedido);

        log.info("Producto eliminado correctamente: pedido={}, producto={}", codigoPedido, codigoProducto);
        return cargarDetalle(codigoPedido);
    }

    /* Confirmaciones / edición */

    @Override
    public Pedido confirmarPedido(String codigoPedido) {
        validarCajaAbierta();

        log.info("Confirmando pedido {}", codigoPedido);

        Pedido pedido = cargarDetalle(codigoPedido);

        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty()) {
            log.warn("No se puede confirmar pedido vacío: {}", codigoPedido);
            throw new IllegalArgumentException("No se puede confirmar un pedido vacío");
        }

        pedido.setEstado(EstadoPedido.EN_COCINA);
        marcarModificado(pedido, "SISTEMA");
        pedidoRepo.save(pedido);

        log.info("Pedido confirmado (EN_COCINA): {}", codigoPedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario) {
        validarCajaAbierta();

        if (pedidoEditado == null) {
            log.warn("confirmarCambiosPedido: pedidoEditado nulo");
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
            log.warn("confirmarCambiosPedido: pedido vacío codigo={}", pedidoEditado.getCodigo());
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }

        boolean modificable =
                pedidoEditado.getEstado() == EstadoPedido.EN_CURSO
                        || (pedidoEditado.getEstado() == EstadoPedido.EN_COCINA
                        && pedidoEditado.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION);

        if (!modificable) {
            log.warn("confirmarCambiosPedido: cocina ya aceptó pedido codigo={}", pedidoEditado.getCodigo());
            throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");
        }

        log.info("Confirmando cambios de pedido (empleado): codigo={}, usuario={}", pedidoEditado.getCodigo(), usuario);

        marcarModificado(pedidoEditado, usuario);
        pedidoRepo.save(pedidoEditado);

        return cargarDetalle(pedidoEditado.getCodigo());
    }

    @Override
    public Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username) {
        validarCajaAbierta();

        if (pedidoEditado == null || pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
            log.warn("confirmarCambiosPedidoCliente: pedido vacío o nulo");
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }

        log.info("Confirmando cambios pedido cliente: codigo={}, username={}", pedidoEditado.getCodigo(), username);

        Pedido actual = cargarDetalleCliente(pedidoEditado.getCodigo(), username);

        if (actual.getEstado() == EstadoPedido.ANULADO) {
            log.warn("confirmarCambiosPedidoCliente: pedido anulado codigo={}", actual.getCodigo());
            throw new IllegalArgumentException("Pedido anulado");
        }
        if (actual.getEstadoCocina() != EstadoCocina.PENDIENTE_ACEPTACION) {
            log.warn("confirmarCambiosPedidoCliente: cocina ya aceptó pedido codigo={}", actual.getCodigo());
            throw new IllegalArgumentException("La cocina ya ha aceptado el pedido");
        }

        actual.getLineaPedidos().clear();

        for (LineaPedido lp : pedidoEditado.getLineaPedidos()) {
            if (lp.getProducto() == null || lp.getProducto().getCodigo() == null) continue;

            Producto producto = productoRepo.findByCodigo(lp.getProducto().getCodigo())
                    .orElseThrow(() -> {
                        log.warn("Producto no encontrado al confirmar cambios cliente: {}", lp.getProducto().getCodigo());
                        return new IllegalArgumentException("Producto no encontrado: " + lp.getProducto().getCodigo());
                    });

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

        log.info("Cambios de pedido cliente guardados: codigo={}", actual.getCodigo());
        return cargarDetalle(actual.getCodigo());
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido cargarDetalleCliente(String codigo, String username) {
        if (codigo == null || codigo.isBlank()) {
            log.warn("cargarDetalleCliente: código inválido");
            throw new IllegalArgumentException("Código inválido");
        }
        if (username == null || username.isBlank()) {
            log.warn("cargarDetalleCliente: username inválido");
            throw new IllegalArgumentException("Usuario inválido");
        }

        log.debug("Cargando detalle cliente: codigo={}, username={}", codigo, username);

        return pedidoRepo.findWithDetalleByCodigoAndCliente_Username(codigo, username)
                .orElseThrow(() -> {
                    log.warn("Pedido no encontrado o no pertenece al cliente: codigo={}, username={}", codigo, username);
                    return new IllegalArgumentException("Pedido no encontrado o no pertenece al cliente");
                });
    }

    @Override
    public Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername) {
        log.info("Cancelar pedido: codigo={}, usuario={}, motivo={}", codigoPedido, camareroUsername, motivo);

        Pedido pedido = cargarDetalle(codigoPedido);

        boolean cancelable =
                pedido.getEstado() == EstadoPedido.EN_CURSO
                        || (pedido.getEstado() == EstadoPedido.EN_COCINA
                        && pedido.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION);

        if (!cancelable) {
            log.warn("No se puede cancelar pedido (estado no permite): codigo={}, estado={}, estadoCocina={}",
                    codigoPedido, pedido.getEstado(), pedido.getEstadoCocina());
            throw new IllegalArgumentException("No se puede cancelar el pedido");
        }

        pedido.setEstado(EstadoPedido.ANULADO);
        pedido.setEstadoCocina(EstadoCocina.CANCELADO);
        pedido.setCanceladoPor(camareroUsername);
        pedido.setMotivoCancelacion(motivo);
        pedido.setFechaCancelacion(LocalDateTime.now());

        marcarModificado(pedido, camareroUsername);
        pedidoRepo.save(pedido);

        log.info("Pedido cancelado correctamente: {}", codigoPedido);
        return cargarDetalle(codigoPedido);
    }

    @Override
    public boolean puedeModificarCliente(Pedido pedido) {
        boolean res = pedido != null
                && pedido.getEstado() != EstadoPedido.ANULADO
                && pedido.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION;

        log.debug("puedeModificarCliente: codigo={}, result={}",
                pedido != null ? pedido.getCodigo() : "-", res);

        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosModificables() {
        log.debug("listarPedidosModificables");
        return pedidoRepo.findByEstadoOrEstadoAndEstadoCocina(
                EstadoPedido.EN_CURSO,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Pedido> listarPedidosModificablesPorMesa(Integer numeroMesa) {
        if (numeroMesa == null || numeroMesa <= 0) {
            log.warn("listarPedidosModificablesPorMesa: mesa inválida {}", numeroMesa);
            throw new IllegalArgumentException("Número de mesa inválido");
        }

        log.debug("listarPedidosModificablesPorMesa: mesa={}", numeroMesa);

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
        if (username == null || username.isBlank()) {
            log.warn("listarPedidosCliente: username inválido");
            throw new IllegalArgumentException("Usuario inválido");
        }

        log.debug("listarPedidosCliente: {}", username);
        return pedidoRepo.findByCliente_UsernameOrderByFechaCreacionDesc(username);
    }

    @Override
    public Pedido cancelarPedidoCliente(String codigoPedido, String motivo, String username) {
        validarCajaAbierta();

        if (codigoPedido == null || codigoPedido.isBlank()) {
            log.warn("cancelarPedidoCliente: código inválido");
            throw new IllegalArgumentException("Código de pedido inválido");
        }
        if (username == null || username.isBlank()) {
            log.warn("cancelarPedidoCliente: username inválido");
            throw new IllegalArgumentException("Usuario inválido");
        }

        log.info("Cancelar pedido cliente: codigo={}, username={}, motivo={}", codigoPedido, username, motivo);

        Pedido pCliente = cargarDetalleCliente(codigoPedido, username);
        String m = (motivo == null || motivo.isBlank()) ? "Cancelado por cliente" : motivo.trim();

        return cancelarPedido(pCliente.getCodigo(), m, username);
    }

    /* Cliente: creación */

    @Override
    public Pedido crearPedidoClienteRecoger(Pedido carrito, String username) {
        validarCajaAbierta();
        log.info("Crear pedido cliente (RECOGER): username={}", username);
        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.RECOGER, null, null, false);
    }

    @Override
    public Pedido crearPedidoClienteDomicilio(Pedido carrito, String username, String direccionEntrega) {
        validarCajaAbierta();

        if (direccionEntrega == null || direccionEntrega.trim().isBlank()) {
            log.warn("crearPedidoClienteDomicilio: dirección obligatoria (username={})", username);
            throw new IllegalArgumentException("La dirección de entrega es obligatoria");
        }

        log.info("Crear pedido cliente (DOMICILIO): username={}, direccion={}", username, direccionEntrega);

        return crearPedidoClienteBase(carrito, username, TipoPedidoCliente.DOMICILIO, direccionEntrega.trim(), null, false);
    }

    @Override
    public Pedido crearPedidoClienteMesa(Pedido carrito, String username, Integer numeroMesa) {
        validarCajaAbierta();
        log.info("Crear pedido cliente (MESA): username={}, mesa={}", username, numeroMesa);
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
                    .orElseThrow(() -> {
                        log.warn("Producto no encontrado al crear pedido cliente: {}", lp.getProducto().getCodigo());
                        return new IllegalArgumentException("Producto no encontrado: " + lp.getProducto().getCodigo());
                    });

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

        log.info("Pedido cliente creado: codigo={}, username={}, tipo={}", nuevo.getCodigo(), username, tipo);

        return cargarDetalle(nuevo.getCodigo());
    }

    /* Carrito -> pedido persistido */

    @Override
    public void volcarCarritoEnPedido(String codigoPedido, Pedido carrito) {
        log.info("Volcando carrito en pedido: codigo={}", codigoPedido);
        carritoService.volcarCarritoEnPedido(codigoPedido, carrito);
    }

    @Override
    public Pedido agregarLineaPersonalizada(String codigoPedido, LineaPedido lineaPersonalizada) {
        validarCajaAbierta();

        if (codigoPedido == null || codigoPedido.isBlank()) {
            log.warn("agregarLineaPersonalizada: código inválido");
            throw new IllegalArgumentException("Código inválido");
        }
        if (lineaPersonalizada == null) {
            log.warn("agregarLineaPersonalizada: línea nula");
            throw new IllegalArgumentException("Línea inválida");
        }
        if (lineaPersonalizada.getProducto() == null || lineaPersonalizada.getProducto().getCodigo() == null) {
            log.warn("agregarLineaPersonalizada: producto inválido en línea");
            throw new IllegalArgumentException("Producto inválido");
        }
        if (lineaPersonalizada.getCantidad() <= 0) {
            log.warn("agregarLineaPersonalizada: cantidad inválida {}", lineaPersonalizada.getCantidad());
            throw new IllegalArgumentException("Cantidad inválida");
        }

        log.info("Agregar línea personalizada: pedido={}, producto={}, cantidad={}",
                codigoPedido, lineaPersonalizada.getProducto().getCodigo(), lineaPersonalizada.getCantidad());

        Pedido pedido = cargarDetalle(codigoPedido);

        Producto producto = productoRepo.findByCodigo(lineaPersonalizada.getProducto().getCodigo())
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado al agregar línea personalizada: {}", lineaPersonalizada.getProducto().getCodigo());
                    return new IllegalArgumentException("Producto no encontrado");
                });

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

        log.info("Línea personalizada añadida correctamente: pedido={}", codigoPedido);

        return cargarDetalle(codigoPedido);
    }

    /* Pago online */

    @Override
    public Pago iniciarPagoOnline(Pedido carrito, String username, MetodoPago metodo) {
        validarCajaAbierta();

        if (metodo == null) {
            log.warn("iniciarPagoOnline: método inválido (username={})", username);
            throw new IllegalArgumentException("Método de pago inválido");
        }

        log.info("Iniciando pago online: username={}, metodo={}", username, metodo);

        Pedido pedidoCreado = crearPedidoClienteRecoger(carrito, username);
        Pago pago = pagoService.iniciarPago(pedidoCreado, metodo);

        log.info("Pago iniciado: pagoId={}, pedido={}", pago != null ? pago.getId() : null, pedidoCreado.getCodigo());

        return pago;
    }

    @Override
    @Transactional(readOnly = true)
    public Pago obtenerPagoCliente(Long pagoId, String username) {
        if (pagoId == null) {
            log.warn("obtenerPagoCliente: pagoId nulo");
            throw new IllegalArgumentException("Pago inválido");
        }
        if (username == null || username.isBlank()) {
            log.warn("obtenerPagoCliente: username inválido");
            throw new IllegalArgumentException("Usuario inválido");
        }

        log.debug("obtenerPagoCliente: pagoId={}, username={}", pagoId, username);

        Pago pago = pagoRepo.findById(pagoId)
                .orElseThrow(() -> {
                    log.warn("Pago no encontrado: {}", pagoId);
                    return new IllegalArgumentException("Pago no encontrado");
                });

        Pedido pedido = pago.getPedido();
        if (pedido == null || pedido.getCliente() == null || !username.equals(pedido.getCliente().getUsername())) {
            log.warn("Pago {} no pertenece al cliente {}", pagoId, username);
            throw new IllegalArgumentException("Pago no pertenece al cliente");
        }

        return pago;
    }

    @Override
    public Pedido confirmarPagoOnline(Long pagoId, String username, String referencia) {
        validarCajaAbierta();

        log.info("Confirmar pago online: pagoId={}, username={}", pagoId, username);

        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) {
            log.warn("Pago ya confirmado: pagoId={}", pagoId);
            throw new IllegalArgumentException("El pago ya está confirmado");
        }
        if (pago.getEstado() == EstadoPago.FALLIDO) {
            log.warn("Pago marcado como FALLIDO: pagoId={}", pagoId);
            throw new IllegalArgumentException("El pago está marcado como fallido");
        }

        Pago confirmado = pagoService.confirmarPago(pago.getId(), referencia);

        Pedido pedido = confirmado.getPedido();
        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            log.warn("Pedido anulado tras confirmar pago: pedido={}", pedido.getCodigo());
            throw new IllegalArgumentException("Pedido anulado");
        }
        if (pedido.getLineaPedidos() == null || pedido.getLineaPedidos().isEmpty()) {
            log.warn("Pedido vacío tras confirmar pago: pedido={}", pedido.getCodigo());
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }

        pedido.setEstado(EstadoPedido.EN_COCINA);
        marcarModificado(pedido, username);
        pedidoRepo.save(pedido);

        log.info("Pago confirmado y pedido enviado a cocina: pedido={}", pedido.getCodigo());

        return cargarDetalleCliente(pedido.getCodigo(), username);
    }

    @Override
    public Pedido marcarPagoOnlineFallido(Long pagoId, String username, String motivo) {
        log.info("Marcar pago online como fallido: pagoId={}, username={}, motivo={}", pagoId, username, motivo);

        Pago pago = obtenerPagoCliente(pagoId, username);

        if (pago.getEstado() == EstadoPago.CONFIRMADO) {
            log.warn("No se puede marcar pago como fallido (ya confirmado): pagoId={}", pagoId);
            throw new IllegalArgumentException("El pago ya está confirmado");
        }

        String m = (motivo == null || motivo.isBlank()) ? "Cancelado por el cliente" : motivo.trim();
        pagoService.marcarPagoFallido(pago.getId(), m);

        Pedido pedido = pago.getPedido();
        log.info("Pago marcado como FALLIDO para pedido={}", pedido != null ? pedido.getCodigo() : null);

        return cargarDetalleCliente(pedido.getCodigo(), username);
    }

    /* Cocina */

    @Override
    @Transactional(readOnly = true)
    public Pedido obtenerPedidoPorId(UUID id) {
        if (id == null) {
            log.warn("obtenerPedidoPorId: id nulo");
            throw new IllegalArgumentException("ID inválido");
        }

        log.debug("obtenerPedidoPorId: {}", id);

        return pedidoRepo.findWithDetalleById(id)
                .orElseThrow(() -> {
                    log.warn("Pedido no encontrado con ID: {}", id);
                    return new IllegalArgumentException("Pedido no encontrado con ID: " + id);
                });
    }

    @Override
    public Pedido cambiarEstadoCocina(UUID id, EstadoCocina nuevoEstado) {
        validarCajaAbierta();

        Pedido pedido = obtenerPedidoPorId(id);

        if (nuevoEstado == null) {
            log.warn("cambiarEstadoCocina: nuevoEstado nulo (pedidoId={})", id);
            throw new IllegalArgumentException("Estado de cocina inválido");
        }
        if (pedido.getEstado() == EstadoPedido.ANULADO) {
            log.warn("cambiarEstadoCocina: pedido anulado (codigo={})", pedido.getCodigo());
            throw new IllegalArgumentException("No se puede modificar un pedido anulado");
        }
        if (pedido.getEstadoCocina() == nuevoEstado) {
            log.debug("cambiarEstadoCocina: estado igual, no cambia (pedido={})", pedido.getCodigo());
            return pedido;
        }

        log.info("Cambio estado cocina: pedido={}, {} -> {}", pedido.getCodigo(), pedido.getEstadoCocina(), nuevoEstado);

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

        Pedido guardado = pedidoRepo.save(pedido);
        log.info("Estado cocina actualizado: pedido={}, estadoCocina={}", guardado.getCodigo(), guardado.getEstadoCocina());

        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Pedido> buscarPedidosFiltrados(LocalDateTime desde,
                                               LocalDateTime hasta,
                                               EstadoPedido estadoPedido,
                                               EstadoCocina estadoCocina,
                                               Integer mesa,
                                               Pageable pageable) {
        log.debug("buscarPedidosFiltrados: desde={}, hasta={}, estadoPedido={}, estadoCocina={}, mesa={}",
                desde, hasta, estadoPedido, estadoCocina, mesa);

        return pedidoRepo.buscarPedidosFiltrados(desde, hasta, estadoPedido, estadoCocina, mesa, pageable);
    }

    public boolean puedeEditarOCancelarCamarero(Pedido pedido) {
        boolean res = pedido != null
                && pedido.getEstado() != EstadoPedido.ANULADO
                && pedido.getEstadoCocina() == EstadoCocina.PENDIENTE_ACEPTACION;

        log.debug("puedeEditarOCancelarCamarero: pedido={}, result={}", pedido != null ? pedido.getCodigo() : "-", res);

        return res;
    }

    public Pedido cancelarPedidoCamarero(String codigoPedido, String motivo) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        String m = (motivo == null || motivo.isBlank()) ? "Cancelado por camarero" : motivo.trim();

        log.info("cancelarPedidoCamarero: pedido={}, username={}, motivo={}", codigoPedido, username, m);

        return cancelarPedido(codigoPedido, m, username);
    }

    @Override
    @Transactional(readOnly = true)
    public Pedido cargarPedidoEditableCamarero(String codigoPedido, String username) {
        if (codigoPedido == null || codigoPedido.isBlank()) {
            log.warn("cargarPedidoEditableCamarero: código inválido");
            throw new IllegalArgumentException("Código inválido");
        }
        if (username == null || username.isBlank()) {
            log.warn("cargarPedidoEditableCamarero: username inválido");
            throw new IllegalArgumentException("Usuario inválido");
        }

        Pedido p = cargarDetalle(codigoPedido);

        if (!puedeEditarOCancelarCamarero(p)) {
            log.warn("Pedido no editable por camarero: pedido={}", p.getCodigo());
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

        Producto producto = productoRepo.findWithIngredientesByCodigo(lp.getProducto().getCodigo())
                .orElseThrow(() -> {
                    log.warn("Producto no encontrado al obtener ingredientes disponibles: {}", lp.getProducto().getCodigo());
                    return new IllegalArgumentException("Producto no encontrado");
                });

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

        log.debug("Cantidad línea aplicada: pedido={}, linea={}, nuevaCantidad={}",
                pedido.getCodigo(), codigoLinea, nuevaCantidad);
    }

    @Override
    public void eliminarLinea(Pedido pedido, String codigoLinea) {
        if (pedido == null) throw new IllegalArgumentException("Pedido inválido");
        if (codigoLinea == null || codigoLinea.isBlank()) throw new IllegalArgumentException("Código línea inválido");
        if (pedido.getLineaPedidos() == null) return;

        boolean removed = pedido.getLineaPedidos().removeIf(lp -> lp != null && codigoLinea.equals(lp.getCodigo()));
        if (!removed) throw new IllegalArgumentException("Línea no encontrada");

        log.debug("Línea eliminada: pedido={}, linea={}", pedido.getCodigo(), codigoLinea);
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

            log.debug("Seleccion ingrediente creada: linea={}, ingrediente={}, incluido={}, extraCantidad={}",
                    lp.getCodigo(), ingrediente.getId(), incluido, extra);

            return;
        }

        existente.setIncluido(incluido);
        existente.setExtraCantidad(extra);

        if (existente.getPrecioExtra() == null) {
            existente.setPrecioExtra(ingrediente.getPrecioExtra() == null ? java.math.BigDecimal.ZERO : ingrediente.getPrecioExtra());
        }

        log.debug("Seleccion ingrediente actualizada: linea={}, ingrediente={}, incluido={}, extraCantidad={}",
                lp.getCodigo(), ingrediente.getId(), incluido, extra);
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

        log.info("confirmarCambiosPedidoClienteConAjuste: pedido={}, username={}",
                pedidoEditado != null ? pedidoEditado.getCodigo() : null, username);

        if (pedidoEditado == null || pedidoEditado.getCodigo() == null || pedidoEditado.getCodigo().isBlank()) {
            log.warn("confirmarCambiosPedidoClienteConAjuste: pedido inválido");
            throw new IllegalArgumentException("Pedido inválido");
        }
        if (pedidoEditado.getLineaPedidos() == null || pedidoEditado.getLineaPedidos().isEmpty()) {
            log.warn("confirmarCambiosPedidoClienteConAjuste: pedido vacío");
            throw new IllegalArgumentException("El pedido no puede quedar vacío");
        }
        if (username == null || username.isBlank()) {
            log.warn("confirmarCambiosPedidoClienteConAjuste: usuario inválido");
            throw new IllegalArgumentException("Usuario inválido");
        }

        Pedido actual = cargarDetalleCliente(pedidoEditado.getCodigo(), username);

        if (actual.getEstado() == EstadoPedido.ANULADO) {
            log.warn("confirmarCambiosPedidoClienteConAjuste: pedido anulado {}", actual.getCodigo());
            throw new IllegalArgumentException("Pedido anulado");
        }
        if (actual.getEstadoCocina() != EstadoCocina.PENDIENTE_ACEPTACION) {
            log.warn("confirmarCambiosPedidoClienteConAjuste: cocina ya aceptó {}", actual.getCodigo());
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

        AjustePagoDTO dto = ajustePagoService.calcularYCrearOActualizarAjuste(
                actual, pagoOriginal, totalAnterior, totalNuevo
        );

        log.info("Ajuste calculado: pedido={}, accion={}, codigoAjuste={}",
                actual.getCodigo(),
                dto != null ? dto.getAccion() : null,
                dto != null ? dto.getCodigoAjuste() : null);

        return dto;
    }

    @Override
    @Transactional
    public AjustePagoDTO prepararAjusteCambiosCliente(Pedido pedidoEditado, String username) {

        log.info("prepararAjusteCambiosCliente: pedido={}, username={}",
                pedidoEditado != null ? pedidoEditado.getCodigo() : null, username);

        if (pedidoEditado == null) throw new IllegalArgumentException("Pedido inválido");
        if (pedidoEditado.getCodigo() == null || pedidoEditado.getCodigo().isBlank()) {
            throw new IllegalArgumentException("Falta código del pedido");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Usuario inválido");
        }

        Pedido pedidoBd = cargarDetalleCliente(pedidoEditado.getCodigo(), username);

        BigDecimal totalAnterior = pedidoCalculoService.calcularTotalPedido(pedidoBd);
        BigDecimal totalNuevo = pedidoCalculoService.calcularTotalPedido(pedidoEditado);

        Pago pagoOriginal = null;
        if (pedidoBd.getPago() != null) {
            pagoOriginal = pedidoBd.getPago();
        }

        AjustePagoDTO dto = ajustePagoService.calcularYCrearOActualizarAjuste(
                pedidoBd,
                pagoOriginal,
                totalAnterior,
                totalNuevo
        );

        log.info("Ajuste preparado: pedido={}, accion={}, codigoAjuste={}",
                pedidoBd.getCodigo(),
                dto != null ? dto.getAccion() : null,
                dto != null ? dto.getCodigoAjuste() : null);

        return dto;
    }
}
