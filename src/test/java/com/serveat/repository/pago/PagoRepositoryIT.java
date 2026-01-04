package com.serveat.repository.pago;

import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
import com.serveat.domain.usuario.Cliente;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PagoRepositoryIT {

    @Autowired
    private PagoRepository repo;

    @PersistenceContext
    private EntityManager em;

    private Cliente crearCliente(String username, String email) {
        Cliente c = new Cliente();
        c.setNombre("Cliente " + username);
        c.setUsername(username);
        c.setEmail(email);
        c.setPassword("password");
        c.setTelefono("600123456");
        c.setDireccion("Calle Principal 1");
        c.setActivo(true);
        return c;
    }

    private Pedido crearPedido(String codigo, Cliente cliente) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setTipoPedido(TipoPedidoCliente.DOMICILIO);
        p.setCliente(cliente); // importante para el EntityGraph pedido.cliente
        return p;
    }

    private Pago crearPago(Pedido pedido, MetodoPago metodo, BigDecimal importe) {
        return new Pago(pedido, metodo, importe); // estado=PENDIENTE, fechaCreacion=now()
    }

    @Test
    void findByPedidoCodigo_cuandoExiste_devuelvePago() {
        Cliente c = crearCliente("cli1", "cli1@test.com");
        em.persist(c);

        Pedido p = crearPedido("P-001", c);
        em.persist(p);

        Pago pago = crearPago(p, MetodoPago.TARJETA, new BigDecimal("12.50"));
        repo.save(pago);
        repo.flush();
        em.clear();

        Optional<Pago> res = repo.findByPedido_Codigo("P-001");

        assertThat(res).isPresent();
        assertThat(res.get().getPedido()).isNotNull();
        assertThat(res.get().getPedido().getCodigo()).isEqualTo("P-001");
        assertThat(res.get().getMetodo()).isEqualTo(MetodoPago.TARJETA);
        assertThat(res.get().getEstado()).isEqualTo(EstadoPago.PENDIENTE);
    }

    @Test
    void findByPedidoCodigo_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByPedido_Codigo("NO-EXISTE")).isEmpty();
    }

    @Test
    void findWithPedidoById_cargaPedidoYCliente_porEntityGraph() {
        Cliente c = crearCliente("cli2", "cli2@test.com");
        em.persist(c);

        Pedido p = crearPedido("P-010", c);
        em.persist(p);

        Pago pago = crearPago(p, MetodoPago.PAYPAL, new BigDecimal("20.00"));
        pago.confirmar("REF-OK-1"); // estado CONFIRMADO + fechaConfirmacion now
        repo.save(pago);
        repo.flush();
        Long pagoId = pago.getId();
        em.clear();

        Optional<Pago> res = repo.findWithPedidoById(pagoId);

        assertThat(res).isPresent();
        Pago loaded = res.get();

        assertThat(loaded.getId()).isEqualTo(pagoId);

        // EntityGraph: pedido y pedido.cliente deben venir inicializados
        assertThat(loaded.getPedido()).isNotNull();
        assertThat(Hibernate.isInitialized(loaded.getPedido())).isTrue();

        assertThat(loaded.getPedido().getCliente()).isNotNull();
        assertThat(Hibernate.isInitialized(loaded.getPedido().getCliente())).isTrue();

        assertThat(loaded.getPedido().getCodigo()).isEqualTo("P-010");
        assertThat(loaded.getPedido().getCliente().getUsername()).isEqualTo("cli2");
    }

    @Test
    void countByEstado_cuentaCorrecto() {
        Cliente c = crearCliente("cli3", "cli3@test.com");
        em.persist(c);

        Pedido p1 = crearPedido("P-101", c);
        Pedido p2 = crearPedido("P-102", c);
        Pedido p3 = crearPedido("P-103", c);
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);

        Pago a = crearPago(p1, MetodoPago.TARJETA, new BigDecimal("10.00"));
        a.confirmar("REF-A"); // CONFIRMADO

        Pago b = crearPago(p2, MetodoPago.EFECTIVO, new BigDecimal("5.00"));
        // se queda PENDIENTE

        Pago d = crearPago(p3, MetodoPago.PAYPAL, new BigDecimal("7.00"));
        d.fallar("KO"); // FALLIDO

        repo.save(a);
        repo.save(b);
        repo.save(d);
        repo.flush();
        em.clear();

        assertThat(repo.countByEstado(EstadoPago.CONFIRMADO)).isEqualTo(1);
        assertThat(repo.countByEstado(EstadoPago.PENDIENTE)).isEqualTo(1);
        assertThat(repo.countByEstado(EstadoPago.FALLIDO)).isEqualTo(1);
    }

    @Test
    void findByEstado_devuelvePagosDeEseEstado_yCargaPedido_porEntityGraph() {
        Cliente c = crearCliente("cli4", "cli4@test.com");
        em.persist(c);

        Pedido p1 = crearPedido("P-201", c);
        Pedido p2 = crearPedido("P-202", c);
        Pedido p3 = crearPedido("P-203", c);
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);

        Pago ok1 = crearPago(p1, MetodoPago.TARJETA, new BigDecimal("11.00"));
        ok1.confirmar("REF-201");

        Pago ok2 = crearPago(p2, MetodoPago.PAYPAL, new BigDecimal("22.00"));
        ok2.confirmar("REF-202");

        Pago pend = crearPago(p3, MetodoPago.EFECTIVO, new BigDecimal("33.00"));
        // PENDIENTE

        repo.save(ok1);
        repo.save(ok2);
        repo.save(pend);
        repo.flush();
        em.clear();

        List<Pago> confirmados = repo.findByEstado(EstadoPago.CONFIRMADO);

        assertThat(confirmados).hasSize(2);
        assertThat(confirmados).allSatisfy(p -> {
            assertThat(p.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
            assertThat(p.getPedido()).isNotNull();
            assertThat(Hibernate.isInitialized(p.getPedido())).isTrue(); // EntityGraph pedido
        });

        assertThat(confirmados)
                .extracting(p -> p.getPedido().getCodigo())
                .containsExactlyInAnyOrder("P-201", "P-202");
    }

    @Test
    void findByEstadoAndFechaConfirmacionBetween_filtraPorRangoIncluyente() {
        Cliente c = crearCliente("cli5", "cli5@test.com");
        em.persist(c);

        Pedido p1 = crearPedido("P-301", c);
        Pedido p2 = crearPedido("P-302", c);
        Pedido p3 = crearPedido("P-303", c);
        em.persist(p1);
        em.persist(p2);
        em.persist(p3);

        // Creamos pagos y fijamos manualmente fechaConfirmacion con EntityManager (no hay setter)
        Pago dentro1 = crearPago(p1, MetodoPago.TARJETA, new BigDecimal("10.00"));
        dentro1.confirmar("REF-301");
        repo.save(dentro1);

        Pago dentro2 = crearPago(p2, MetodoPago.PAYPAL, new BigDecimal("20.00"));
        dentro2.confirmar("REF-302");
        repo.save(dentro2);

        Pago fuera = crearPago(p3, MetodoPago.EFECTIVO, new BigDecimal("30.00"));
        fuera.confirmar("REF-303");
        repo.save(fuera);

        repo.flush();

        // Forzamos fechas exactas para el test (evitamos depender del "now()")
        LocalDateTime t1 = LocalDateTime.of(2026, 1, 3, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 1, 3, 12, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 1, 4, 10, 0);

        em.createQuery("update Pago p set p.fechaConfirmacion = :fc where p.id = :id")
                .setParameter("fc", t1)
                .setParameter("id", dentro1.getId())
                .executeUpdate();

        em.createQuery("update Pago p set p.fechaConfirmacion = :fc where p.id = :id")
                .setParameter("fc", t2)
                .setParameter("id", dentro2.getId())
                .executeUpdate();

        em.createQuery("update Pago p set p.fechaConfirmacion = :fc where p.id = :id")
                .setParameter("fc", t3)
                .setParameter("id", fuera.getId())
                .executeUpdate();

        em.flush();
        em.clear();

        LocalDateTime desde = LocalDateTime.of(2026, 1, 3, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2026, 1, 3, 23, 59);

        List<Pago> res = repo.findByEstadoAndFechaConfirmacionBetween(
                EstadoPago.CONFIRMADO,
                desde,
                hasta
        );

        assertThat(res).hasSize(2);
        assertThat(res).extracting(p -> p.getPedido().getCodigo())
                .containsExactlyInAnyOrder("P-301", "P-302");

        assertThat(res).allSatisfy(p -> {
            assertThat(p.getEstado()).isEqualTo(EstadoPago.CONFIRMADO);
            assertThat(p.getFechaConfirmacion()).isNotNull();
            assertThat(!p.getFechaConfirmacion().isBefore(desde)).isTrue();
            assertThat(!p.getFechaConfirmacion().isAfter(hasta)).isTrue();
        });
    }
}