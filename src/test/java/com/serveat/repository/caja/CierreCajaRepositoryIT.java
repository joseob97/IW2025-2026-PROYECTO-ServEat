package com.serveat.repository.caja;

import com.serveat.domain.caja.CierreCaja;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CierreCajaRepositoryIT {

    @Autowired
    private CierreCajaRepository repo;

    @Test
    void existsByFecha_cuandoExiste_devuelveTrue() {
        LocalDate fecha = LocalDate.of(2026, 1, 1);

        repo.save(new CierreCaja(
                fecha,
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                new BigDecimal("50.00"),
                new BigDecimal("10.00")
        ));

        assertThat(repo.existsByFecha(fecha)).isTrue();
    }

    @Test
    void existsByFecha_cuandoNoExiste_devuelveFalse() {
        assertThat(repo.existsByFecha(LocalDate.of(2026, 1, 2))).isFalse();
    }

    @Test
    void findByFecha_cuandoExiste_devuelveEntidad() {
        LocalDate fecha = LocalDate.of(2026, 1, 3);

        repo.save(new CierreCaja(
                fecha,
                new BigDecimal("200.00"),
                new BigDecimal("80.00"),
                new BigDecimal("90.00"),
                new BigDecimal("30.00")
        ));

        Optional<CierreCaja> res = repo.findByFecha(fecha);

        assertThat(res).isPresent();
        assertThat(res.get().getFecha()).isEqualTo(fecha);
        assertThat(res.get().getTotalGeneral()).isEqualByComparingTo("200.00");
    }

    @Test
    void findByFecha_cuandoNoExiste_devuelveEmpty() {
        assertThat(repo.findByFecha(LocalDate.of(2026, 1, 4))).isEmpty();
    }

    @Test
    void findTop7ByOrderByFechaDesc_devuelveMax7_yOrdenadoDesc() {
        for (int i = 1; i <= 10; i++) {
            LocalDate fecha = LocalDate.of(2026, 1, i);
            repo.save(new CierreCaja(
                    fecha,
                    new BigDecimal("10.00"),
                    new BigDecimal("5.00"),
                    new BigDecimal("3.00"),
                    new BigDecimal("2.00")
            ));
        }

        List<CierreCaja> top = repo.findTop7ByOrderByFechaDesc();

        assertThat(top).hasSize(7);
        assertThat(top.get(0).getFecha()).isEqualTo(LocalDate.of(2026, 1, 10));
        assertThat(top.get(6).getFecha()).isEqualTo(LocalDate.of(2026, 1, 4));
    }
}