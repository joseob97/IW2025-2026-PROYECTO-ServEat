package com.serveat.repository.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.domain.usuario.Cliente;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoSeguimientoRepositoryIT {

    @Autowired
    private PedidoSeguimientoRepository repo;

    @Autowired
    private EntityManager em;

    @Test
    void buscarActivosClienteFiltrados_sinFiltros_devuelveSoloNoAnuladosYNoEntregados() {
        Cliente cliente = persistCliente("cliente1", "cliente1@correo.com");

        Pedido activo = persistPedido(cliente, "P-001", EstadoPedido.EN_CURSO, EstadoCocina.EN_PREPARACION, EstadoReparto.ASIGNADO,
                LocalDateTime.of(2026, 1, 3, 10, 0), null);

        Pedido entregado = persistPedido(cliente, "P-002", EstadoPedido.EN_CURSO, EstadoCocina.LISTO, EstadoReparto.ENTREGADO,
                LocalDateTime.of(2026, 1, 3, 9, 0), LocalDateTime.of(2026, 1, 3, 9, 30));

        Pedido anulado = persistPedido(cliente, "P-003", EstadoPedido.ANULADO, EstadoCocina.CANCELADO, EstadoReparto.NO_APLICA,
                LocalDateTime.of(2026, 1, 3, 8, 0), null);

        Page<Pedido> page = repo.buscarActivosClienteFiltrados(
                "cliente1",
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCodigo()).isEqualTo(activo.getCodigo());
        assertThat(page.getContent().get(0).getFechaEntrega()).isNull();
        assertThat(page.getContent().get(0).getEstado()).isNotEqualTo(EstadoPedido.ANULADO);
    }

    @Test
    void buscarActivosClienteFiltrados_conFiltros_devuelveCoincidencias() {
        Cliente cliente = persistCliente("cliente2", "cliente2@correo.com");

        persistPedido(cliente, "P-010", EstadoPedido.EN_CURSO, EstadoCocina.ACEPTADO, EstadoReparto.ASIGNADO,
                LocalDateTime.of(2026, 1, 3, 10, 0), null);

        Pedido objetivo = persistPedido(cliente, "P-011", EstadoPedido.EN_COCINA, EstadoCocina.EN_PREPARACION, EstadoReparto.EN_REPARTO,
                LocalDateTime.of(2026, 1, 3, 11, 0), null);

        LocalDateTime desde = LocalDateTime.of(2026, 1, 3, 10, 30);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 3, 12, 0);

        Page<Pedido> page = repo.buscarActivosClienteFiltrados(
                "cliente2",
                desde,
                hasta,
                EstadoPedido.EN_COCINA,
                EstadoCocina.EN_PREPARACION,
                EstadoReparto.EN_REPARTO,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCodigo()).isEqualTo(objetivo.getCodigo());
    }

    @Test
    void buscarAnterioresClienteFiltrados_sinFiltros_devuelveAnuladosYOEntregados() {
        Cliente cliente = persistCliente("cliente3", "cliente3@correo.com");

        Pedido activo = persistPedido(cliente, "P-020", EstadoPedido.EN_CURSO, EstadoCocina.EN_PREPARACION, EstadoReparto.ASIGNADO,
                LocalDateTime.of(2026, 1, 3, 10, 0), null);

        Pedido entregado = persistPedido(cliente, "P-021", EstadoPedido.EN_CURSO, EstadoCocina.LISTO, EstadoReparto.ENTREGADO,
                LocalDateTime.of(2026, 1, 3, 9, 0), LocalDateTime.of(2026, 1, 3, 9, 30));

        Pedido anulado = persistPedido(cliente, "P-022", EstadoPedido.ANULADO, EstadoCocina.CANCELADO, EstadoReparto.NO_APLICA,
                LocalDateTime.of(2026, 1, 3, 8, 0), null);

        Page<Pedido> page = repo.buscarAnterioresClienteFiltrados(
                "cliente3",
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Pedido::getCodigo)
                .contains(entregado.getCodigo(), anulado.getCodigo())
                .doesNotContain(activo.getCodigo());
    }

    @Test
    void findWithDetalleByCodigoAndCliente_Username_cuandoExiste_devuelvePedido() {
        Cliente cliente = persistCliente("cliente4", "cliente4@correo.com");
        ReservaMesa mesa = new ReservaMesa(12);
        em.persist(mesa);

        Pedido pedido = new Pedido();
        pedido.setCodigo("P-030");
        pedido.setEstado(EstadoPedido.EN_CURSO);
        pedido.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        pedido.setTipoPedido(TipoPedidoCliente.MESA);
        pedido.setEstadoReparto(EstadoReparto.NO_APLICA);
        pedido.setCliente(cliente);
        pedido.setReservaMesa(mesa);
        pedido.setFechaCreacion(LocalDateTime.of(2026, 1, 3, 12, 0));
        em.persist(pedido);
        em.flush();

        Optional<Pedido> res = repo.findWithDetalleByCodigoAndCliente_Username("P-030", "cliente4");

        assertThat(res).isPresent();
        assertThat(res.get().getCodigo()).isEqualTo("P-030");
        assertThat(res.get().getCliente().getUsername()).isEqualTo("cliente4");
        assertThat(res.get().getReservaMesa()).isNotNull();
    }

    @Test
    void findWithDetalleByCodigoAndCliente_Username_cuandoNoEsDelCliente_devuelveEmpty() {
        Cliente clienteA = persistCliente("clienteA", "clienteA@correo.com");
        Cliente clienteB = persistCliente("clienteB", "clienteB@correo.com");

        persistPedido(clienteA, "P-040", EstadoPedido.EN_CURSO, EstadoCocina.ACEPTADO, EstadoReparto.NO_APLICA,
                LocalDateTime.of(2026, 1, 3, 13, 0), null);

        Optional<Pedido> res = repo.findWithDetalleByCodigoAndCliente_Username("P-040", "clienteB");

        assertThat(res).isEmpty();
        assertThat(clienteB.getUsername()).isEqualTo("clienteB");
    }

    private Cliente persistCliente(String username, String email) {
        Cliente c = new Cliente();
        c.setNombre("Nombre " + username);
        c.setUsername(username);
        c.setEmail(email);
        c.setPassword("password");
        c.setTelefono("600000000");
        c.setDireccion("Calle Falsa 123");
        c.setActivo(true);
        em.persist(c);
        em.flush();
        return c;
    }

    private Pedido persistPedido(Cliente cliente,
                                 String codigo,
                                 EstadoPedido estadoPedido,
                                 EstadoCocina estadoCocina,
                                 EstadoReparto estadoReparto,
                                 LocalDateTime fechaCreacion,
                                 LocalDateTime fechaEntrega) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setEstado(estadoPedido);
        p.setEstadoCocina(estadoCocina);
        p.setTipoPedido(TipoPedidoCliente.DOMICILIO);
        p.setEstadoReparto(estadoReparto);
        p.setCliente(cliente);
        p.setFechaCreacion(fechaCreacion);
        p.setFechaEntrega(fechaEntrega);
        em.persist(p);
        em.flush();
        return p;
    }
}