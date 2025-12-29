package com.serveat.repository.pago;

import com.serveat.domain.pago.ajuste.AjustePago;
import com.serveat.domain.pago.ajuste.EstadoAjustePago;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AjustePagoRepository extends JpaRepository<AjustePago, UUID> {

    @EntityGraph(attributePaths = {"pedido"})
    Optional<AjustePago> findByCodigo(String codigo);

    @EntityGraph(attributePaths = {"pedido"})
    List<AjustePago> findByPedido_CodigoOrderByFechaCreacionDesc(String codigoPedido);

    @EntityGraph(attributePaths = {"pedido"})
    Optional<AjustePago> findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(String codigoPedido, EstadoAjustePago estado);

    boolean existsByPedido_CodigoAndEstado(String codigoPedido, EstadoAjustePago estado);
}