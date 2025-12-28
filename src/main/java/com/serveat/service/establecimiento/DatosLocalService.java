package com.serveat.service.establecimiento;

import com.serveat.domain.establecimiento.DatosLocal;
import com.serveat.repository.establecimiento.DatosLocalRepository;
import org.springframework.stereotype.Service;

@Service
public class DatosLocalService {

    private final DatosLocalRepository repository;

    public DatosLocalService(DatosLocalRepository repository) {
        this.repository = repository;
    }

    public DatosLocal obtenerDatos() {
        return repository.findAll()
                .stream()
                .findFirst()
                .orElseGet(this::crearDatosPorDefecto);
    }

    public DatosLocal guardar(DatosLocal datos) {
        return repository.save(datos);
    }

    private DatosLocal crearDatosPorDefecto() {
        DatosLocal datos = new DatosLocal();
        datos.setNombreLocal("ServEat");
        datos.setDescripcion("ServEat es una plataforma para pedir a domicilio, recoger en local o gestionar pedidos en sala.");
        datos.setDescripcion2("Esta web está en desarrollo para el proyecto de Ingeniería Web.");
        datos.setHorario("Lunes a Domingo 13:00 - 00:00");
        datos.setTelefono("123456789");
        datos.setEmail("contacto@serveat.com");
        datos.setDireccion("Calle de ServEat nº1, 11405, Jerez de la Frontera, Cádiz.");
        return repository.save(datos);
    }
}
