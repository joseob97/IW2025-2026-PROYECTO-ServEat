package com.serveat.repository.seguridad;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureActiva;
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
class FeatureActivaRepositoryIT {

    @Autowired
    private FeatureActivaRepository repo;

    @Test
    void findByFeature_cuandoExiste_devuelveEntidad() {
        FeatureActiva fa = new FeatureActiva(Feature.PAGO_ONLINE);
        repo.save(fa);

        Optional<FeatureActiva> res = repo.findByFeature(Feature.PAGO_ONLINE);

        assertThat(res).isPresent();
        assertThat(res.get().getFeature()).isEqualTo(Feature.PAGO_ONLINE);
    }

    @Test
    void findByFeature_cuandoNoExiste_devuelveEmpty() {
        Optional<FeatureActiva> res = repo.findByFeature(Feature.INGREDIENTES);

        assertThat(res).isEmpty();
    }

    @Test
    void existsByFeature_cuandoExiste_devuelveTrue() {
        FeatureActiva fa = new FeatureActiva(Feature.NOTIFICACIONES);
        repo.save(fa);

        boolean existe = repo.existsByFeature(Feature.NOTIFICACIONES);

        assertThat(existe).isTrue();
    }

    @Test
    void existsByFeature_cuandoNoExiste_devuelveFalse() {
        boolean existe = repo.existsByFeature(Feature.EXPORTAR_DATOS);

        assertThat(existe).isFalse();
    }

    @Test
    void guardarFeatureActiva_persisteCorrectamente() {
        FeatureActiva fa = new FeatureActiva(Feature.CIERRE_CAJA);
        fa.setActiva(true);

        FeatureActiva guardada = repo.save(fa);

        assertThat(guardada.getFeature()).isEqualTo(Feature.CIERRE_CAJA);
        assertThat(guardada.isActiva()).isTrue();
    }
}