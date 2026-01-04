package com.serveat.repository.notificaciones;

import com.serveat.domain.notificaciones.PushNotificacion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PushNotificacionRepositoryIT {

    @Autowired
    private PushNotificacionRepository repo;

    @PersistenceContext
    private EntityManager em;

    private PushNotificacion crear(String titulo, String mensaje) {
        return new PushNotificacion(titulo, mensaje);
    }

    @Test
    void findAllByOrderByCreadaEnDesc_devuelveVacio_siNoHayNotificaciones() {
        List<PushNotificacion> res = repo.findAllByOrderByCreadaEnDesc();
        assertThat(res).isEmpty();
    }

    @Test
    void findAllByOrderByCreadaEnDesc_ordenaDescPorCreadaEn() {
        // Creamos 3 notificaciones, y forzamos timestamps distintos (sin depender del "now()")
        PushNotificacion n1 = crear("N1", "Mensaje 1");
        PushNotificacion n2 = crear("N2", "Mensaje 2");
        PushNotificacion n3 = crear("N3", "Mensaje 3");

        // PushNotificacion no tiene setter pública para creadaEn,
        // así que lo ajustamos con JPQL update tras persistir.
        repo.save(n1);
        repo.save(n2);
        repo.save(n3);
        repo.flush();

        LocalDateTime t1 = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 1, 2, 10, 0);
        LocalDateTime t3 = LocalDateTime.of(2026, 1, 3, 10, 0);

        em.createQuery("update PushNotificacion p set p.creadaEn = :t where p.id = :id")
                .setParameter("t", t1)
                .setParameter("id", n1.getId())
                .executeUpdate();

        em.createQuery("update PushNotificacion p set p.creadaEn = :t where p.id = :id")
                .setParameter("t", t2)
                .setParameter("id", n2.getId())
                .executeUpdate();

        em.createQuery("update PushNotificacion p set p.creadaEn = :t where p.id = :id")
                .setParameter("t", t3)
                .setParameter("id", n3.getId())
                .executeUpdate();

        em.flush();
        em.clear();

        List<PushNotificacion> res = repo.findAllByOrderByCreadaEnDesc();

        assertThat(res).hasSize(3);

        // Orden DESC esperado: t3, t2, t1
        assertThat(res.get(0).getTitulo()).isEqualTo("N3");
        assertThat(res.get(1).getTitulo()).isEqualTo("N2");
        assertThat(res.get(2).getTitulo()).isEqualTo("N1");

        assertThat(res.get(0).getCreadaEn()).isEqualTo(t3);
        assertThat(res.get(1).getCreadaEn()).isEqualTo(t2);
        assertThat(res.get(2).getCreadaEn()).isEqualTo(t1);
    }

    @Test
    void save_yFindAll_funcionanCorrecto() {
        PushNotificacion n = repo.save(crear("Titulo", "Mensaje"));
        repo.flush();
        em.clear();

        List<PushNotificacion> all = repo.findAll();

        assertThat(all).hasSize(1);
        assertThat(all.get(0).getId()).isEqualTo(n.getId());
        assertThat(all.get(0).getTitulo()).isEqualTo("Titulo");
        assertThat(all.get(0).getMensaje()).isEqualTo("Mensaje");
        assertThat(all.get(0).getCreadaEn()).isNotNull();
        assertThat(all.get(0).isLeida()).isFalse();
    }
}