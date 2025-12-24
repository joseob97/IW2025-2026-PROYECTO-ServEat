package com.serveat.repository.usuario;

import com.serveat.domain.usuario.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByUsername(String username);

    Optional<Cliente> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByActivoFalse();
}
