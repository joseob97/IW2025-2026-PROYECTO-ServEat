package com.serveat.repository.pago;

import com.serveat.domain.pago.ajuste.AjustePago;
import com.serveat.domain.pago.ajuste.EstadoAjustePago;
import com.serveat.domain.pago.ajuste.TipoAjustePago;
import com.serveat.domain.pedido.EstadoPedido;
import com.serveat.domain.pedido.Pedido;
import com.serveat.domain.pedido.TipoPedidoCliente;
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
class AjustePagoRepositoryIT {

    @Autowired
    private AjustePagoRepository repo;

    @PersistenceContext
    private EntityManager em;

    private Pedido crearPedido(String codigo) {
        Pedido p = new Pedido();
        p.setCodigo(codigo);
        p.setEstado(EstadoPedido.EN_CURSO);
        p.setTipoPedido(TipoPedidoCliente.DOMICILIO);
        // el resto tiene defaults (fechaCreacion, estadoCocina, estadoReparto)
        return p;
    }

    private AjustePago crearAjuste(Pedido pedido, String codigo, TipoAjustePago tipo, BigDecimal importe) {
        return new AjustePago(pedido, codigo, tipo, importe);
    }

    private void forzarFechaCreacion(AjustePago a, LocalDateTime fecha) {
        // AjustePago tiene setter de fechaCreacion en tu entidad -> lo usamos
        a.setFechaCreacion(fecha);
    }

    @Test
    void findByCodigo_cuandoExiste_devuelveAjusteConPedidoCargado() {
        Pedido p = crearPedido("P-001");
        em.persist(p);

        AjustePago a = crearAjuste(p, "AJ-001", TipoAjustePago.COBRO, new BigDecimal("12.50"));
        repo.save(a);
        repo.flush();
        em.clear();

        Optional<AjustePago> res = repo.findByCodigo("AJ-001");

        assertThat(res).isPresent();
        assertThat(res.get().getCodigo()).isEqualTo("AJ-001");
        assertThat(res.get().getPedido()).isNotNull();
        assertThat(res.get().getPedido().getCodigo()).isEqualTo("P-001");

        // comprueba que el pedido viene cargado por EntityGraph (sin reventar por lazy)
        assertThat(Hibernate.isInitialized(res.get().getPedido())).isTrue();
    }

    @Test
    void findByCodigo_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByCodigo("NO-EXISTE")).isEmpty();
    }

    @Test
    void findByPedidoCodigoOrderByFechaCreacionDesc_ordenaDesc_yCargaPedido() {
        Pedido p = crearPedido("P-100");
        em.persist(p);

        AjustePago a1 = crearAjuste(p, "AJ-1001", TipoAjustePago.COBRO, new BigDecimal("10.00"));
        AjustePago a2 = crearAjuste(p, "AJ-1002", TipoAjustePago.DEVOLUCION, new BigDecimal("2.00"));
        AjustePago a3 = crearAjuste(p, "AJ-1003", TipoAjustePago.COBRO, new BigDecimal("1.00"));

        // Forzamos fechas para ordenar sin depender del now()
        forzarFechaCreacion(a1, LocalDateTime.of(2026, 1, 1, 10, 0));
        forzarFechaCreacion(a2, LocalDateTime.of(2026, 1, 3, 10, 0));
        forzarFechaCreacion(a3, LocalDateTime.of(2026, 1, 2, 10, 0));

        repo.save(a1);
        repo.save(a2);
        repo.save(a3);
        repo.flush();
        em.clear();

        List<AjustePago> res = repo.findByPedido_CodigoOrderByFechaCreacionDesc("P-100");

        assertThat(res).hasSize(3);
        // DESC: 2026-01-03 (a2), 2026-01-02 (a3), 2026-01-01 (a1)
        assertThat(res.get(0).getCodigo()).isEqualTo("AJ-1002");
        assertThat(res.get(1).getCodigo()).isEqualTo("AJ-1003");
        assertThat(res.get(2).getCodigo()).isEqualTo("AJ-1001");

        assertThat(res).allSatisfy(a -> {
            assertThat(a.getPedido()).isNotNull();
            assertThat(a.getPedido().getCodigo()).isEqualTo("P-100");
            assertThat(Hibernate.isInitialized(a.getPedido())).isTrue();
        });
    }

    @Test
    void findFirstByPedidoCodigoAndEstadoOrderByFechaCreacionDesc_devuelveElMasReciente_deEseEstado() {
        Pedido p = crearPedido("P-200");
        em.persist(p);

        AjustePago ok1 = crearAjuste(p, "AJ-2001", TipoAjustePago.COBRO, new BigDecimal("5.00"));
        ok1.setEstado(EstadoAjustePago.COMPLETADO);
        forzarFechaCreacion(ok1, LocalDateTime.of(2026, 1, 1, 9, 0));

        AjustePago ok2 = crearAjuste(p, "AJ-2002", TipoAjustePago.COBRO, new BigDecimal("6.00"));
        ok2.setEstado(EstadoAjustePago.COMPLETADO);
        forzarFechaCreacion(ok2, LocalDateTime.of(2026, 1, 2, 9, 0));

        AjustePago pend = crearAjuste(p, "AJ-2003", TipoAjustePago.DEVOLUCION, new BigDecimal("1.00"));
        pend.setEstado(EstadoAjustePago.PENDIENTE);
        forzarFechaCreacion(pend, LocalDateTime.of(2026, 1, 3, 9, 0));

        repo.save(ok1);
        repo.save(ok2);
        repo.save(pend);
        repo.flush();
        em.clear();

        Optional<AjustePago> res = repo.findFirstByPedido_CodigoAndEstadoOrderByFechaCreacionDesc(
                "P-200",
                EstadoAjustePago.COMPLETADO
        );

        assertThat(res).isPresent();
        assertThat(res.get().getCodigo()).isEqualTo("AJ-2002"); // el COMPLETADO más reciente
        assertThat(res.get().getEstado()).isEqualTo(EstadoAjustePago.COMPLETADO);
        assertThat(res.get().getPedido().getCodigo()).isEqualTo("P-200");
        assertThat(Hibernate.isInitialized(res.get().getPedido())).isTrue();
    }

    @Test
    void existsByPedidoCodigoAndEstado_true_siExiste_yFalse_siNoExiste() {
        Pedido p1 = crearPedido("P-300");
        Pedido p2 = crearPedido("P-301");
        em.persist(p1);
        em.persist(p2);

        AjustePago a = crearAjuste(p1, "AJ-3001", TipoAjustePago.COBRO, new BigDecimal("9.99"));
        a.setEstado(EstadoAjustePago.PENDIENTE);
        forzarFechaCreacion(a, LocalDateTime.of(2026, 1, 1, 10, 0));

        repo.save(a);
        repo.flush();
        em.clear();

        assertThat(repo.existsByPedido_CodigoAndEstado("P-300", EstadoAjustePago.PENDIENTE)).isTrue();
        assertThat(repo.existsByPedido_CodigoAndEstado("P-300", EstadoAjustePago.COMPLETADO)).isFalse();
        assertThat(repo.existsByPedido_CodigoAndEstado("P-301", EstadoAjustePago.PENDIENTE)).isFalse();
        assertThat(repo.existsByPedido_CodigoAndEstado("NOPE", EstadoAjustePago.PENDIENTE)).isFalse();
    }
}