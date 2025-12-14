package com.serveat.service.usuario;

import com.serveat.domain.usuario.Empleado;

import java.util.List;
import java.util.Optional;

public interface EmpleadoService {

    List<Empleado> findAll();

    Optional<Empleado> findById(Long id);

    Empleado save(Empleado empleado);

    void delete(Empleado empleado);

    void updatePassword(Empleado empleado, String rawPassword);
}
