package com.serveat.service.pedido;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface PedidoService {

    /* Empleados / backoffice */

    Pedido crearPedidoMesa(Integer numeroMesa);

    Pedido obtenerPorCodigo(String codigo);

    List<Pedido> listarPedidos();

    List<Pedido> listarTodosOrdenadosPorFecha();

    List<Pedido> buscarPorEstado(EstadoPedido estado);

    List<Pedido> obtenerPedidosPorEstado(EstadoCocina estado);

    List<Pedido> obtenerPedidosPorMesa(Integer numeroMesa);

    List<Pedido> obtenerPedidosPorEstadoYMesa(EstadoCocina estado, Integer numeroMesa);

    /* Persistencia simple (por producto). Para personalizaciones, usar PedidoCarritoService por codigoLinea. */

    Pedido agregarProducto(String codigoPedido, String codigoProducto, int cantidad);

    Pedido actualizarCantidadProducto(String codigoPedido, String codigoProducto, int nuevaCantidad);

    Pedido eliminarProducto(String codigoPedido, String codigoProducto);

    /* Confirmaciones / edición */

    Pedido confirmarPedido(String codigoPedido);

    Pedido confirmarCambiosPedido(Pedido pedidoEditado, String usuario);

    Pedido confirmarCambiosPedidoCliente(Pedido pedidoEditado, String username);

    Pedido cargarDetalleCliente(String codigo, String username);

    Pedido cancelarPedido(String codigoPedido, String motivo, String camareroUsername);

    boolean puedeModificarCliente(Pedido pedido);

    List<Pedido> listarPedidosModificables();

    List<Pedido> listarPedidosModificablesPorMesa(Integer numeroMesa);

    List<Pedido> listarPedidosCliente(String username);

    Pedido cancelarPedidoCliente(String codigoPedido, String motivo, String username);

    /* Cliente: creación */

    Pedido crearPedidoClienteRecoger(Pedido carrito, String username);

    Pedido crearPedidoClienteDomicilio(Pedido carrito, String username, String direccionEntrega);

    Pedido crearPedidoClienteMesa(Pedido carrito, String username, Integer numeroMesa);

    /* Carrito, pedido persistido (respetando personalización) */

    void volcarCarritoEnPedido(String codigoPedido, Pedido carrito);

    Pedido agregarLineaPersonalizada(String codigoPedido, LineaPedido lineaPersonalizada);

    /* Pago online */

    Pago iniciarPagoOnline(Pedido carrito, String username, MetodoPago metodo);

    Pago obtenerPagoCliente(Long pagoId, String username);

    Pedido confirmarPagoOnline(Long pagoId, String username, String referencia);

    Pedido marcarPagoOnlineFallido(Long pagoId, String username, String motivo);

    com.serveat.service.pago.dto.AjustePagoDTO confirmarCambiosPedidoClienteConAjuste(Pedido pedidoEditado, String username);

    /* Cocina */

    Pedido obtenerPedidoPorId(UUID id);

    Pedido cambiarEstadoCocina(UUID id, EstadoCocina nuevoEstado);

    Page<Pedido> buscarPedidosFiltrados(LocalDateTime desde,
                                        LocalDateTime hasta,
                                        EstadoPedido estadoPedido,
                                        EstadoCocina estadoCocina,
                                        Integer mesa,
                                        Pageable pageable);

    // Editar pedido (camarero)

    boolean puedeEditarOCancelarCamarero(Pedido pedido);

    Pedido cancelarPedidoCamarero(String codigoPedido, String motivo);

    Pedido cargarPedidoEditableCamarero(String codigoPedido, String username);

    List<LineaPedido> ordenarLineasParaVista(Set<LineaPedido> lineas);

    List<Ingrediente> obtenerIngredientesDisponiblesLinea(LineaPedido lp);

    void aplicarCantidadLinea(Pedido pedido, String codigoLinea, int nuevaCantidad);

    void eliminarLinea(Pedido pedido, String codigoLinea);

    LineaPedidoIngrediente obtenerSeleccionIngrediente(LineaPedido lp, UUID ingredienteId);

    void aplicarSeleccionIngrediente(LineaPedido lp, Ingrediente ingrediente, boolean incluido, int extraCantidad);

    Map<UUID, ProductoIngrediente> obtenerRecetaPorIngrediente(LineaPedido lp);
}