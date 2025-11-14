package com.serveat.repository.pedido;

import com.serveat.domain.pedido.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    Optional<Pedido> findByCodigo(String codigo);

    List<Pedido> findByEstado(String estado);

    List<Pedido> findByEstadoIn(List<String> estados);
}