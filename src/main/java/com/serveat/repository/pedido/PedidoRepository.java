package com.serveat.repository.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            "cliente",
            "repartidor"
    })
    List<Pedido> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "cliente",
            "repartidor"
    })
    List<Pedido> findByReservaMesa_NumeroMesaOrderByFechaCreacionDesc(Integer numeroMesa);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "cliente",
            "repartidor"
    })
    List<Pedido> findByEstadoCocinaAndReservaMesa_NumeroMesaOrderByFechaCreacionDesc(EstadoCocina estado, Integer numeroMesa);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "cliente",
            "repartidor"
    })
    Optional<Pedido> findWithDetalleByCodigo(String codigo);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "cliente",
            "repartidor"
    })
    Optional<Pedido> findWithDetalleById(UUID id);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "cliente",
            "repartidor"
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
            "cliente",
            "repartidor"
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
            "cliente",
            "repartidor"
    })
    List<Pedido> findByCliente_UsernameOrderByFechaCreacionDesc(String username);

    @EntityGraph(attributePaths = {
            "reservaMesa",
            "lineaPedidos",
            "lineaPedidos.productos",
            "cliente",
            "repartidor"
    })
    Optional<Pedido> findWithDetalleByCodigoAndCliente_Username(String codigo, String username);

    @EntityGraph(attributePaths = {
            "cliente",
            "lineaPedidos",
            "lineaPedidos.productos",
            "repartidor"
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
            "repartidor"
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
            "repartidor"
    })
    List<Pedido> findByRepartidor_Username(String username);

    @EntityGraph(attributePaths = {
            "cliente",
            "lineaPedidos",
            "lineaPedidos.productos"
    })
    List<Pedido> findByEstadoAndEstadoCocina(
            EstadoPedido estado,
            EstadoCocina estadoCocina
    );

    @EntityGraph(attributePaths = {
            "cliente",
            "lineaPedidos",
            "lineaPedidos.productos",
            "reservaMesa"
    })
    List<Pedido> findByEstadoCocina(EstadoCocina estadoCocina);
}
