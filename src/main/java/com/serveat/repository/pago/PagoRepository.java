package com.serveat.repository.pago;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.Pago;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByPedido_Codigo(String codigoPedido);

    @EntityGraph(attributePaths = {"pedido", "pedido.cliente"})
    Optional<Pago> findWithPedidoById(Long id);

    // Para las estadísticas (facturación)
    long countByEstado(EstadoPago estado);

    @EntityGraph(attributePaths = {"pedido"})
    List<Pago> findByEstado(EstadoPago estado);

    // Para el cierre de caja diario
    List<Pago> findByEstadoAndFechaConfirmacionBetween(EstadoPago estado, LocalDateTime desde, LocalDateTime hasta);
}
