package com.serveat.service.usuario;

import com.serveat.domain.usuario.Cliente;
import java.util.List;

public interface ClienteService {

    Cliente obtenerPorUsername(String username);

    List<Cliente> obtenerTodos();

    Cliente guardar(Cliente cliente);

    void desactivar(Cliente cliente);

    void eliminar(Cliente cliente);
}