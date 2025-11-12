package com.serveat.repository;

import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.usuario.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findByCliente(Cliente cliente);
    List<Pedido> findByEstado(String estado);
}
