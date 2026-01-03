package com.serveat.repository.pedido;

import com.serveat.domain.pedido.EstadoCocina;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.EstadoReparto;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.domain.reserva.ReservaMesa;
import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PedidoRepositoryIT {

    @Autowired
    private PedidoRepository repo;

    @PersistenceContext
    private EntityManager em;

    // Crea un cliente válido cumpliendo restricciones
    private Cliente crearCliente(String username, String email, boolean activo) {
        Cliente c = new Cliente();
        c.setNombre("Cliente " + username);
        c.setUsername(username);
        c.setEmail(email);
        c.setPassword("password");
        c.setTelefono("600123456");
        c.setDireccion("Calle Principal 1");
        c.setActivo(activo);
        return c;
    }

    // Crea un empleado válido cumpliendo restricciones
    private Empleado crearEmpleado(String username, String email, String rol, boolean enabled) {
        return new Empleado(
                "Empleado " + username,
                username,
                "password",
                "600123457",
                email,
                "Calle Secundaria 2",
                rol,
                enabled
        );
    }

    // Crea una reserva de mesa válida
    private ReservaMesa crearReservaMesa(Integer numeroMesa) {
        return new ReservaMesa(numeroMesa);
    }

    // Crea un pedido con los campos obligatorios
    private Pedido crearPedido(String codigo,
                               EstadoPedido estado,
                               TipoPedidoCliente tipoPedido,
                               EstadoCocina estadoCocina,
                               EstadoReparto estadoReparto,
                               LocalDateTime fechaCreacion) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setEstado(estado);
        p.setTipoPedido(tipoPedido);
        p.setEstadoCocina(estadoCocina);
        p.setEstadoReparto(estadoReparto);
        p.setFechaCreacion(fechaCreacion);
        return p;
    }

    // Verifica findByCodigo cuando existe
    @Test
    void findByCodigo_cuandoExiste_devuelvePedido() {
        Pedido p = repo.save(crearPedido(
                "P-001",
                EstadoPedido.EN_CURSO,
                TipoPedidoCliente.MESA,
                EstadoCocina.PENDIENTE_ACEPTACION,
                EstadoReparto.NO_APLICA,
                LocalDateTime.of(2026, 1, 3, 10, 0)
        ));

        Optional<Pedido> res = repo.findByCodigo("P-001");

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(p.getId());
        assertThat(res.get().getEstado()).isEqualTo(EstadoPedido.EN_CURSO);
    }

    // Verifica findByCodigo cuando no existe
    @Test
    void findByCodigo_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByCodigo("NO-EXISTE")).isEmpty();
    }

    // Verifica findByEstado filtra correctamente
    @Test
    void findByEstado_devuelveSoloEseEstado() {
        repo.save(crearPedido("P-101", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 9, 0)));
        repo.save(crearPedido("P-102", EstadoPedido.EN_COCINA, TipoPedidoCliente.MESA, EstadoCocina.ACEPTADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 9, 5)));
        repo.save(crearPedido("P-103", EstadoPedido.EN_CURSO, TipoPedidoCliente.RECOGER, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 9, 10)));

        List<Pedido> enCurso = repo.findByEstado(EstadoPedido.EN_CURSO);

        assertThat(enCurso).hasSize(2);
        assertThat(enCurso).allMatch(p -> p.getEstado() == EstadoPedido.EN_CURSO);
        assertThat(enCurso).extracting(Pedido::getCodigo).containsExactlyInAnyOrder("P-101", "P-103");
    }

    // Verifica findByEstadoIn filtra correctamente
    @Test
    void findByEstadoIn_devuelveSoloEstadosIncluidos() {
        repo.save(crearPedido("P-201", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 8, 0)));
        repo.save(crearPedido("P-202", EstadoPedido.EN_COCINA, TipoPedidoCliente.MESA, EstadoCocina.EN_PREPARACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 8, 5)));
        repo.save(crearPedido("P-203", EstadoPedido.ANULADO, TipoPedidoCliente.MESA, EstadoCocina.CANCELADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 8, 10)));

        List<Pedido> res = repo.findByEstadoIn(List.of(EstadoPedido.EN_CURSO, EstadoPedido.EN_COCINA));

        assertThat(res).hasSize(2);
        assertThat(res).extracting(Pedido::getCodigo).containsExactlyInAnyOrder("P-201", "P-202");
    }

    // Verifica findAllByOrderByFechaCreacionDesc ordena descendente
    @Test
    void findAllByOrderByFechaCreacionDesc_ordenaDesc() {
        repo.save(crearPedido("P-301", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 10, 0)));
        repo.save(crearPedido("P-302", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 12, 0)));
        repo.save(crearPedido("P-303", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 11, 0)));

        List<Pedido> res = repo.findAllByOrderByFechaCreacionDesc();

        assertThat(res).hasSize(3);
        assertThat(res.get(0).getCodigo()).isEqualTo("P-302");
        assertThat(res.get(1).getCodigo()).isEqualTo("P-303");
        assertThat(res.get(2).getCodigo()).isEqualTo("P-301");
    }

    // Verifica findByReservaMesa_NumeroMesaOrderByFechaCreacionDesc filtra por mesa y ordena
    @Test
    void findByReservaMesaNumeroMesaOrderByFechaCreacionDesc_filtraYOrdena() {
        ReservaMesa mesa10 = crearReservaMesa(10);
        ReservaMesa mesa11 = crearReservaMesa(11);
        em.persist(mesa10);
        em.persist(mesa11);

        Pedido p1 = crearPedido("P-401", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 10, 0));
        p1.setReservaMesa(mesa10);
        repo.save(p1);

        Pedido p2 = crearPedido("P-402", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 12, 0));
        p2.setReservaMesa(mesa10);
        repo.save(p2);

        Pedido p3 = crearPedido("P-403", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 11, 0));
        p3.setReservaMesa(mesa11);
        repo.save(p3);

        List<Pedido> res = repo.findByReservaMesa_NumeroMesaOrderByFechaCreacionDesc(10);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getCodigo()).isEqualTo("P-402");
        assertThat(res.get(1).getCodigo()).isEqualTo("P-401");
    }

    // Verifica findByEstadoCocinaAndReservaMesa_NumeroMesaOrderByFechaCreacionDesc
    @Test
    void findByEstadoCocinaAndReservaMesaNumeroMesaOrderByFechaCreacionDesc_filtraCorrecto() {
        ReservaMesa mesa7 = crearReservaMesa(7);
        em.persist(mesa7);

        Pedido p1 = crearPedido("P-501", EstadoPedido.EN_COCINA, TipoPedidoCliente.MESA, EstadoCocina.ACEPTADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 9, 0));
        p1.setReservaMesa(mesa7);
        repo.save(p1);

        Pedido p2 = crearPedido("P-502", EstadoPedido.EN_COCINA, TipoPedidoCliente.MESA, EstadoCocina.EN_PREPARACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 10, 0));
        p2.setReservaMesa(mesa7);
        repo.save(p2);

        Pedido p3 = crearPedido("P-503", EstadoPedido.EN_COCINA, TipoPedidoCliente.MESA, EstadoCocina.ACEPTADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 11, 0));
        p3.setReservaMesa(mesa7);
        repo.save(p3);

        List<Pedido> res = repo.findByEstadoCocinaAndReservaMesa_NumeroMesaOrderByFechaCreacionDesc(EstadoCocina.ACEPTADO, 7);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getCodigo()).isEqualTo("P-503");
        assertThat(res.get(1).getCodigo()).isEqualTo("P-501");
    }

    // Verifica findByCliente_UsernameOrderByFechaCreacionDesc
    @Test
    void findByClienteUsernameOrderByFechaCreacionDesc_filtraYOrdena() {
        Cliente c1 = crearCliente("cli1", "cli1@test.com", true);
        Cliente c2 = crearCliente("cli2", "cli2@test.com", true);
        em.persist(c1);
        em.persist(c2);

        Pedido p1 = crearPedido("P-601", EstadoPedido.EN_CURSO, TipoPedidoCliente.RECOGER, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 10, 0));
        p1.setCliente(c1);
        repo.save(p1);

        Pedido p2 = crearPedido("P-602", EstadoPedido.EN_CURSO, TipoPedidoCliente.RECOGER, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 12, 0));
        p2.setCliente(c1);
        repo.save(p2);

        Pedido p3 = crearPedido("P-603", EstadoPedido.EN_CURSO, TipoPedidoCliente.RECOGER, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 11, 0));
        p3.setCliente(c2);
        repo.save(p3);

        List<Pedido> res = repo.findByCliente_UsernameOrderByFechaCreacionDesc("cli1");

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getCodigo()).isEqualTo("P-602");
        assertThat(res.get(1).getCodigo()).isEqualTo("P-601");
    }

    // Verifica findWithDetalleByCodigoAndCliente_Username
    @Test
    void findWithDetalleByCodigoAndClienteUsername_cuandoCoincide_devuelvePedido() {
        Cliente c1 = crearCliente("cli3", "cli3@test.com", true);
        em.persist(c1);

        Pedido p = crearPedido("P-701", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 10, 0));
        p.setCliente(c1);
        repo.save(p);

        Optional<Pedido> res = repo.findWithDetalleByCodigoAndCliente_Username("P-701", "cli3");

        assertThat(res).isPresent();
        assertThat(res.get().getCodigo()).isEqualTo("P-701");
        assertThat(res.get().getCliente().getUsername()).isEqualTo("cli3");
    }

    // Verifica findByTipoPedidoAndEstadoReparto
    @Test
    void findByTipoPedidoAndEstadoReparto_filtraCorrecto() {
        repo.save(crearPedido("P-801", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.PENDIENTE_ASIGNACION, LocalDateTime.of(2026, 1, 3, 9, 0)));
        repo.save(crearPedido("P-802", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.ASIGNADO, LocalDateTime.of(2026, 1, 3, 9, 5)));
        repo.save(crearPedido("P-803", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.ACEPTADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 9, 10)));

        List<Pedido> res = repo.findByTipoPedidoAndEstadoReparto(TipoPedidoCliente.DOMICILIO, EstadoReparto.PENDIENTE_ASIGNACION);

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getCodigo()).isEqualTo("P-801");
    }

    // Verifica findByTipoPedidoAndEstadoRepartoAndEstadoCocina
    @Test
    void findByTipoPedidoAndEstadoRepartoAndEstadoCocina_filtraCorrecto() {
        repo.save(crearPedido("P-901", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.PENDIENTE_ASIGNACION, LocalDateTime.of(2026, 1, 3, 10, 0)));
        repo.save(crearPedido("P-902", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.EN_PREPARACION, EstadoReparto.PENDIENTE_ASIGNACION, LocalDateTime.of(2026, 1, 3, 10, 5)));

        List<Pedido> res = repo.findByTipoPedidoAndEstadoRepartoAndEstadoCocina(
                TipoPedidoCliente.DOMICILIO,
                EstadoReparto.PENDIENTE_ASIGNACION,
                EstadoCocina.ACEPTADO
        );

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getCodigo()).isEqualTo("P-901");
    }

    // Verifica findByRepartidor_Username
    @Test
    void findByRepartidorUsername_filtraCorrecto() {
        Empleado r1 = crearEmpleado("rep1", "rep1@test.com", "REPARTIDOR", true);
        Empleado r2 = crearEmpleado("rep2", "rep2@test.com", "REPARTIDOR", true);
        em.persist(r1);
        em.persist(r2);

        Pedido p1 = crearPedido("P-1001", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.ASIGNADO, LocalDateTime.of(2026, 1, 3, 11, 0));
        p1.setRepartidor(r1);
        repo.save(p1);

        Pedido p2 = crearPedido("P-1002", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.ASIGNADO, LocalDateTime.of(2026, 1, 3, 11, 5));
        p2.setRepartidor(r2);
        repo.save(p2);

        List<Pedido> res = repo.findByRepartidor_Username("rep1");

        assertThat(res).hasSize(1);
        assertThat(res.get(0).getCodigo()).isEqualTo("P-1001");
    }

    // Verifica countByEstadoCocina y countByEstado
    @Test
    void countByEstadoYCountByEstadoCocina_cuentaCorrecto() {
        repo.save(crearPedido("P-1101", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 8, 0)));
        repo.save(crearPedido("P-1102", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 8, 5)));
        repo.save(crearPedido("P-1103", EstadoPedido.EN_COCINA, TipoPedidoCliente.MESA, EstadoCocina.ACEPTADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 8, 10)));

        assertThat(repo.countByEstado(EstadoPedido.EN_CURSO)).isEqualTo(2);
        assertThat(repo.countByEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION)).isEqualTo(2);
        assertThat(repo.countByEstadoCocina(EstadoCocina.ACEPTADO)).isEqualTo(1);
        assertThat(repo.count()).isEqualTo(3);
    }

    // Verifica buscarPedidosFiltrados aplica filtros y orden desc
    @Test
    void buscarPedidosFiltrados_filtraYOrdenaDesc() {
        ReservaMesa mesa20 = crearReservaMesa(20);
        em.persist(mesa20);

        Pedido p1 = crearPedido("P-1201", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.PENDIENTE_ACEPTACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 9, 0));
        p1.setReservaMesa(mesa20);
        repo.save(p1);

        Pedido p2 = crearPedido("P-1202", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.ACEPTADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 10, 0));
        p2.setReservaMesa(mesa20);
        repo.save(p2);

        Pedido p3 = crearPedido("P-1203", EstadoPedido.ANULADO, TipoPedidoCliente.MESA, EstadoCocina.CANCELADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 11, 0));
        repo.save(p3);

        Page<Pedido> page = repo.buscarPedidosFiltrados(
                LocalDateTime.of(2026, 1, 3, 8, 0),
                LocalDateTime.of(2026, 1, 3, 12, 0),
                EstadoPedido.EN_CURSO,
                null,
                20,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getCodigo()).isEqualTo("P-1202");
        assertThat(page.getContent().get(1).getCodigo()).isEqualTo("P-1201");
    }

    // Verifica buscarPedidosCocinaHoy orden asc
    @Test
    void buscarPedidosCocinaHoy_ordenAsc() {
        ReservaMesa mesa30 = crearReservaMesa(30);
        em.persist(mesa30);

        Pedido p1 = crearPedido("P-1301", EstadoPedido.EN_COCINA, TipoPedidoCliente.MESA, EstadoCocina.EN_PREPARACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 9, 0));
        p1.setReservaMesa(mesa30);
        repo.save(p1);

        Pedido p2 = crearPedido("P-1302", EstadoPedido.EN_COCINA, TipoPedidoCliente.MESA, EstadoCocina.EN_PREPARACION, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 10, 0));
        p2.setReservaMesa(mesa30);
        repo.save(p2);

        Page<Pedido> page = repo.buscarPedidosCocinaHoy(
                LocalDateTime.of(2026, 1, 3, 0, 0),
                LocalDateTime.of(2026, 1, 3, 23, 59),
                EstadoCocina.EN_PREPARACION,
                30,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getCodigo()).isEqualTo("P-1301");
        assertThat(page.getContent().get(1).getCodigo()).isEqualTo("P-1302");
    }

    // Verifica buscarPedidosDisponiblesRepartidor devuelve solo domicilio pendiente asignacion
    @Test
    void buscarPedidosDisponiblesRepartidor_filtraCorrecto() {
        repo.save(crearPedido("P-1401", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.PENDIENTE_ASIGNACION, LocalDateTime.of(2026, 1, 3, 9, 0)));
        repo.save(crearPedido("P-1402", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.ASIGNADO, LocalDateTime.of(2026, 1, 3, 10, 0)));
        repo.save(crearPedido("P-1403", EstadoPedido.EN_CURSO, TipoPedidoCliente.MESA, EstadoCocina.ACEPTADO, EstadoReparto.NO_APLICA, LocalDateTime.of(2026, 1, 3, 11, 0)));

        Page<Pedido> page = repo.buscarPedidosDisponiblesRepartidor(
                LocalDateTime.of(2026, 1, 3, 8, 0),
                LocalDateTime.of(2026, 1, 3, 12, 0),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCodigo()).isEqualTo("P-1401");
    }

    // Verifica buscarMisRepartosFiltrados filtra por repartidor y estado reparto
    @Test
    void buscarMisRepartosFiltrados_filtraCorrecto() {
        Empleado r1 = crearEmpleado("rep10", "rep10@test.com", "REPARTIDOR", true);
        Empleado r2 = crearEmpleado("rep20", "rep20@test.com", "REPARTIDOR", true);
        em.persist(r1);
        em.persist(r2);

        Pedido p1 = crearPedido("P-1501", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.ASIGNADO, LocalDateTime.of(2026, 1, 3, 9, 0));
        p1.setRepartidor(r1);
        repo.save(p1);

        Pedido p2 = crearPedido("P-1502", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.EN_REPARTO, LocalDateTime.of(2026, 1, 3, 10, 0));
        p2.setRepartidor(r1);
        repo.save(p2);

        Pedido p3 = crearPedido("P-1503", EstadoPedido.EN_CURSO, TipoPedidoCliente.DOMICILIO, EstadoCocina.ACEPTADO, EstadoReparto.ASIGNADO, LocalDateTime.of(2026, 1, 3, 11, 0));
        p3.setRepartidor(r2);
        repo.save(p3);

        Page<Pedido> page = repo.buscarMisRepartosFiltrados(
                "rep10",
                LocalDateTime.of(2026, 1, 3, 8, 0),
                LocalDateTime.of(2026, 1, 3, 12, 0),
                EstadoReparto.ASIGNADO,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCodigo()).isEqualTo("P-1501");
    }
}