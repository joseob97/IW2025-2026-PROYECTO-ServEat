package com.serveat.service.usuario;

import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import java.util.List;

public interface EmpleadoService {

    // =========================
    // CONSULTAS
    // =========================
    Empleado obtenerPorId(Long id);

    Empleado obtenerPorUsername(String username);

    List<Empleado> obtenerTodos();

    List<Empleado> obtenerPorRol(String rol);


    // =========================
    // GESTIÓN
    // =========================
    Empleado guardar(Empleado empleado);

    void activar(Empleado empleado);

    void desactivar(Empleado empleado);

    void eliminar(Empleado empleado);
}

