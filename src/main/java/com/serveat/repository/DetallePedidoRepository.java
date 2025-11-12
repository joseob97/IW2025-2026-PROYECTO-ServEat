package com.serveat.repository;

import com.serveat.domain.pedido.DetallePedido;
import com.serveat.domain.pedido.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DetallePedidoRepository extends JpaRepository<DetallePedido, UUID> {
    List<DetallePedido> findByPedido(Pedido pedido);
}
