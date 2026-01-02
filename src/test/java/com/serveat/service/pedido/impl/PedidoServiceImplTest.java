package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PedidoServiceImplTest {

    @Mock
    private PedidoRepository pedidoRepo;
    @Mock
    private ProductoRepository productoRepo;
    @Mock
    private ReservaMesaRepository reservaMesaRepo;
    @Mock
    private ClienteRepository clienteRepo;
    @Mock
    private PagoService pagoService;
    @Mock
    private PagoRepository pagoRepo;
    @Mock
    private PedidoCarritoService carritoService;
    @Mock
    private EstadoCajaService estadoCajaService;
    @Mock
    private PedidoCalculoService pedidoCalculoService;
    @Mock
    private AjustePagoService ajustePagoService;

    @InjectMocks
    private PedidoServiceImpl service;

    private final Map<String, Pedido> detallePorCodigo = new HashMap<>();

    @BeforeEach
    void setup() {
        // Por defecto: caja abierta
        lenient().when(estadoCajaService.isCajaAbierta()).thenReturn(true);

        // Simula recarga de detalle devolviendo el pedido previamente "persistido"
        lenient().when(pedidoRepo.findWithDetalleByCodigo(anyString())).thenAnswer(inv -> {
            String codigo = inv.getArgument(0, String.class);
            Pedido p = detallePorCodigo.get(codigo);
            return Optional.ofNullable(p);
        });

        // Simula persistencia: guarda el pedido y lo deja disponible para la recarga por código
        lenient().when(pedidoRepo.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0, Pedido.class);
            if (p != null && p.getCodigo() != null) {
                detallePorCodigo.put(p.getCodigo(), p);
            }
            return p;
        });

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        detallePorCodigo.clear();
    }

    // Helpers

    private static Cliente cliente(String username) {
        Cliente c = new Cliente();
        c.setUsername(username);
        c.setNombre("Nombre");
        c.setEmail(username + "@mail.com");
        c.setPassword("pass");
        c.setTelefono("600000000");
        c.setDireccion("Dir");
        return c;
    }

    private static Producto producto(String codigo, BigDecimal precio) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre("Prod " + codigo);
        p.setPrecio(precio);
        return p;
    }

    private static Pedido pedidoBase(String codigo) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        p.setTipoPedido(TipoPedidoCliente.MESA);
        p.setEstadoReparto(EstadoReparto.NO_APLICA);
        p.setLineaPedidos(new LinkedHashSet<>());
        return p;
    }

    private static LineaPedido lineaConProducto(Pedido pedido, Producto producto, int cantidad, String codigoLinea) {
        LineaPedido lp = new LineaPedido(pedido, producto, cantidad);
        lp.setCodigo(codigoLinea);
        return lp;
    }

    private void stubCargarDetalle(String codigo, Pedido pedido) {
        detallePorCodigo.put(codigo, pedido);
        when(pedidoRepo.findWithDetalleByCodigo(eq(codigo))).thenReturn(Optional.of(pedido));
    }

    private void stubCargarDetalleCliente(String codigo, String username, Pedido pedido) {
        when(pedidoRepo.findWithDetalleByCodigoAndCliente_Username(eq(codigo), eq(username)))
                .thenReturn(Optional.of(pedido));
    }

    private static void setIngredienteId(Ingrediente ing, UUID id) {
        try {
            Field f = Ingrediente.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(ing, id);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo setear el id del Ingrediente por reflexión. Ajusta el nombre del campo o añade setId().", e);
        }
    }

    private static void forceLineaPedidoIngredientePrecioExtraNull(LineaPedidoIngrediente li) {
        try {
            Field f = LineaPedidoIngrediente.class.getDeclaredField("precioExtra");
            f.setAccessible(true);
            f.set(li, null);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo forzar precioExtra a null en LineaPedidoIngrediente por reflexión.", e);
        }
    }

    // Empleados / backoffice

    @Test
    void crearPedidoMesa_ok_cajaAbierta_mesaAbierta() {
        int mesaNum = 5;

        ReservaMesa mesa = new ReservaMesa(mesaNum);
        when(reservaMesaRepo.findByNumeroMesaAndEstado(mesaNum, EstadoReservaMesa.ABIERTA))
                .thenReturn(Optional.of(mesa));

        Pedido res = service.crearPedidoMesa(mesaNum);

        assertThat(res).isNotNull();
        assertThat(res.getCodigo()).startsWith("PED-");
        assertThat(res.getEstado()).isEqualTo(EstadoPedido.EN_CURSO);
        assertThat(res.getEstadoCocina()).isEqualTo(EstadoCocina.PENDIENTE_ACEPTACION);
        assertThat(res.getTipoPedido()).isEqualTo(TipoPedidoCliente.MESA);
        assertThat(res.getReservaMesa()).isNotNull();
        verify(pedidoRepo).save(any(Pedido.class));
        verify(reservaMesaRepo).findByNumeroMesaAndEstado(mesaNum, EstadoReservaMesa.ABIERTA);
    }

    @Test
    void crearPedidoMesa_ok_cajaAbierta_mesa_no_existe_crea_nueva() {
        int mesaNum = 7;

        when(reservaMesaRepo.findByNumeroMesaAndEstado(mesaNum, EstadoReservaMesa.ABIERTA))
                .thenReturn(Optional.empty());

        when(reservaMesaRepo.save(any(ReservaMesa.class))).thenAnswer(inv -> inv.getArgument(0));

        Pedido res = service.crearPedidoMesa(mesaNum);

        assertThat(res.getReservaMesa()).isNotNull();
        assertThat(res.getReservaMesa().getNumeroMesa()).isEqualTo(mesaNum);
        verify(reservaMesaRepo).save(any(ReservaMesa.class));
    }

    @Test
    void crearPedidoMesa_falla_si_caja_cerrada() {
        when(estadoCajaService.isCajaAbierta()).thenReturn(false);

        assertThatThrownBy(() -> service.crearPedidoMesa(1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("caja está cerrada");

        verifyNoInteractions(pedidoRepo);
    }

    @Test
    void obtenerPorCodigo_ok() {
        Pedido p = pedidoBase("PED-AAAA1111");
        stubCargarDetalle("PED-AAAA1111", p);

        assertThat(service.obtenerPorCodigo("PED-AAAA1111")).isSameAs(p);
    }

    @Test
    void obtenerPorCodigo_falla_si_codigo_invalido() {
        assertThatThrownBy(() -> service.obtenerPorCodigo("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listarPedidos_ok() {
        when(pedidoRepo.findAll()).thenReturn(List.of(pedidoBase("P1"), pedidoBase("P2")));
        assertThat(service.listarPedidos()).hasSize(2);
    }

    @Test
    void listarTodosOrdenadosPorFecha_ok() {
        when(pedidoRepo.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(pedidoBase("P2"), pedidoBase("P1")));
        assertThat(service.listarTodosOrdenadosPorFecha()).extracting(Pedido::getCodigo).containsExactly("P2", "P1");
    }

    @Test
    void buscarPorEstado_ok() {
        when(pedidoRepo.findByEstado(EstadoPedido.EN_CURSO)).thenReturn(List.of(pedidoBase("P1")));
        assertThat(service.buscarPorEstado(EstadoPedido.EN_CURSO)).hasSize(1);
    }

    @Test
    void buscarPorEstado_falla_si_null() {
        assertThatThrownBy(() -> service.buscarPorEstado(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado inválido");
    }

    @Test
    void obtenerPedidosPorEstado_ok() {
        when(pedidoRepo.findByEstadoCocina(EstadoCocina.LISTO)).thenReturn(List.of(pedidoBase("P1")));
        assertThat(service.obtenerPedidosPorEstado(EstadoCocina.LISTO)).hasSize(1);
    }

    @Test
    void obtenerPedidosPorEstado_falla_si_null() {
        assertThatThrownBy(() -> service.obtenerPedidosPorEstado(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estado de cocina inválido");
    }

    @Test
    void obtenerPedidosPorMesa_ok() {
        when(pedidoRepo.findByReservaMesa_NumeroMesaOrderByFechaCreacionDesc(3)).thenReturn(List.of(pedidoBase("P1")));
        assertThat(service.obtenerPedidosPorMesa(3)).hasSize(1);
    }

    @Test
    void obtenerPedidosPorMesa_falla_si_invalida() {
        assertThatThrownBy(() -> service.obtenerPedidosPorMesa(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Número de mesa inválido");
    }

    @Test
    void obtenerPedidosPorEstadoYMesa_ok() {
        when(pedidoRepo.findByEstadoCocinaAndReservaMesa_NumeroMesaOrderByFechaCreacionDesc(EstadoCocina.ACEPTADO, 2))
                .thenReturn(List.of(pedidoBase("P1")));
        assertThat(service.obtenerPedidosPorEstadoYMesa(EstadoCocina.ACEPTADO, 2)).hasSize(1);
    }

    @Test
    void obtenerPedidosPorEstadoYMesa_falla_por_parametros() {
        assertThatThrownBy(() -> service.obtenerPedidosPorEstadoYMesa(null, 2))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> service.obtenerPedidosPorEstadoYMesa(EstadoCocina.ACEPTADO, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Persistencia simple por producto

    @Test
    void agregarProducto_ok() {
        Pedido p = pedidoBase("PED-XYZ");
        stubCargarDetalle("PED-XYZ", p);

        Producto prod = producto("PR-1", new BigDecimal("3.50"));
        when(productoRepo.findByCodigo("PR-1")).thenReturn(Optional.of(prod));

        Pedido res = service.agregarProducto("PED-XYZ", "PR-1", 2);

        assertThat(res).isSameAs(p);
        verify(carritoService).agregarProducto(p, prod, 2);
        verify(pedidoRepo).save(p);
    }

    @Test
    void agregarProducto_falla_validaciones() {
        assertThatThrownBy(() -> service.agregarProducto("PED", " ", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.agregarProducto("PED", "PR", 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actualizarCantidadProducto_ok_setea_nuevaCantidad() {
        Pedido p = pedidoBase("PED-XYZ");
        Producto prod = producto("PR-1", new BigDecimal("3.50"));
        LineaPedido lp = lineaConProducto(p, prod, 1, "LP-1");
        p.getLineaPedidos().add(lp);

        stubCargarDetalle("PED-XYZ", p);

        Pedido res = service.actualizarCantidadProducto("PED-XYZ", "PR-1", 5);

        assertThat(res).isSameAs(p);
        assertThat(lp.getCantidad()).isEqualTo(5);
        verify(pedidoRepo).save(p);
    }

    @Test
    void actualizarCantidadProducto_ok_elimina_si_nuevaCantidad_menor_igual_0() {
        Pedido p = pedidoBase("PED-XYZ");
        Producto prod = producto("PR-1", new BigDecimal("3.50"));
        LineaPedido lp = lineaConProducto(p, prod, 1, "LP-1");
        p.getLineaPedidos().add(lp);

        stubCargarDetalle("PED-XYZ", p);

        service.actualizarCantidadProducto("PED-XYZ", "PR-1", 0);

        assertThat(p.getLineaPedidos()).isEmpty();
        verify(pedidoRepo).save(p);
    }

    @Test
    void actualizarCantidadProducto_falla_si_no_esta_en_pedido() {
        Pedido p = pedidoBase("PED-XYZ");
        stubCargarDetalle("PED-XYZ", p);

        assertThatThrownBy(() -> service.actualizarCantidadProducto("PED-XYZ", "PR-1", 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producto no está en el pedido");
    }

    @Test
    void eliminarProducto_ok() {
        Pedido p = pedidoBase("PED-XYZ");
        Producto prod = producto("PR-1", new BigDecimal("3.50"));
        p.getLineaPedidos().add(lineaConProducto(p, prod, 1, "LP-1"));

        stubCargarDetalle("PED-XYZ", p);

        service.eliminarProducto("PED-XYZ", "PR-1");

        assertThat(p.getLineaPedidos()).isEmpty();
        verify(pedidoRepo).save(p);
    }

    @Test
    void eliminarProducto_falla_si_no_existe() {
        Pedido p = pedidoBase("PED-XYZ");
        stubCargarDetalle("PED-XYZ", p);

        assertThatThrownBy(() -> service.eliminarProducto("PED-XYZ", "PR-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producto no está en el pedido");
    }

    // Confirmaciones / edición

    @Test
    void confirmarPedido_ok() {
        Pedido p = pedidoBase("PED-XYZ");
        p.getLineaPedidos().add(lineaConProducto(p, producto("PR-1", BigDecimal.ONE), 1, "LP-1"));

        stubCargarDetalle("PED-XYZ", p);

        Pedido res = service.confirmarPedido("PED-XYZ");

        assertThat(res.getEstado()).isEqualTo(EstadoPedido.EN_COCINA);
        verify(pedidoRepo).save(p);
    }

    @Test
    void confirmarPedido_falla_si_vacio() {
        Pedido p = pedidoBase("PED-XYZ");
        stubCargarDetalle("PED-XYZ", p);

        assertThatThrownBy(() -> service.confirmarPedido("PED-XYZ"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pedido vacío");
    }

    @Test
    void confirmarCambiosPedido_ok_modificable_en_curso() {
        Pedido edit = pedidoBase("PED-XYZ");
        edit.setEstado(EstadoPedido.EN_CURSO);
        edit.getLineaPedidos().add(lineaConProducto(edit, producto("PR-1", BigDecimal.ONE), 1, "LP-1"));

        stubCargarDetalle("PED-XYZ", edit);

        Pedido res = service.confirmarCambiosPedido(edit, "user1");

        assertThat(res).isSameAs(edit);
        assertThat(edit.getModificadoPor()).isEqualTo("user1");
        assertThat(edit.getFechaUltimaModificacion()).isNotNull();
        verify(pedidoRepo).save(edit);
    }

    @Test
    void confirmarCambiosPedido_falla_si_no_modificable() {
        Pedido edit = pedidoBase("PED-XYZ");
        edit.setEstado(EstadoPedido.EN_COCINA);
        edit.setEstadoCocina(EstadoCocina.ACEPTADO);
        edit.getLineaPedidos().add(lineaConProducto(edit, producto("PR-1", BigDecimal.ONE), 1, "LP-1"));

        assertThatThrownBy(() -> service.confirmarCambiosPedido(edit, "user1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cocina ya ha aceptado");
    }

    @Test
    void confirmarCambiosPedidoCliente_ok_reconstruye_lineas() {
        String codigo = "PED-CL";
        String username = "cliente1";

        Cliente c = cliente(username);
        Pedido actual = pedidoBase(codigo);
        actual.setCliente(c);
        actual.setEstado(EstadoPedido.EN_CURSO);
        actual.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);

        Pedido edit = pedidoBase(codigo);
        Producto prodEdit = producto("PR-1", new BigDecimal("2.00"));
        LineaPedido lpEdit = lineaConProducto(edit, prodEdit, 2, "LP-X");
        edit.setLineaPedidos(new LinkedHashSet<>(Set.of(lpEdit)));

        stubCargarDetalleCliente(codigo, username, actual);
        when(productoRepo.findByCodigo("PR-1")).thenReturn(Optional.of(prodEdit));
        stubCargarDetalle(codigo, actual);

        Pedido res = service.confirmarCambiosPedidoCliente(edit, username);

        assertThat(res).isSameAs(actual);
        assertThat(actual.getLineaPedidos()).hasSize(1);
        LineaPedido nueva = actual.getLineaPedidos().iterator().next();
        assertThat(nueva.getProducto().getCodigo()).isEqualTo("PR-1");
        assertThat(nueva.getCantidad()).isEqualTo(2);
        assertThat(actual.getModificadoPor()).isEqualTo(username);
        verify(pedidoRepo).save(actual);
    }

    @Test
    void cargarDetalleCliente_ok() {
        Pedido p = pedidoBase("P1");
        stubCargarDetalleCliente("P1", "u1", p);
        assertThat(service.cargarDetalleCliente("P1", "u1")).isSameAs(p);
    }

    @Test
    void cargarDetalleCliente_falla_validacion() {
        assertThatThrownBy(() -> service.cargarDetalleCliente(" ", "u1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.cargarDetalleCliente("P1", " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelarPedido_ok_si_cancelable() {
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);

        stubCargarDetalle("P1", p);

        Pedido res = service.cancelarPedido("P1", "motivo", "cam1");

        assertThat(res.getEstado()).isEqualTo(EstadoPedido.ANULADO);
        assertThat(res.getEstadoCocina()).isEqualTo(EstadoCocina.CANCELADO);
        assertThat(res.getCanceladoPor()).isEqualTo("cam1");
        assertThat(res.getMotivoCancelacion()).isEqualTo("motivo");
        assertThat(res.getFechaCancelacion()).isNotNull();
        verify(pedidoRepo).save(p);
    }

    @Test
    void cancelarPedido_falla_si_no_cancelable() {
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_COCINA);
        p.setEstadoCocina(EstadoCocina.ACEPTADO);

        stubCargarDetalle("P1", p);

        assertThatThrownBy(() -> service.cancelarPedido("P1", "m", "cam1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se puede cancelar el pedido");
    }

    @Test
    void puedeModificarCliente_true_solo_si_pendiente_aceptacion_y_no_anulado() {
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        assertThat(service.puedeModificarCliente(p)).isTrue();

        p.setEstado(EstadoPedido.ANULADO);
        assertThat(service.puedeModificarCliente(p)).isFalse();

        p.setEstado(EstadoPedido.EN_CURSO);
        p.setEstadoCocina(EstadoCocina.ACEPTADO);
        assertThat(service.puedeModificarCliente(p)).isFalse();

        assertThat(service.puedeModificarCliente(null)).isFalse();
    }

    @Test
    void listarPedidosModificables_ok() {
        when(pedidoRepo.findByEstadoOrEstadoAndEstadoCocina(
                EstadoPedido.EN_CURSO,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        )).thenReturn(List.of(pedidoBase("P1")));

        assertThat(service.listarPedidosModificables()).hasSize(1);
    }

    @Test
    void listarPedidosModificablesPorMesa_ok() {
        when(pedidoRepo.findByReservaMesa_NumeroMesaAndEstadoOrReservaMesa_NumeroMesaAndEstadoAndEstadoCocina(
                2,
                EstadoPedido.EN_CURSO,
                2,
                EstadoPedido.EN_COCINA,
                EstadoCocina.PENDIENTE_ACEPTACION
        )).thenReturn(List.of(pedidoBase("P1")));

        assertThat(service.listarPedidosModificablesPorMesa(2)).hasSize(1);
    }

    @Test
    void listarPedidosModificablesPorMesa_falla_si_invalida() {
        assertThatThrownBy(() -> service.listarPedidosModificablesPorMesa(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listarPedidosCliente_ok() {
        when(pedidoRepo.findByCliente_UsernameOrderByFechaCreacionDesc("u1"))
                .thenReturn(List.of(pedidoBase("P1")));
        assertThat(service.listarPedidosCliente("u1")).hasSize(1);
    }

    @Test
    void listarPedidosCliente_falla_si_invalido() {
        assertThatThrownBy(() -> service.listarPedidosCliente(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cancelarPedidoCliente_ok_default_motivo() {
        Pedido p = pedidoBase("P1");
        Cliente c = cliente("u1");
        p.setCliente(c);

        stubCargarDetalleCliente("P1", "u1", p);
        stubCargarDetalle("P1", p);

        service.cancelarPedidoCliente("P1", "   ", "u1");

        assertThat(p.getMotivoCancelacion()).isEqualTo("Cancelado por cliente");
        assertThat(p.getCanceladoPor()).isEqualTo("u1");
    }

    // Cliente: creación

    @Test
    void crearPedidoClienteRecoger_ok() {
        Pedido carrito = pedidoBase("CAR");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        carrito.getLineaPedidos().add(lineaConProducto(carrito, prod, 2, "LP-1"));

        when(clienteRepo.findByUsername("u1")).thenReturn(Optional.of(cliente("u1")));
        when(productoRepo.findByCodigo("PR-1")).thenReturn(Optional.of(prod));

        Pedido res = service.crearPedidoClienteRecoger(carrito, "u1");

        assertThat(res.getTipoPedido()).isEqualTo(TipoPedidoCliente.RECOGER);
        assertThat(res.getEstado()).isEqualTo(EstadoPedido.EN_CURSO);
        assertThat(res.getEstadoReparto()).isEqualTo(EstadoReparto.NO_APLICA);
        assertThat(res.getDireccionEntrega()).isNull();
        assertThat(res.getLineaPedidos()).isNotEmpty();
    }

    @Test
    void crearPedidoClienteDomicilio_ok() {
        Pedido carrito = pedidoBase("CAR");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        carrito.getLineaPedidos().add(lineaConProducto(carrito, prod, 1, "LP-1"));

        when(clienteRepo.findByUsername("u1")).thenReturn(Optional.of(cliente("u1")));
        when(productoRepo.findByCodigo("PR-1")).thenReturn(Optional.of(prod));

        Pedido res = service.crearPedidoClienteDomicilio(carrito, "u1", " Calle 1 ");

        assertThat(res.getTipoPedido()).isEqualTo(TipoPedidoCliente.DOMICILIO);
        assertThat(res.getDireccionEntrega()).isEqualTo("Calle 1");
        assertThat(res.getEstadoReparto()).isEqualTo(EstadoReparto.PENDIENTE_ASIGNACION);
    }

    @Test
    void crearPedidoClienteDomicilio_falla_si_direccion_vacia() {
        Pedido carrito = pedidoBase("CAR");
        carrito.getLineaPedidos().add(lineaConProducto(carrito, producto("PR-1", BigDecimal.ONE), 1, "LP-1"));

        assertThatThrownBy(() -> service.crearPedidoClienteDomicilio(carrito, "u1", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dirección de entrega");
    }

    @Test
    void crearPedidoClienteMesa_ok_envia_directo_a_cocina() {
        Pedido carrito = pedidoBase("CAR");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        carrito.getLineaPedidos().add(lineaConProducto(carrito, prod, 1, "LP-1"));

        when(clienteRepo.findByUsername("u1")).thenReturn(Optional.of(cliente("u1")));
        when(productoRepo.findByCodigo("PR-1")).thenReturn(Optional.of(prod));

        ReservaMesa mesa = new ReservaMesa(3);
        when(reservaMesaRepo.findByNumeroMesaAndEstado(3, EstadoReservaMesa.ABIERTA)).thenReturn(Optional.of(mesa));

        Pedido res = service.crearPedidoClienteMesa(carrito, "u1", 3);

        assertThat(res.getTipoPedido()).isEqualTo(TipoPedidoCliente.MESA);
        assertThat(res.getEstado()).isEqualTo(EstadoPedido.EN_COCINA);
        assertThat(res.getReservaMesa()).isNotNull();
    }

    // Carrito -> pedido persistido / línea personalizada

    @Test
    void volcarCarritoEnPedido_delega_en_carritoService() {
        Pedido carrito = pedidoBase("CAR");
        service.volcarCarritoEnPedido("P1", carrito);
        verify(carritoService).volcarCarritoEnPedido("P1", carrito);
    }

    @Test
    void agregarLineaPersonalizada_ok() {
        Pedido pedido = pedidoBase("P1");
        stubCargarDetalle("P1", pedido);

        Producto prod = producto("PR-1", new BigDecimal("2.00"));
        when(productoRepo.findByCodigo("PR-1")).thenReturn(Optional.of(prod));

        LineaPedido personalizada = lineaConProducto(null, prod, 3, "LP-X");

        Pedido res = service.agregarLineaPersonalizada("P1", personalizada);

        assertThat(res).isSameAs(pedido);
        assertThat(pedido.getLineaPedidos()).hasSize(1);
        verify(pedidoRepo).save(pedido);
    }

    // Pago online

    @Test
    void iniciarPagoOnline_ok() {
        Pedido carrito = pedidoBase("CAR");
        carrito.getLineaPedidos().add(lineaConProducto(carrito, producto("PR-1", BigDecimal.ONE), 1, "LP-1"));

        when(clienteRepo.findByUsername("u1")).thenReturn(Optional.of(cliente("u1")));
        when(productoRepo.findByCodigo("PR-1")).thenReturn(Optional.of(producto("PR-1", BigDecimal.ONE)));

        Pago pago = new Pago(pedidoBase("X"), MetodoPago.TARJETA, new BigDecimal("10.00"));
        when(pagoService.iniciarPago(any(Pedido.class), eq(MetodoPago.TARJETA))).thenReturn(pago);

        Pago res = service.iniciarPagoOnline(carrito, "u1", MetodoPago.TARJETA);

        assertThat(res).isSameAs(pago);
        verify(pagoService).iniciarPago(any(Pedido.class), eq(MetodoPago.TARJETA));
    }

    @Test
    void iniciarPagoOnline_falla_si_metodo_null() {
        assertThatThrownBy(() -> service.iniciarPagoOnline(pedidoBase("CAR"), "u1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Método de pago inválido");
    }

    @Test
    void obtenerPagoCliente_ok() {
        Cliente c = cliente("u1");
        Pedido p = pedidoBase("P1");
        p.setCliente(c);

        Pago pago = new Pago(p, MetodoPago.PAYPAL, BigDecimal.TEN);
        when(pagoRepo.findById(1L)).thenReturn(Optional.of(pago));

        Pago res = service.obtenerPagoCliente(1L, "u1");
        assertThat(res).isSameAs(pago);
    }

    @Test
    void obtenerPagoCliente_falla_si_no_pertenece() {
        Cliente c = cliente("otro");
        Pedido p = pedidoBase("P1");
        p.setCliente(c);

        Pago pago = new Pago(p, MetodoPago.PAYPAL, BigDecimal.TEN);
        when(pagoRepo.findById(1L)).thenReturn(Optional.of(pago));

        assertThatThrownBy(() -> service.obtenerPagoCliente(1L, "u1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pago no pertenece al cliente");
    }

    @Test
    void confirmarPagoOnline_ok() {
        String username = "u1";
        Cliente c = cliente(username);

        Pedido p = pedidoBase("P1");
        p.setCliente(c);
        p.getLineaPedidos().add(lineaConProducto(p, producto("PR-1", BigDecimal.ONE), 1, "LP-1"));
        stubCargarDetalleCliente("P1", username, p);

        Pago pago = new Pago(p, MetodoPago.TARJETA, BigDecimal.TEN);
        when(pagoRepo.findById(10L)).thenReturn(Optional.of(pago));

        Pago pagoConfirmado = new Pago(p, MetodoPago.TARJETA, BigDecimal.TEN);
        pagoConfirmado.confirmar("REF");

        when(pagoService.confirmarPago(nullable(Long.class), eq("REF"))).thenReturn(pagoConfirmado);

        Pedido res = service.confirmarPagoOnline(10L, username, "REF");

        assertThat(res).isSameAs(p);
        assertThat(p.getEstado()).isEqualTo(EstadoPedido.EN_COCINA);
        verify(pedidoRepo).save(p);
    }

    @Test
    void confirmarPagoOnline_falla_si_pago_confirmado() {
        String username = "u1";
        Cliente c = cliente(username);

        Pedido p = pedidoBase("P1");
        p.setCliente(c);

        Pago pago = new Pago(p, MetodoPago.TARJETA, BigDecimal.TEN);
        pago.confirmar("X");
        when(pagoRepo.findById(10L)).thenReturn(Optional.of(pago));

        assertThatThrownBy(() -> service.confirmarPagoOnline(10L, username, "REF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ya está confirmado");
    }

    @Test
    void marcarPagoOnlineFallido_ok() {
        String username = "u1";
        Cliente c = cliente(username);

        Pedido p = pedidoBase("P1");
        p.setCliente(c);
        stubCargarDetalleCliente("P1", username, p);

        Pago pago = new Pago(p, MetodoPago.TARJETA, BigDecimal.TEN);
        when(pagoRepo.findById(10L)).thenReturn(Optional.of(pago));

        Pedido res = service.marcarPagoOnlineFallido(10L, username, " ");

        assertThat(res).isSameAs(p);
        verify(pagoService).marcarPagoFallido(eq(pago.getId()), eq("Cancelado por el cliente"));
    }

    // Cocina

    @Test
    void obtenerPedidoPorId_ok() {
        UUID id = UUID.randomUUID();
        Pedido p = pedidoBase("P1");
        when(pedidoRepo.findWithDetalleById(id)).thenReturn(Optional.of(p));
        assertThat(service.obtenerPedidoPorId(id)).isSameAs(p);
    }

    @Test
    void obtenerPedidoPorId_falla_si_null() {
        assertThatThrownBy(() -> service.obtenerPedidoPorId(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void cambiarEstadoCocina_ok_listo_setea_reparto_domicilio() {
        UUID id = UUID.randomUUID();
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_COCINA);
        p.setTipoPedido(TipoPedidoCliente.DOMICILIO);

        when(pedidoRepo.findWithDetalleById(id)).thenReturn(Optional.of(p));

        Pedido res = service.cambiarEstadoCocina(id, EstadoCocina.LISTO);

        assertThat(res.getEstadoCocina()).isEqualTo(EstadoCocina.LISTO);
        assertThat(res.getEstadoReparto()).isEqualTo(EstadoReparto.PENDIENTE_ASIGNACION);
        verify(pedidoRepo).save(p);
    }

    @Test
    void cambiarEstadoCocina_ok_cancelado_anula_pedido() {
        UUID id = UUID.randomUUID();
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_COCINA);

        when(pedidoRepo.findWithDetalleById(id)).thenReturn(Optional.of(p));

        Pedido res = service.cambiarEstadoCocina(id, EstadoCocina.CANCELADO);

        assertThat(res.getEstado()).isEqualTo(EstadoPedido.ANULADO);
        assertThat(res.getEstadoCocina()).isEqualTo(EstadoCocina.CANCELADO);
        assertThat(res.getEstadoReparto()).isEqualTo(EstadoReparto.NO_APLICA);
        verify(pedidoRepo).save(p);
    }

    @Test
    void cambiarEstadoCocina_devuelve_mismo_si_igual_estado() {
        UUID id = UUID.randomUUID();
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_COCINA);
        p.setEstadoCocina(EstadoCocina.ACEPTADO);

        when(pedidoRepo.findWithDetalleById(id)).thenReturn(Optional.of(p));

        Pedido res = service.cambiarEstadoCocina(id, EstadoCocina.ACEPTADO);

        assertThat(res).isSameAs(p);
        verify(pedidoRepo, never()).save(any());
    }

    @Test
    void buscarPedidosFiltrados_delega_repo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Pedido> page = new PageImpl<>(List.of(pedidoBase("P1")));
        when(pedidoRepo.buscarPedidosFiltrados(any(), any(), any(), any(), any(), eq(pageable))).thenReturn(page);

        Page<Pedido> res = service.buscarPedidosFiltrados(null, null, null, null, null, pageable);

        assertThat(res.getContent()).hasSize(1);
    }

    // Camarero

    @Test
    void puedeEditarOCancelarCamarero_ok() {
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        assertThat(service.puedeEditarOCancelarCamarero(p)).isTrue();

        p.setEstado(EstadoPedido.ANULADO);
        assertThat(service.puedeEditarOCancelarCamarero(p)).isFalse();
    }

    @Test
    void cancelarPedidoCamarero_ok_usa_securityContext_y_default_motivo() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("cam1", "x")
        );

        Pedido p = pedidoBase("P1");
        stubCargarDetalle("P1", p);

        service.cancelarPedidoCamarero("P1", " ");

        assertThat(p.getCanceladoPor()).isEqualTo("cam1");
        assertThat(p.getMotivoCancelacion()).isEqualTo("Cancelado por camarero");
    }

    @Test
    void cargarPedidoEditableCamarero_ok() {
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        stubCargarDetalle("P1", p);

        Pedido res = service.cargarPedidoEditableCamarero("P1", "cam1");

        assertThat(res).isSameAs(p);
    }

    @Test
    void cargarPedidoEditableCamarero_falla_si_no_editable() {
        Pedido p = pedidoBase("P1");
        p.setEstado(EstadoPedido.EN_COCINA);
        p.setEstadoCocina(EstadoCocina.ACEPTADO);
        stubCargarDetalle("P1", p);

        assertThatThrownBy(() -> service.cargarPedidoEditableCamarero("P1", "cam1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // Utilidades de líneas / ingredientes

    @Test
    void ordenarLineasParaVista_ok_orden_por_codigo_case_insensitive_y_nulls_last() {
        Pedido p = pedidoBase("P1");
        LineaPedido a = lineaConProducto(p, producto("PR", BigDecimal.ONE), 1, "b");
        LineaPedido b = lineaConProducto(p, producto("PR", BigDecimal.ONE), 1, "A");
        LineaPedido c = lineaConProducto(p, producto("PR", BigDecimal.ONE), 1, null);

        List<LineaPedido> res = service.ordenarLineasParaVista(new LinkedHashSet<>(List.of(a, b, c)));

        assertThat(res).extracting(LineaPedido::getCodigo).containsExactly("A", "b", null);
    }

    @Test
    void obtenerIngredientesDisponiblesLinea_devuelve_de_producto_si_existe_receta() {
        Pedido p = pedidoBase("P1");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        LineaPedido lp = lineaConProducto(p, prod, 1, "LP-1");

        Ingrediente i1 = new Ingrediente();
        setIngredienteId(i1, UUID.randomUUID());
        i1.setNombre("Zeta");

        Ingrediente i2 = new Ingrediente();
        setIngredienteId(i2, UUID.randomUUID());
        i2.setNombre("alfa");

        ProductoIngrediente pi1 = new ProductoIngrediente(prod, i1, true, true, BigDecimal.ZERO);
        ProductoIngrediente pi2 = new ProductoIngrediente(prod, i2, true, true, BigDecimal.ZERO);

        Producto prodFetch = producto("PR-1", BigDecimal.ONE);
        prodFetch.setIngredientes(List.of(pi1, pi2));

        when(productoRepo.findWithIngredientesByCodigo("PR-1")).thenReturn(Optional.of(prodFetch));

        List<Ingrediente> res = service.obtenerIngredientesDisponiblesLinea(lp);

        assertThat(res).extracting(Ingrediente::getNombre).containsExactly("alfa", "Zeta");
    }

    @Test
    void obtenerIngredientesDisponiblesLinea_fallback_a_linea_si_producto_sin_ingredientes() {
        Pedido p = pedidoBase("P1");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        LineaPedido lp = lineaConProducto(p, prod, 1, "LP-1");

        Producto prodFetch = producto("PR-1", BigDecimal.ONE);
        prodFetch.setIngredientes(List.of());

        when(productoRepo.findWithIngredientesByCodigo("PR-1")).thenReturn(Optional.of(prodFetch));

        Ingrediente i1 = new Ingrediente();
        setIngredienteId(i1, UUID.randomUUID());
        i1.setNombre("b");

        LineaPedidoIngrediente sel = new LineaPedidoIngrediente(lp, i1, true, 0, BigDecimal.ZERO);
        lp.setIngredientes(new LinkedHashSet<>(Set.of(sel)));

        List<Ingrediente> res = service.obtenerIngredientesDisponiblesLinea(lp);

        assertThat(res).extracting(Ingrediente::getNombre).containsExactly("b");
    }

    @Test
    void aplicarCantidadLinea_ok() {
        Pedido p = pedidoBase("P1");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        LineaPedido lp = lineaConProducto(p, prod, 1, "LP-1");
        p.getLineaPedidos().add(lp);

        service.aplicarCantidadLinea(p, "LP-1", 5);

        assertThat(lp.getCantidad()).isEqualTo(5);
    }

    @Test
    void aplicarCantidadLinea_falla_validaciones() {
        assertThatThrownBy(() -> service.aplicarCantidadLinea(null, "LP-1", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.aplicarCantidadLinea(pedidoBase("P1"), " ", 1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.aplicarCantidadLinea(pedidoBase("P1"), "LP-1", 0)).isInstanceOf(IllegalArgumentException.class);

        Pedido p = pedidoBase("P1");
        p.setLineaPedidos(null);
        assertThatThrownBy(() -> service.aplicarCantidadLinea(p, "LP-1", 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eliminarLinea_ok() {
        Pedido p = pedidoBase("P1");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        LineaPedido lp = lineaConProducto(p, prod, 1, "LP-1");
        p.getLineaPedidos().add(lp);

        service.eliminarLinea(p, "LP-1");

        assertThat(p.getLineaPedidos()).isEmpty();
    }

    @Test
    void eliminarLinea_falla_si_no_encontrada() {
        Pedido p = pedidoBase("P1");
        p.getLineaPedidos().add(lineaConProducto(p, producto("PR-1", BigDecimal.ONE), 1, "LP-1"));

        assertThatThrownBy(() -> service.eliminarLinea(p, "LP-X"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Línea no encontrada");
    }

    @Test
    void obtenerSeleccionIngrediente_ok() {
        Pedido p = pedidoBase("P1");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        LineaPedido lp = lineaConProducto(p, prod, 1, "LP-1");

        Ingrediente ing = new Ingrediente();
        UUID ingId = UUID.randomUUID();
        setIngredienteId(ing, ingId);
        ing.setNombre("A");

        LineaPedidoIngrediente sel = new LineaPedidoIngrediente(lp, ing, true, 0, BigDecimal.ZERO);
        lp.setIngredientes(new LinkedHashSet<>(Set.of(sel)));

        assertThat(service.obtenerSeleccionIngrediente(lp, ingId)).isSameAs(sel);
        assertThat(service.obtenerSeleccionIngrediente(lp, UUID.randomUUID())).isNull();
    }

    @Test
    void aplicarSeleccionIngrediente_crea_nuevo_si_no_existe_y_normaliza_extraCantidad() {
        Pedido p = pedidoBase("P1");
        LineaPedido lp = lineaConProducto(p, producto("PR-1", BigDecimal.ONE), 1, "LP-1");
        lp.setIngredientes(new LinkedHashSet<>());

        Ingrediente ing = new Ingrediente();
        UUID id = UUID.randomUUID();
        setIngredienteId(ing, id);
        ing.setNombre("X");
        ing.setPrecioExtra(new BigDecimal("0.50"));

        service.aplicarSeleccionIngrediente(lp, ing, false, -2);

        assertThat(lp.getIngredientes()).hasSize(1);
        LineaPedidoIngrediente creado = lp.getIngredientes().iterator().next();
        assertThat(creado.isIncluido()).isFalse();
        assertThat(creado.getExtraCantidad()).isEqualTo(0);
        assertThat(creado.getPrecioExtra()).isEqualByComparingTo("0.50");
    }

    @Test
    void aplicarSeleccionIngrediente_actualiza_existente_y_setea_precio_extra_si_null() {
        Pedido p = pedidoBase("P1");
        LineaPedido lp = lineaConProducto(p, producto("PR-1", BigDecimal.ONE), 1, "LP-1");

        Ingrediente ing = new Ingrediente();
        UUID id = UUID.randomUUID();
        setIngredienteId(ing, id);
        ing.setPrecioExtra(new BigDecimal("0.30"));

        LineaPedidoIngrediente existente = new LineaPedidoIngrediente(lp, ing, true, 0, BigDecimal.ZERO);
        forceLineaPedidoIngredientePrecioExtraNull(existente);
        lp.setIngredientes(new LinkedHashSet<>(Set.of(existente)));

        service.aplicarSeleccionIngrediente(lp, ing, false, 3);

        assertThat(existente.isIncluido()).isFalse();
        assertThat(existente.getExtraCantidad()).isEqualTo(3);
        assertThat(existente.getPrecioExtra()).isEqualByComparingTo("0.30");
    }

    @Test
    void obtenerRecetaPorIngrediente_ok_mapea_por_id() {
        Pedido p = pedidoBase("P1");
        Producto prod = producto("PR-1", BigDecimal.ONE);
        LineaPedido lp = lineaConProducto(p, prod, 1, "LP-1");

        Ingrediente ing1 = new Ingrediente();
        UUID id1 = UUID.randomUUID();
        setIngredienteId(ing1, id1);

        ProductoIngrediente pi = new ProductoIngrediente(prod, ing1, true, true, BigDecimal.ZERO);

        when(productoRepo.findByProductoCodigoFetchIngrediente("PR-1")).thenReturn(List.of(pi));

        Map<UUID, ProductoIngrediente> res = service.obtenerRecetaPorIngrediente(lp);

        assertThat(res).containsKey(id1);
        assertThat(res.get(id1)).isSameAs(pi);
    }

    // Ajustes de pago

    @Test
    void confirmarCambiosPedidoClienteConAjuste_ok_calcula_totales_y_llama_servicio() {
        String codigo = "P1";
        String username = "u1";

        Cliente c = cliente(username);
        Pedido actual = pedidoBase(codigo);
        actual.setCliente(c);
        actual.setEstado(EstadoPedido.EN_CURSO);
        actual.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        actual.setLineaPedidos(new LinkedHashSet<>());

        Pedido edit = pedidoBase(codigo);
        Producto prod = producto("PR-1", new BigDecimal("2.00"));
        edit.setLineaPedidos(new LinkedHashSet<>(Set.of(lineaConProducto(edit, prod, 2, "LP-E"))));

        stubCargarDetalleCliente(codigo, username, actual);
        when(productoRepo.findByCodigo("PR-1")).thenReturn(Optional.of(prod));

        when(pedidoCalculoService.calcularTotalPedido(actual)).thenReturn(new BigDecimal("5.00"), new BigDecimal("7.00"));
        when(pagoRepo.findByPedido_Codigo(codigo)).thenReturn(Optional.empty());

        AjustePagoDTO dto = mock(AjustePagoDTO.class);
        when(ajustePagoService.calcularYCrearOActualizarAjuste(eq(actual), isNull(), eq(new BigDecimal("5.00")), eq(new BigDecimal("7.00"))))
                .thenReturn(dto);

        AjustePagoDTO res = service.confirmarCambiosPedidoClienteConAjuste(edit, username);

        assertThat(res).isSameAs(dto);
        verify(ajustePagoService).calcularYCrearOActualizarAjuste(eq(actual), isNull(),
                eq(new BigDecimal("5.00")), eq(new BigDecimal("7.00")));
        verify(pedidoRepo).save(actual);
    }

    @Test
    void prepararAjusteCambiosCliente_ok_no_persiste_pedido_pero_llama_ajuste() {
        String codigo = "P1";
        String username = "u1";

        Cliente c = cliente(username);
        Pedido pedidoBd = pedidoBase(codigo);
        pedidoBd.setCliente(c);

        Pedido edit = pedidoBase(codigo);

        stubCargarDetalleCliente(codigo, username, pedidoBd);

        when(pedidoCalculoService.calcularTotalPedido(any(Pedido.class))).thenAnswer(inv -> {
            Pedido arg = inv.getArgument(0);
            if (arg == pedidoBd) return new BigDecimal("10.00");
            if (arg == edit) return new BigDecimal("12.00");
            return BigDecimal.ZERO;
        });

        AjustePagoDTO dto = mock(AjustePagoDTO.class);

        doReturn(dto).when(ajustePagoService).calcularYCrearOActualizarAjuste(
                any(Pedido.class),
                any(),
                any(BigDecimal.class),
                any(BigDecimal.class)
        );

        AjustePagoDTO res = service.prepararAjusteCambiosCliente(edit, username);

        assertThat(res).isSameAs(dto);
        verify(pedidoRepo, never()).save(any());

        ArgumentCaptor<Pedido> pedidoCaptor = ArgumentCaptor.forClass(Pedido.class);
        ArgumentCaptor<Pago> pagoCaptor = ArgumentCaptor.forClass(Pago.class);
        ArgumentCaptor<BigDecimal> anteriorCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        ArgumentCaptor<BigDecimal> nuevoCaptor = ArgumentCaptor.forClass(BigDecimal.class);

        verify(ajustePagoService).calcularYCrearOActualizarAjuste(
                pedidoCaptor.capture(),
                pagoCaptor.capture(),
                anteriorCaptor.capture(),
                nuevoCaptor.capture()
        );

        assertThat(pedidoCaptor.getValue()).isSameAs(pedidoBd);
        assertThat(pagoCaptor.getValue()).isNull();
        assertThat(anteriorCaptor.getValue()).isEqualByComparingTo("10.00");
        assertThat(nuevoCaptor.getValue()).isEqualByComparingTo("12.00");
    }

}