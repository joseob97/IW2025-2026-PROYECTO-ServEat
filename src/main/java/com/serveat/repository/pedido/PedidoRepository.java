package com.serveat.repository.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    List<Pedido> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    List<Pedido> findByReservaMesa_NumeroMesaOrderByFechaCreacionDesc(Integer numeroMesa);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    List<Pedido> findByEstadoCocinaAndReservaMesa_NumeroMesaOrderByFechaCreacionDesc(EstadoCocina estado, Integer numeroMesa);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    Optional<Pedido> findWithDetalleByCodigo(String codigo);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    Optional<Pedido> findWithDetalleById(UUID id);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    List<Pedido> findByEstadoOrEstadoAndEstadoCocina(
            EstadoPedido estadoEnCurso,
            EstadoPedido estadoEnCocina,
            EstadoCocina estadoCocinaPendiente
    );

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    List<Pedido> findByReservaMesa_NumeroMesaAndEstadoOrReservaMesa_NumeroMesaAndEstadoAndEstadoCocina(
            Integer numeroMesa1,
            EstadoPedido estadoEnCurso,
            Integer numeroMesa2,
            EstadoPedido estadoEnCocina,
            EstadoCocina estadoCocinaPendiente
    );

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    List<Pedido> findByCliente_UsernameOrderByFechaCreacionDesc(String username);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    Optional<Pedido> findWithDetalleByCodigoAndCliente_Username(String codigo, String username);

    @EntityGraph(attributePaths = {
            "cliente",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "repartidor",
            "pago"
    })
    List<Pedido> findByTipoPedidoAndEstadoReparto(
            TipoPedidoCliente tipo,
            EstadoReparto estadoReparto
    );

    // NUEVO: Filtrar por Tipo, EstadoReparto y EstadoCocina (para repartidores)
    @EntityGraph(attributePaths = {
            "cliente",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "repartidor",
            "pago"
    })
    List<Pedido> findByTipoPedidoAndEstadoRepartoAndEstadoCocina(
            TipoPedidoCliente tipo,
            EstadoReparto estadoReparto,
            EstadoCocina estadoCocina
    );

    @EntityGraph(attributePaths = {
            "cliente",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "repartidor",
            "pago"
    })
    List<Pedido> findByRepartidor_Username(String username);

    @EntityGraph(attributePaths = {
            "cliente",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "pago"
    })
    List<Pedido> findByEstadoAndEstadoCocina(
            EstadoPedido estado,
            EstadoCocina estadoCocina
    );

    @EntityGraph(attributePaths = {
            "cliente",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "reservaMesa",
            "pago"
    })
    List<Pedido> findByEstadoCocina(EstadoCocina estadoCocina);

    long countByEstadoCocina(EstadoCocina estadoCocina);

    // Para las estadísticas
    long countByEstado(EstadoPedido estado);

    long count();

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "lineaPedidos.ingredientes",
            "lineaPedidos.ingredientes.ingrediente",
            "cliente",
            "repartidor",
            "pago"
    })
    @Query("""
        select p
        from Pedido p
        where (:desde is null or p.fechaCreacion >= :desde)
          and (:hasta is null or p.fechaCreacion <= :hasta)
          and (:estadoPedido is null or p.estado = :estadoPedido)
          and (:estadoCocina is null or p.estadoCocina = :estadoCocina)
          and (:mesa is null or p.reservaMesa.numeroMesa = :mesa)
        order by p.fechaCreacion desc
    """)
    Page<Pedido> buscarPedidosFiltrados(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hasta,
                                        @Param("estadoPedido") EstadoPedido estadoPedido,
                                        @Param("estadoCocina") EstadoCocina estadoCocina,
                                        @Param("mesa") Integer mesa,
                                        Pageable pageable);
}