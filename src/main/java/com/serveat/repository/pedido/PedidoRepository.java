package com.serveat.repository.pedido;

import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    Optional<Pedido> findByCodigo(String codigo);

    List<Pedido> findByEstado(EstadoPedido estado);

    List<Pedido> findByEstadoIn(List<EstadoPedido> estados);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos"
    })
    Optional<Pedido> findWithDetalleByCodigo(String codigo);
}