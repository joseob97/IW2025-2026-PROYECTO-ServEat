package com.serveat.service.pedido.impl;

import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.service.pedido.PedidoCalculoService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PedidoCalculoServiceImpl implements PedidoCalculoService {

    @Override
    public BigDecimal calcularPrecioLinea(LineaPedido linea) {
        if (linea == null) return BigDecimal.ZERO;

        if (linea.getProducto() == null || linea.getProducto().getPrecio() == null) {
            return BigDecimal.ZERO;
        }

        int cantidad = Math.max(linea.getCantidad(), 0);

        BigDecimal baseUnitario = linea.getProducto().getPrecio();
        BigDecimal base = baseUnitario.multiply(BigDecimal.valueOf(cantidad));

        BigDecimal extrasUnitarios = BigDecimal.ZERO;
        if (linea.getIngredientes() != null) {
            extrasUnitarios = linea.getIngredientes().stream()
                    .map(this::calcularExtraUnitario)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        return base.add(extrasUnitarios.multiply(BigDecimal.valueOf(cantidad)));
    }

    @Override
    public BigDecimal calcularTotalPedido(Pedido pedido) {
        if (pedido == null || pedido.getLineaPedidos() == null) return BigDecimal.ZERO;

        return pedido.getLineaPedidos().stream()
                .map(this::calcularPrecioLinea)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calcularExtraUnitario(LineaPedidoIngrediente li) {
        if (li == null) return BigDecimal.ZERO;
        if (!li.isIncluido()) return BigDecimal.ZERO;

        int extraCant = Math.max(li.getExtraCantidad(), 0);
        if (extraCant <= 0) return BigDecimal.ZERO;

        BigDecimal precioExtra = (li.getPrecioExtra() == null) ? BigDecimal.ZERO : li.getPrecioExtra();
        return precioExtra.multiply(BigDecimal.valueOf(extraCant));
    }
}