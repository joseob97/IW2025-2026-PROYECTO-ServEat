package com.serveat.repository.pedido;

import com.serveat.domain.pedido.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    List<Pedido> findByEstado(String estado);
    List<Pedido> findByRangoFechaBetween(LocalDate fecha);
    List<Pedido> findByEstados(List<String> estados);
    List<Pedido> findByEstadoNot(String estado);
}