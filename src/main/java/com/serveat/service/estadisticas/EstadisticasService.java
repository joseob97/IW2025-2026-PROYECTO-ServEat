package com.serveat.service.estadisticas;

import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.TipoPedidoCliente;

import java.math.BigDecimal;
import java.time.Month;
import java.util.List;
import java.util.Map;

public interface EstadisticasService {

    /* Devuelve el número total de pedidos registrados */
    long totalPedidos();

    /* Devuelve el número de pedidos confirmados según la regla de negocio */
    long pedidosConfirmados();

    /* Devuelve el número de pedidos cancelados */
    long pedidosCancelados();

    /* Devuelve el número de pagos confirmados */
    long pagosConfirmados();

    /* Devuelve el importe total facturado sumando pagos confirmados */
    BigDecimal totalFacturado();

    /* Devuelve los años disponibles en función de los pedidos existentes */
    List<Integer> añosDisponibles();

    /* Devuelve todos los meses disponibles para aplicar filtros */
    List<Month> mesesDisponibles();

    /* Devuelve el nombre del mes en español */
    String etiquetaMes(Month m);

    /* Devuelve la etiqueta legible de un método de pago */
    String etiquetaMetodoPago(MetodoPago m);

    /* Devuelve la etiqueta legible del tipo de pedido */
    String etiquetaTipoPedido(TipoPedidoCliente t);

    /* Devuelve la etiqueta legible del estado del pedido */
    String etiquetaEstadoPedido(EstadoPedido e);

    /* Devuelve la etiqueta legible del estado de cocina */
    String etiquetaEstadoCocina(EstadoCocina e);

    /* Mensaje estándar a mostrar cuando no hay resultados */
    String mensajeSinResultados();

    /* Devuelve sugerencias de productos según prefijo y filtros aplicados */
    List<String> sugerirProductos(String prefix,
                                  Integer year, Month month,
                                  TipoPedidoCliente tipoPedido,
                                  MetodoPago metodoPago,
                                  EstadoPedido estadoPedido,
                                  EstadoCocina estadoCocina,
                                  int limit);

    /* Devuelve una página del ranking de productos por unidades vendidas */
    List<Map<String, Object>> topProductosPorUnidadesPage(Integer year, Month month,
                                                          TipoPedidoCliente tipoPedido,
                                                          MetodoPago metodoPago,
                                                          EstadoPedido estadoPedido,
                                                          EstadoCocina estadoCocina,
                                                          String productoExactoOrNull,
                                                          int offset, int limit);

    /* Devuelve el total de productos distintos en el ranking por unidades */
    long topProductosPorUnidadesCount(Integer year, Month month,
                                      TipoPedidoCliente tipoPedido,
                                      MetodoPago metodoPago,
                                      EstadoPedido estadoPedido,
                                      EstadoCocina estadoCocina,
                                      String productoExactoOrNull);

    /* Devuelve una página del ranking de productos por facturación */
    List<Map<String, Object>> topProductosPorFacturacionPage(Integer year, Month month,
                                                             TipoPedidoCliente tipoPedido,
                                                             MetodoPago metodoPago,
                                                             EstadoPedido estadoPedido,
                                                             EstadoCocina estadoCocina,
                                                             String productoExactoOrNull,
                                                             int offset, int limit);

    /* Devuelve el total de productos distintos en el ranking por facturación */
    long topProductosPorFacturacionCount(Integer year, Month month,
                                         TipoPedidoCliente tipoPedido,
                                         MetodoPago metodoPago,
                                         EstadoPedido estadoPedido,
                                         EstadoCocina estadoCocina,
                                         String productoExactoOrNull);

    /* Devuelve el conteo de pedidos agrupados por estado de cocina */
    Map<String, Long> resumenEstadosCocina(Integer year, Month month,
                                           TipoPedidoCliente tipoPedido,
                                           EstadoPedido estadoPedido);
}