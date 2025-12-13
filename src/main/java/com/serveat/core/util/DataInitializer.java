package com.serveat.core.util;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureActiva;
import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.seguridad.FeatureActivaRepository;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.repository.usuario.EmpleadoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(EmpleadoRepository empleadoRepository,
                                   ClienteRepository clienteRepository,
                                   CategoriaRepository categoriaRepository,
                                   ProductoRepository productoRepository,
                                   FeatureActivaRepository featureActivaRepository) {
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
                cliente.setDireccion("Casa del cliente");
                cliente.setTelefono("696369963");
                cliente.setUsername("cliente1");
                cliente.setPassword(pass);
                cliente.setRol("CLIENTE");

                clienteRepository.save(cliente);

                System.out.println("Cliente de prueba creado.");
            }


            // CATEGORIAS
            if (categoriaRepository.count() == 0) {
                System.out.println("Insertando categorías iniciales...");

                Categoria burgers = new Categoria();
                burgers.setNombre("Hamburguesas");

                Categoria pizzas = new Categoria();
                pizzas.setNombre("Pizzas");

                Categoria bebidas = new Categoria();
                bebidas.setNombre("Bebidas");

                categoriaRepository.save(burgers);
                categoriaRepository.save(pizzas);
                categoriaRepository.save(bebidas);

                System.out.println("Categorías iniciales creadas.");
            }

            // PRODUCTOS

            if (productoRepository.count() == 0) {
                System.out.println("Insertando productos iniciales...");

                // Recuperar categorías ya guardadas (por nombre)
                Categoria burgers = categoriaRepository.findByNombre("Hamburguesas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Hamburguesas"));
                Categoria pizzas = categoriaRepository.findByNombre("Pizzas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Pizzas"));
                Categoria bebidas = categoriaRepository.findByNombre("Bebidas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Bebidas"));

                Producto p1 = new Producto();
                p1.setCodigo("BURG-001");
                p1.setNombre("Hamburguesa Clásica");
                p1.setDescripcion("Carne de ternera, lechuga y tomate");
                p1.setPrecio(new BigDecimal("8.50"));
                p1.setCategoria(burgers);

                Producto p2 = new Producto();
                p2.setCodigo("BURG-002");
                p2.setNombre("Hamburguesa Doble Queso");
                p2.setDescripcion("Doble carne, doble queso");
                p2.setPrecio(new BigDecimal("10.00"));
                p2.setCategoria(burgers);

                Producto p3 = new Producto();
                p3.setCodigo("PIZZ-001");
                p3.setNombre("Pizza Margarita");
                p3.setDescripcion("Tomate, mozzarella y albahaca");
                p3.setPrecio(new BigDecimal("9.00"));
                p3.setCategoria(pizzas);

                Producto p4 = new Producto();
                p4.setCodigo("BEB-001");
                p4.setNombre("Coca-Cola");
                p4.setDescripcion("Refresco frío");
                p4.setPrecio(new BigDecimal("2.50"));
                p4.setCategoria(bebidas);

                productoRepository.save(p1);
                productoRepository.save(p2);
                productoRepository.save(p3);
                productoRepository.save(p4);

                System.out.println("Productos iniciales creados.");
            }

            // FEATURES (todas desactivadas por defecto)
            for (Feature f : Feature.values()) {
                featureActivaRepository.findByFeature(f)
                        .orElseGet(() -> featureActivaRepository.save(new FeatureActiva(f)));
            }
            System.out.println("Features inicializadas (faltantes creadas) a false.");

        };
    }
}
