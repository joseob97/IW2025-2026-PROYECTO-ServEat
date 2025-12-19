package com.serveat.service.usuario;

import com.serveat.domain.usuario.Cliente;

public interface ClienteService {

    Cliente obtenerPorUsername(String username);

    Cliente obtenerConPedidosPorUsername(String username);
}