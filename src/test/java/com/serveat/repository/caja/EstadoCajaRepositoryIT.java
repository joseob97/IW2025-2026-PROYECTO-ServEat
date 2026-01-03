package com.serveat.repository.caja;

import com.serveat.domain.caja.EstadoCaja;
import com.serveat.domain.caja.TipoEstadoCaja;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EstadoCajaRepositoryIT {

    @Autowired
    private EstadoCajaRepository repo;

    @Test
    void findTopByOrderByFechaHoraDesc_cuandoNoHayEstados_devuelveEmpty() {
        Optional<EstadoCaja> res = repo.findTopByOrderByFechaHoraDesc();
        assertThat(res).isEmpty();
    }

    @Test
    void findTopByOrderByFechaHoraDesc_conUnEstado_devuelveEse() {
        EstadoCaja e1 = repo.save(new EstadoCaja(TipoEstadoCaja.ABIERTA, "user1"));

        Optional<EstadoCaja> res = repo.findTopByOrderByFechaHoraDesc();

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(e1.getId());
        assertThat(res.get().getTipo()).isEqualTo(TipoEstadoCaja.ABIERTA);
        assertThat(res.get().getUsuario()).isEqualTo("user1");
    }

    @Test
    void findTopByOrderByFechaHoraDesc_conVariosEstados_devuelveElMasReciente() throws InterruptedException {
        EstadoCaja older = repo.save(new EstadoCaja(TipoEstadoCaja.ABIERTA, "user1"));

        // aseguramos diferencia de fechaHora (LocalDateTime.now())
        Thread.sleep(5);

        EstadoCaja newer = repo.save(new EstadoCaja(TipoEstadoCaja.CERRADA, "user2"));

        Optional<EstadoCaja> res = repo.findTopByOrderByFechaHoraDesc();

        assertThat(res).isPresent();
        assertThat(res.get().getId()).isEqualTo(newer.getId());
        assertThat(res.get().getFechaHora()).isAfter(older.getFechaHora());
        assertThat(res.get().getTipo()).isEqualTo(TipoEstadoCaja.CERRADA);
        assertThat(res.get().getUsuario()).isEqualTo("user2");
    }
}