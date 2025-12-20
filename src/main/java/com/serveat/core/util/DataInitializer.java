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
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

@Profile("dev")
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(EmpleadoRepository empleadoRepository,
                                   ClienteRepository clienteRepository,
                                   CategoriaRepository categoriaRepository,
                                   ProductoRepository productoRepository,
                                   FeatureActivaRepository featureActivaRepository) {
        return args -> {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String rawPassword = System.getenv("DEMO_PASSWORD");

            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalStateException(
                        "La variable de entorno DEMO_PASSWORD no está definida"
                );
            }

            String pass = encoder.encode(rawPassword);

            //   EMPLEADOS INICIALES
            insertarEmpleados(empleadoRepository, pass);

            //   CLIENTE INICIAL
            if (clienteRepository.count() == 0) {
                log.info("Insertando cliente inicial...");

                Cliente cliente = new Cliente();
                cliente.setNombre("Cliente Demo");
                cliente.setEmail("cliente@demo.com");
                cliente.setDireccion("Casa del cliente");
                cliente.setTelefono("696369963");
                cliente.setUsername("cliente1");
                cliente.setPassword(pass);
                cliente.setRol("CLIENTE");

                clienteRepository.save(cliente);

                log.info("Cliente de prueba creado.");
            }


            // CATEGORIAS
            if (categoriaRepository.count() == 0) {
                log.info("Insertando categorías iniciales...");

                Categoria burgers = new Categoria();
                burgers.setNombre("Hamburguesas");

                Categoria pizzas = new Categoria();
                pizzas.setNombre("Pizzas");

                Categoria bebidas = new Categoria();
                bebidas.setNombre("Bebidas");

                categoriaRepository.save(burgers);
                categoriaRepository.save(pizzas);
                categoriaRepository.save(bebidas);

                log.info("Categorías iniciales creadas.");
            }

            // PRODUCTOS
            if (productoRepository.count() == 0) {
                log.info("Insertando productos iniciales...");

                Categoria burgers = categoriaRepository.findByNombre("Hamburguesas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Hamburguesas"));
                Categoria pizzas = categoriaRepository.findByNombre("Pizzas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Pizzas"));
                Categoria bebidas = categoriaRepository.findByNombre("Bebidas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Bebidas"));

                // Helper para no repetir código
                java.util.function.Function<String, String> img = name -> "/images/productos/" + name + ".png";

                // ---------- HAMBURGUESAS (10)
                Producto b1 = new Producto();
                b1.setCodigo("BURG-001");
                b1.setNombre("Hamburguesa Clásica");
                b1.setDescripcion("Carne de ternera, queso, lechuga y tomate.");
                b1.setPrecio(new BigDecimal("8.50"));
                b1.setCategoria(burgers);
                b1.setImagenUrl(img.apply("burg-001"));

                Producto b2 = new Producto();
                b2.setCodigo("BURG-002");
                b2.setNombre("Hamburguesa Doble Queso");
                b2.setDescripcion("Doble carne, doble queso cheddar.");
                b2.setPrecio(new BigDecimal("10.00"));
                b2.setCategoria(burgers);
                b2.setImagenUrl(img.apply("burg-002"));

                Producto b3 = new Producto();
                b3.setCodigo("BURG-003");
                b3.setNombre("Hamburguesa BBQ Bacon");
                b3.setDescripcion("Salsa BBQ, bacon crujiente y queso.");
                b3.setPrecio(new BigDecimal("10.50"));
                b3.setCategoria(burgers);
                b3.setImagenUrl(img.apply("burg-003"));

                Producto b4 = new Producto();
                b4.setCodigo("BURG-004");
                b4.setNombre("Hamburguesa Pollo Crispy");
                b4.setDescripcion("Pollo crujiente, lechuga y mayonesa.");
                b4.setPrecio(new BigDecimal("9.50"));
                b4.setCategoria(burgers);
                b4.setImagenUrl(img.apply("burg-004"));

                Producto b5 = new Producto();
                b5.setCodigo("BURG-005");
                b5.setNombre("Hamburguesa Veggie");
                b5.setDescripcion("Hamburguesa vegetal, tomate y rúcula.");
                b5.setPrecio(new BigDecimal("9.00"));
                b5.setCategoria(burgers);
                b5.setImagenUrl(img.apply("burg-005"));

                Producto b6 = new Producto();
                b6.setCodigo("BURG-006");
                b6.setNombre("Hamburguesa Picante Jalapeños");
                b6.setDescripcion("Jalapeños, queso y salsa picante.");
                b6.setPrecio(new BigDecimal("10.25"));
                b6.setCategoria(burgers);
                b6.setImagenUrl(img.apply("burg-006"));

                Producto b7 = new Producto();
                b7.setCodigo("BURG-007");
                b7.setNombre("Hamburguesa Trufa y Setas");
                b7.setDescripcion("Setas salteadas y mayo de trufa.");
                b7.setPrecio(new BigDecimal("11.50"));
                b7.setCategoria(burgers);
                b7.setImagenUrl(img.apply("burg-007"));

                Producto b8 = new Producto();
                b8.setCodigo("BURG-008");
                b8.setNombre("Hamburguesa Smash Burger");
                b8.setDescripcion("Smash doble, cebolla y queso fundido.");
                b8.setPrecio(new BigDecimal("10.75"));
                b8.setCategoria(burgers);
                b8.setImagenUrl(img.apply("burg-008"));

                Producto b9 = new Producto();
                b9.setCodigo("BURG-009");
                b9.setNombre("Hamburguesa 4 Quesos");
                b9.setDescripcion("Mezcla de 4 quesos fundidos.");
                b9.setPrecio(new BigDecimal("11.00"));
                b9.setCategoria(burgers);
                b9.setImagenUrl(img.apply("burg-009"));

                Producto b10 = new Producto();
                b10.setCodigo("BURG-010");
                b10.setNombre("Hamburguesa Deluxe");
                b10.setDescripcion("Burger gourmet con aros de cebolla.");
                b10.setPrecio(new BigDecimal("12.00"));
                b10.setCategoria(burgers);
                b10.setImagenUrl(img.apply("burg-010"));

                // ---------- PIZZAS (10)
                Producto p1 = new Producto();
                p1.setCodigo("PIZZ-001");
                p1.setNombre("Pizza Margarita");
                p1.setDescripcion("Tomate, mozzarella y albahaca.");
                p1.setPrecio(new BigDecimal("9.00"));
                p1.setCategoria(pizzas);
                p1.setImagenUrl(img.apply("pizz-001"));

                Producto p2 = new Producto();
                p2.setCodigo("PIZZ-002");
                p2.setNombre("Pizza Pepperoni");
                p2.setDescripcion("Pepperoni y mozzarella.");
                p2.setPrecio(new BigDecimal("10.50"));
                p2.setCategoria(pizzas);
                p2.setImagenUrl(img.apply("pizz-002"));

                Producto p3 = new Producto();
                p3.setCodigo("PIZZ-003");
                p3.setNombre("Pizza 4 Quesos");
                p3.setDescripcion("Mozzarella, gorgonzola, parmesano y cheddar.");
                p3.setPrecio(new BigDecimal("11.00"));
                p3.setCategoria(pizzas);
                p3.setImagenUrl(img.apply("pizz-003"));

                Producto p4 = new Producto();
                p4.setCodigo("PIZZ-004");
                p4.setNombre("Pizza Barbacoa");
                p4.setDescripcion("Pollo, salsa BBQ y cebolla.");
                p4.setPrecio(new BigDecimal("11.50"));
                p4.setCategoria(pizzas);
                p4.setImagenUrl(img.apply("pizz-004"));

                Producto p5 = new Producto();
                p5.setCodigo("PIZZ-005");
                p5.setNombre("Pizza Hawaiana");
                p5.setDescripcion("Jamón y piña.");
                p5.setPrecio(new BigDecimal("10.75"));
                p5.setCategoria(pizzas);
                p5.setImagenUrl(img.apply("pizz-005"));

                Producto p6 = new Producto();
                p6.setCodigo("PIZZ-006");
                p6.setNombre("Pizza Vegetariana");
                p6.setDescripcion("Verduras asadas y mozzarella.");
                p6.setPrecio(new BigDecimal("10.25"));
                p6.setCategoria(pizzas);
                p6.setImagenUrl(img.apply("pizz-006"));

                Producto p7 = new Producto();
                p7.setCodigo("PIZZ-007");
                p7.setNombre("Pizza Diavola");
                p7.setDescripcion("Salami picante y mozzarella.");
                p7.setPrecio(new BigDecimal("11.25"));
                p7.setCategoria(pizzas);
                p7.setImagenUrl(img.apply("pizz-007"));

                Producto p8 = new Producto();
                p8.setCodigo("PIZZ-008");
                p8.setNombre("Pizza Prosciutto y Rúcula");
                p8.setDescripcion("Jamón, rúcula y lascas de parmesano.");
                p8.setPrecio(new BigDecimal("12.00"));
                p8.setCategoria(pizzas);
                p8.setImagenUrl(img.apply("pizz-008"));

                Producto p9 = new Producto();
                p9.setCodigo("PIZZ-009");
                p9.setNombre("Pizza Carbonara");
                p9.setDescripcion("Bacon, crema y parmesano.");
                p9.setPrecio(new BigDecimal("11.75"));
                p9.setCategoria(pizzas);
                p9.setImagenUrl(img.apply("pizz-009"));

                Producto p10 = new Producto();
                p10.setCodigo("PIZZ-010");
                p10.setNombre("Pizza Tonno (Atún y Cebolla)");
                p10.setDescripcion("Atún, cebolla y mozzarella.");
                p10.setPrecio(new BigDecimal("11.25"));
                p10.setCategoria(pizzas);
                p10.setImagenUrl(img.apply("pizz-010"));

                // ---------- BEBIDAS (5)
                Producto d1 = new Producto();
                d1.setCodigo("BEB-001");
                d1.setNombre("Coca-Cola");
                d1.setDescripcion("Refresco frío con hielo.");
                d1.setPrecio(new BigDecimal("2.50"));
                d1.setCategoria(bebidas);
                d1.setImagenUrl(img.apply("beb-001"));

                Producto d2 = new Producto();
                d2.setCodigo("BEB-002");
                d2.setNombre("Agua Mineral");
                d2.setDescripcion("Agua fría (botella).");
                d2.setPrecio(new BigDecimal("1.80"));
                d2.setCategoria(bebidas);
                d2.setImagenUrl(img.apply("beb-002"));

                Producto d3 = new Producto();
                d3.setCodigo("BEB-003");
                d3.setNombre("Cerveza (0,33)");
                d3.setDescripcion("Cerveza fría (33cl).");
                d3.setPrecio(new BigDecimal("2.80"));
                d3.setCategoria(bebidas);
                d3.setImagenUrl(img.apply("beb-003"));

                Producto d4 = new Producto();
                d4.setCodigo("BEB-004");
                d4.setNombre("Zumo de Naranja");
                d4.setDescripcion("Zumo natural bien frío.");
                d4.setPrecio(new BigDecimal("2.90"));
                d4.setCategoria(bebidas);
                d4.setImagenUrl(img.apply("beb-004"));

                Producto d5 = new Producto();
                d5.setCodigo("BEB-005");
                d5.setNombre("Café Espresso");
                d5.setDescripcion("Espresso corto e intenso.");
                d5.setPrecio(new BigDecimal("1.60"));
                d5.setCategoria(bebidas);
                d5.setImagenUrl(img.apply("beb-005"));

                productoRepository.saveAll(List.of(
                        b1,b2,b3,b4,b5,b6,b7,b8,b9,b10,
                        p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,
                        d1,d2,d3,d4,d5
                ));

                log.info("25 productos iniciales creados.");
            }

        };
    }

    private static void insertarEmpleados(EmpleadoRepository empleadoRepository, String pass) {
        if (empleadoRepository.count() == 0) {
            log.info("Insertando empleados iniciales...");

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

            log.info("Empleados iniciales creados.");
        }
    }
}
