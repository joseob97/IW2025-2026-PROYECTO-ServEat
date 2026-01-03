package com.serveat.repository.seguridad;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureDatos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FeatureDatosRepositoryIT {

    @Autowired
    private FeatureDatosRepository repo;

    @Test
    void findByFeature_cuandoExiste_devuelveEntidad() {
        FeatureDatos datos = new FeatureDatos(
                Feature.INGREDIENTES,
                new BigDecimal("9.99"),
                "ABCDE"
        );
        repo.save(datos);

        Optional<FeatureDatos> res = repo.findByFeature(Feature.INGREDIENTES);

        assertThat(res).isPresent();
        assertThat(res.get().getFeature()).isEqualTo(Feature.INGREDIENTES);
        assertThat(res.get().getPrecio()).isEqualByComparingTo("9.99");
    }

    @Test
    void findByFeature_cuandoNoExiste_devuelveEmpty() {
        Optional<FeatureDatos> res = repo.findByFeature(Feature.PROMOCIONES);

        assertThat(res).isEmpty();
    }

    @Test
    void findByCodigoDesbloqueo_cuandoExiste_devuelveEntidad() {
        FeatureDatos datos = new FeatureDatos(
                Feature.NOTIFICACIONES,
                new BigDecimal("4.50"),
                "ZXCVB"
        );
        repo.save(datos);

        Optional<FeatureDatos> res = repo.findByCodigoDesbloqueo("ZXCVB");

        assertThat(res).isPresent();
        assertThat(res.get().getCodigoDesbloqueo()).isEqualTo("ZXCVB");
        assertThat(res.get().getFeature()).isEqualTo(Feature.NOTIFICACIONES);
    }

    @Test
    void findByCodigoDesbloqueo_cuandoNoExiste_devuelveEmpty() {
        Optional<FeatureDatos> res = repo.findByCodigoDesbloqueo("XXXXX");

        assertThat(res).isEmpty();
    }
}