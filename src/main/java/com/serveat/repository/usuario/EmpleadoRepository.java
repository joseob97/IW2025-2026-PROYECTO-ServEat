package com.serveat.repository.usuario;

import com.serveat.domain.usuario.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {

    Optional<Empleado> findByUsername(String username);

    Optional<Empleado> findByEmail(String email);

    List<Empleado> findByActivoTrue();

    List<Empleado> findByActivoFalse();

    List<Empleado> findByRol(String rol);
}