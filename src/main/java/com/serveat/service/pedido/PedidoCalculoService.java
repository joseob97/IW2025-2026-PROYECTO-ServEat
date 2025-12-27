package com.serveat.service.pedido;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.Pedido;

import java.math.BigDecimal;

public interface PedidoCalculoService {

    BigDecimal calcularPrecioLinea(LineaPedido linea);

    BigDecimal calcularTotalPedido(Pedido pedido);
}