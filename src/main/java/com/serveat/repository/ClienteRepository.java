package com.serveat.repository;

import com.serveat.domain.usuario.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Object> {

    Optional<Cliente> findByUsername(String username);
}
