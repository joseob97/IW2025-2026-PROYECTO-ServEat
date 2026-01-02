package com.serveat.service.seguridad.impl;

import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureActiva;
import com.serveat.repository.seguridad.FeatureActivaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeatureServiceImplTest {

    @Mock
    private FeatureActivaRepository featureRepo;

    @InjectMocks
    private FeatureServiceImpl service;

    @Test
    void tieneFeature_si_existe_y_activa_devuelve_true() {
        FeatureActiva fa = featureActiva(Feature.INGREDIENTES, true, LocalDateTime.now().minusDays(1));
        when(featureRepo.findByFeature(Feature.INGREDIENTES)).thenReturn(Optional.of(fa));

        boolean res = service.tieneFeature(Feature.INGREDIENTES);

        assertThat(res).isTrue();
        verify(featureRepo).findByFeature(Feature.INGREDIENTES);
    }

    @Test
    void tieneFeature_si_existe_y_inactiva_devuelve_false() {
        FeatureActiva fa = featureActiva(Feature.INGREDIENTES, false, null);
        when(featureRepo.findByFeature(Feature.INGREDIENTES)).thenReturn(Optional.of(fa));

        boolean res = service.tieneFeature(Feature.INGREDIENTES);

        assertThat(res).isFalse();
        verify(featureRepo).findByFeature(Feature.INGREDIENTES);
    }

    @Test
    void tieneFeature_si_no_existe_devuelve_false() {
        when(featureRepo.findByFeature(Feature.INGREDIENTES)).thenReturn(Optional.empty());

        boolean res = service.tieneFeature(Feature.INGREDIENTES);

        assertThat(res).isFalse();
        verify(featureRepo).findByFeature(Feature.INGREDIENTES);
    }

    @Test
    void activarFeature_si_no_existe_inicializa_activa_setea_fecha_y_guarda() {
        when(featureRepo.findByFeature(Feature.NOTIFICACIONES)).thenReturn(Optional.empty());
        ArgumentCaptor<FeatureActiva> captor = ArgumentCaptor.forClass(FeatureActiva.class);

        service.activarFeature(Feature.NOTIFICACIONES);

        verify(featureRepo).save(captor.capture());
        FeatureActiva saved = captor.getValue();

        assertThat(saved.getFeature()).isEqualTo(Feature.NOTIFICACIONES);
        assertThat(saved.isActiva()).isTrue();
        assertThat(saved.getActivadaEn()).isNotNull();
    }

    @Test
    void activarFeature_si_existe_pero_ya_activa_no_guarda() {
        FeatureActiva fa = featureActiva(Feature.PAGO_ONLINE, true, LocalDateTime.now().minusHours(2));
        when(featureRepo.findByFeature(Feature.PAGO_ONLINE)).thenReturn(Optional.of(fa));

        service.activarFeature(Feature.PAGO_ONLINE);

        verify(featureRepo, never()).save(any());
        assertThat(fa.isActiva()).isTrue();
        assertThat(fa.getActivadaEn()).isNotNull();
    }

    @Test
    void activarFeature_si_existe_y_esta_inactiva_la_activa_setea_fecha_y_guarda() {
        FeatureActiva fa = featureActiva(Feature.PAGO_ONLINE, false, null);
        when(featureRepo.findByFeature(Feature.PAGO_ONLINE)).thenReturn(Optional.of(fa));

        service.activarFeature(Feature.PAGO_ONLINE);

        verify(featureRepo).save(fa);
        assertThat(fa.isActiva()).isTrue();
        assertThat(fa.getActivadaEn()).isNotNull();
    }

    @Test
    void desactivarFeature_si_no_existe_lanza_illegalState() {
        when(featureRepo.findByFeature(Feature.EXPORTAR_DATOS)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.desactivarFeature(Feature.EXPORTAR_DATOS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Feature no inicializada");

        verify(featureRepo, never()).save(any());
    }

    @Test
    void desactivarFeature_si_existe_pero_ya_inactiva_no_guarda() {
        FeatureActiva fa = featureActiva(Feature.EXPORTAR_DATOS, false, null);
        when(featureRepo.findByFeature(Feature.EXPORTAR_DATOS)).thenReturn(Optional.of(fa));

        service.desactivarFeature(Feature.EXPORTAR_DATOS);

        verify(featureRepo, never()).save(any());
        assertThat(fa.isActiva()).isFalse();
        assertThat(fa.getActivadaEn()).isNull();
    }

    @Test
    void desactivarFeature_si_existe_y_activa_la_desactiva_limpia_fecha_y_guarda() {
        FeatureActiva fa = featureActiva(Feature.EXPORTAR_DATOS, true, LocalDateTime.now().minusDays(3));
        when(featureRepo.findByFeature(Feature.EXPORTAR_DATOS)).thenReturn(Optional.of(fa));

        service.desactivarFeature(Feature.EXPORTAR_DATOS);

        verify(featureRepo).save(fa);
        assertThat(fa.isActiva()).isFalse();
        assertThat(fa.getActivadaEn()).isNull();
    }

    @Test
    void listarFeaturesActivos_devuelve_solo_las_activas_y_sin_duplicados() {
        FeatureActiva f1 = featureActiva(Feature.INGREDIENTES, true, LocalDateTime.now());
        FeatureActiva f2 = featureActiva(Feature.PAGO_ONLINE, false, null);
        FeatureActiva f3 = featureActiva(Feature.NOTIFICACIONES, true, LocalDateTime.now());

        when(featureRepo.findAll()).thenReturn(List.of(f1, f2, f3));

        Set<Feature> res = service.listarFeaturesActivos();

        assertThat(res).containsExactly(Feature.INGREDIENTES, Feature.NOTIFICACIONES);
        verify(featureRepo).findAll();
    }

    @Test
    void listarTodas_devuelve_todas_las_features_en_el_orden_del_repo() {
        FeatureActiva f1 = featureActiva(Feature.INGREDIENTES, true, LocalDateTime.now());
        FeatureActiva f2 = featureActiva(Feature.PAGO_ONLINE, false, null);

        when(featureRepo.findAll()).thenReturn(List.of(f1, f2));

        List<Feature> res = service.listarTodas();

        assertThat(res).containsExactly(Feature.INGREDIENTES, Feature.PAGO_ONLINE);
        verify(featureRepo).findAll();
    }

    private static FeatureActiva featureActiva(Feature feature, boolean activa, LocalDateTime activadaEn) {
        FeatureActiva fa = new FeatureActiva(feature);
        fa.setActiva(activa);
        fa.setActivadaEn(activadaEn);
        return fa;
    }
}