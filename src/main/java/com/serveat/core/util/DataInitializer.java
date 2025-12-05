package com.serveat.core.util;

import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.repository.usuario.EmpleadoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(EmpleadoRepository empleadoRepository,
                                   ClienteRepository clienteRepository) {
        return args -> {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String pass = encoder.encode("123456");

            //   EMPLEADOS INICIALES
            if (empleadoRepository.count() == 0) {
                System.out.println("Insertando empleados iniciales...");

                empleadoRepository.save(new Empleado(
                        "Camarero Demo",
                        "camarero1",
                        pass,
                        "612345678",
                        "camarero@gmail.com",
                        "Calle del camarero nº1",
                        "CAMARERO",
                        true
                ));

                empleadoRepository.save(new Empleado(
                        "Cocinero Demo",
                        "cocinero1",
                        pass,
                        "712345678",
                        "cocinero@gmail.com",
                        "Calle del cocinero nº25",
                        "COCINERO",
                        true
                ));

                empleadoRepository.save(new Empleado(
                        "Repartidor Demo",
                        "repartidor1",
                        pass,
                        "798765432",
                        "repartidor@gmail.com",
                        "Calle del repartidor nº99",
                        "REPARTIDOR",
                        true
                ));

                empleadoRepository.save(new Empleado(
                        "Administrador",
                        "admin1",
                        pass,
                        "698765432",
                        "admin@gmail.com",
                        "Calle del administrador nº10",
                        "ADMIN",
                        true
                ));

                System.out.println("Empleados iniciales creados.");
            }

            //   CLIENTE INICIAL
            if (clienteRepository.count() == 0) {
                System.out.println("Insertando cliente inicial...");

                Cliente cliente = new Cliente();
                cliente.setNombre("Cliente Demo");
                cliente.setEmail("cliente@demo.com");
                cliente.setUsername("cliente1");
                cliente.setPassword(pass);
                cliente.setRol("CLIENTE");

                clienteRepository.save(cliente);

                System.out.println("Cliente de prueba creado.");
            }
        };
    }
}
