package com.serveat.repository.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PedidoSeguimientoRepository extends JpaRepository<Pedido, UUID> {

    @EntityGraph(attributePaths = {"reservaMesa", "cliente"})
    @Query("""
        select p from Pedido p
        where p.cliente.username = :username
          and p.estado <> com.serveat.domain.pedido.EstadoPedido.ANULADO
          and p.fechaEntrega is null
          and (:desde is null or p.fechaCreacion >= :desde)
          and (:hasta is null or p.fechaCreacion <= :hasta)
          and (:estadoPedido is null or p.estado = :estadoPedido)
          and (:estadoCocina is null or p.estadoCocina = :estadoCocina)
          and (:estadoReparto is null or p.estadoReparto = :estadoReparto)
        order by p.fechaCreacion desc
    """)
    Page<Pedido> buscarActivosClienteFiltrados(@Param("username") String username,
                                               @Param("desde") LocalDateTime desde,
                                               @Param("hasta") LocalDateTime hasta,
                                               @Param("estadoPedido") EstadoPedido estadoPedido,
                                               @Param("estadoCocina") EstadoCocina estadoCocina,
                                               @Param("estadoReparto") EstadoReparto estadoReparto,
                                               Pageable pageable);

    @EntityGraph(attributePaths = {"reservaMesa", "cliente"})
    @Query("""
        select p from Pedido p
        where p.cliente.username = :username
          and (p.estado = com.serveat.domain.pedido.EstadoPedido.ANULADO or p.fechaEntrega is not null)
          and (:desde is null or p.fechaCreacion >= :desde)
          and (:hasta is null or p.fechaCreacion <= :hasta)
          and (:estadoPedido is null or p.estado = :estadoPedido)
          and (:estadoCocina is null or p.estadoCocina = :estadoCocina)
          and (:estadoReparto is null or p.estadoReparto = :estadoReparto)
        order by p.fechaCreacion desc
    """)
    Page<Pedido> buscarAnterioresClienteFiltrados(@Param("username") String username,
                                                  @Param("desde") LocalDateTime desde,
                                                  @Param("hasta") LocalDateTime hasta,
                                                  @Param("estadoPedido") EstadoPedido estadoPedido,
                                                  @Param("estadoCocina") EstadoCocina estadoCocina,
                                                  @Param("estadoReparto") EstadoReparto estadoReparto,
                                                  Pageable pageable);

    @EntityGraph(attributePaths = {
            "lineaPedidos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.productos",
            "reservaMesa",
            "cliente"
    })
    @Query("""
        select p from Pedido p
        where p.codigo = :codigo
          and p.cliente.username = :username
    """)
    Optional<Pedido> findWithDetalleByCodigoAndCliente_Username(@Param("codigo") String codigo,
                                                                @Param("username") String username);
}