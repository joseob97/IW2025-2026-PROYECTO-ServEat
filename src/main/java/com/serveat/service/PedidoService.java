package com.serveat.service;

import com.serveat.domain.menu.Plato;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.usuario.Cliente;
import java.util.List;
import java.util.UUID;

public interface PedidoService {

    Pedido crearPedido(Cliente cliente);
    Pedido agregarPlato(Pedido pedido, Plato plato, int cantidad);
    Pedido guardarPedido(Pedido pedido);
    Pedido obtenerPedidoPorId(UUID id);
    List<Pedido> obtenerPedidosDeCliente(Cliente cliente);
}
