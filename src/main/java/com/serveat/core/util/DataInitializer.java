package com.serveat.core.util;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.*;
import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.repository.menu.IngredienteRepository;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.repository.seguridad.FeatureDatosRepository;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.pedido.PedidoCalculoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.serveat.domain.seguridad.Feature;
import com.serveat.domain.seguridad.FeatureDatos;

import java.security.SecureRandom;


@Profile("dev")
@Configuration
public class DataInitializer {

    public static final String EXTRA_JALAPENOS = "Extra jalapeños";
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    public static final String LECHUGA = "Lechuga";
    public static final String TOMATE = "Tomate";
    public static final String PAN_BRIOCHE = "Pan Brioche";
    public static final String CARNE_DE_TERNERA = "Carne de ternera";
    public static final String POLLO_CRISPY = "Pollo crispy";
    public static final String QUESO_CHEDDAR = "Queso cheddar";
    public static final String CEBOLLA = "Cebolla";
    public static final String PEPINILLO = "Pepinillo";
    public static final String MAYONESA = "Mayonesa";
    public static final String SALSA_BBQ = "Salsa BBQ";
    public static final String JALAPENOS = "Jalapeños";
    public static final String BACON = "Bacon";
    public static final String SETAS = "Setas";
    public static final String JAMON = "Jamón";
    public static final String CHAMPINONES = "Champiñones";
    public static final String ACEITUNAS = "Aceitunas";
    public static final String PARMESANO = "Parmesano";
    public static final String GORGONZOLA = "Gorgonzola";
    public static final String CHEDDAR_PIZZA = "Cheddar (pizza)";
    public static final String BACON_PIZZA = "Bacon (pizza)";
    public static final String EXTRA_QUESO = "Extra queso";
    public static final String EXTRA_BACON = "Extra bacon";
    public static final String EXTRA_PEPPERONI = "Extra pepperoni";
    public static final String EXTRA_CHAMPINONES = "Extra champiñones";
    public static final String MAYO_DE_TRUFA = "Mayo de trufa";
    public static final String TOMATE_PIZZA = "Tomate (pizza)";
    public static final String MOZZARELLA = "Mozzarella";
    public static final String PEPPERONI = "Pepperoni";
    public static final String SALAMI_PICANTE = "Salami picante";
    public static final String PINA = "Piña";
    public static final String ATUN = "Atún";
    public static final String RUCULA = "Rúcula";
    public static final String REPARTIDOR_1 = "repartidor1";
    public static final String COCINERO_1 = "cocinero1";
    public static final String CLIENTE = "CLIENTE";

    @Bean
    CommandLineRunner initDatabase(EmpleadoRepository empleadoRepository,
                                   ClienteRepository clienteRepository,
                                   CategoriaRepository categoriaRepository,
                                   ProductoRepository productoRepository,
                                   IngredienteRepository ingredienteRepository,
                                   PedidoRepository pedidoRepository,
                                   PagoRepository pagoRepository,
                                   PedidoCalculoService pedidoCalculoService,
                                   FeatureDatosRepository featureDatosRepository) {


        return args -> {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String rawPassword = System.getenv("DEMO_PASSWORD");

            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalStateException("La variable de entorno DEMO_PASSWORD no está definida");
            }

            String pass = encoder.encode(rawPassword);

            // EMPLEADOS
            insertarEmpleados(empleadoRepository, pass);

            // FEATURES (datos base: precio + código)
            inicializarFeatureDatos(featureDatosRepository);

            // CLIENTES
            if (clienteRepository.count() == 0) {
                log.info("Insertando clientes iniciales...");

                Cliente cliente = new Cliente();
                cliente.setNombre("Cliente Demo");
                cliente.setEmail("cliente@demo.com");
                cliente.setDireccion("Casa del cliente");
                cliente.setTelefono("696369963");
                cliente.setUsername("cliente1");
                cliente.setPassword(pass);
                cliente.setRol(CLIENTE);
                clienteRepository.save(cliente);

                Cliente cliente2 = new Cliente();
                cliente2.setNombre("Cliente Test");
                cliente2.setEmail("cliente2@demo.com");
                cliente2.setDireccion("Dirección cliente test");
                cliente2.setTelefono("600123456");
                cliente2.setUsername("cliente2");
                cliente2.setPassword(pass);
                cliente2.setRol(CLIENTE);
                clienteRepository.save(cliente2);

                Cliente cliente3 = new Cliente();
                cliente3.setNombre("Cliente Prueba");
                cliente3.setEmail("prueba@prueba.com");
                cliente3.setDireccion("Dirección cliente prueba");
                cliente3.setTelefono("123654987");
                cliente3.setUsername("prueba");
                cliente3.setPassword(pass);
                cliente3.setRol(CLIENTE);
                clienteRepository.save(cliente3);

                log.info("Clientes de prueba creados.");
            }

            // CATEGORÍAS
            if (categoriaRepository.count() == 0) {
                log.info("Insertando categorías iniciales...");

                Categoria burgers = new Categoria();
                burgers.setNombre("Hamburguesas");

                Categoria pizzas = new Categoria();
                pizzas.setNombre("Pizzas");

                Categoria bebidas = new Categoria();
                bebidas.setNombre("Bebidas");

                categoriaRepository.saveAll(List.of(burgers, pizzas, bebidas));
                log.info("Categorías iniciales creadas.");
            }

            // INGREDIENTES
            if (ingredienteRepository.count() == 0) {
                log.info("Insertando ingredientes iniciales...");

                ingredienteRepository.saveAll(List.of(
                        ing(PAN_BRIOCHE, "0.00"),
                        ing(CARNE_DE_TERNERA, "0.00"),
                        ing(POLLO_CRISPY, "0.00"),
                        ing(QUESO_CHEDDAR, "0.00"),
                        ing(LECHUGA, "0.00"),
                        ing(TOMATE, "0.00"),
                        ing(CEBOLLA, "0.00"),
                        ing(PEPINILLO, "0.00"),
                        ing(MAYONESA, "0.00"),
                        ing(SALSA_BBQ, "0.00"),
                        ing(JALAPENOS, "0.00"),
                        ing(BACON, "0.00"),
                        ing(SETAS, "0.00"),
                        ing(MAYO_DE_TRUFA, "0.00"),
                        ing(TOMATE_PIZZA, "0.00"),
                        ing(MOZZARELLA, "0.00"),
                        ing(PEPPERONI, "0.00"),
                        ing(SALAMI_PICANTE, "0.00"),
                        ing(JAMON, "0.00"),
                        ing(PINA, "0.00"),
                        ing(ATUN, "0.00"),
                        ing(CHAMPINONES, "0.00"),
                        ing(ACEITUNAS, "0.00"),
                        ing(RUCULA, "0.00"),
                        ing(PARMESANO, "0.00"),
                        ing(GORGONZOLA, "0.00"),
                        ing(CHEDDAR_PIZZA, "0.00"),
                        ing(BACON_PIZZA, "0.00"),

                        ing(EXTRA_QUESO, "0.80"),
                        ing(EXTRA_BACON, "1.20"),
                        ing(EXTRA_JALAPENOS, "0.50"),
                        ing(EXTRA_PEPPERONI, "0.90"),
                        ing(EXTRA_CHAMPINONES, "0.70")
                ));


                log.info("Ingredientes iniciales creados.");
            }

            // PRODUCTOS + RECETAS
            if (productoRepository.count() == 0) {
                log.info("Insertando productos iniciales...");

                Categoria burgers = categoriaRepository.findByNombre("Hamburguesas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Hamburguesas"));
                Categoria pizzas = categoriaRepository.findByNombre("Pizzas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Pizzas"));
                Categoria bebidas = categoriaRepository.findByNombre("Bebidas")
                        .orElseThrow(() -> new IllegalStateException("No existe la categoría Bebidas"));

                java.util.function.Function<String, String> img = name -> "/images/productos/" + name + ".png";

                // Hamburguesas
                Producto b1 = prod("BURG-001", "Hamburguesa Clásica", "Carne de ternera, queso, lechuga y tomate.", "8.50", burgers, img.apply("burg-001"));
                Producto b2 = prod("BURG-002", "Hamburguesa Doble Queso", "Doble carne, doble queso cheddar.", "10.00", burgers, img.apply("burg-002"));
                Producto b3 = prod("BURG-003", "Hamburguesa BBQ Bacon", "Salsa BBQ, bacon crujiente y queso.", "10.50", burgers, img.apply("burg-003"));
                Producto b4 = prod("BURG-004", "Hamburguesa Pollo Crispy", "Pollo crujiente, lechuga y mayonesa.", "9.50", burgers, img.apply("burg-004"));
                Producto b5 = prod("BURG-005", "Hamburguesa Veggie", "Hamburguesa vegetal, tomate y rúcula.", "9.00", burgers, img.apply("burg-005"));
                Producto b6 = prod("BURG-006", "Hamburguesa Picante Jalapeños", "Jalapeños, queso y salsa picante.", "10.25", burgers, img.apply("burg-006"));
                Producto b7 = prod("BURG-007", "Hamburguesa Trufa y Setas", "Setas salteadas y mayo de trufa.", "11.50", burgers, img.apply("burg-007"));
                Producto b8 = prod("BURG-008", "Hamburguesa Smash Burger", "Smash doble, cebolla y queso fundido.", "10.75", burgers, img.apply("burg-008"));
                Producto b9 = prod("BURG-009", "Hamburguesa 4 Quesos", "Mezcla de 4 quesos fundidos.", "11.00", burgers, img.apply("burg-009"));
                Producto b10 = prod("BURG-010", "Hamburguesa Deluxe", "Burger gourmet con aros de cebolla.", "12.00", burgers, img.apply("burg-010"));

                // Pizzas
                Producto p1 = prod("PIZZ-001", "Pizza Margarita", "Tomate, mozzarella y albahaca.", "9.00", pizzas, img.apply("pizz-001"));
                Producto p2 = prod("PIZZ-002", "Pizza Pepperoni", "Pepperoni y mozzarella.", "10.50", pizzas, img.apply("pizz-002"));
                Producto p3 = prod("PIZZ-003", "Pizza 4 Quesos", "Mozzarella, gorgonzola, parmesano y cheddar.", "11.00", pizzas, img.apply("pizz-003"));
                Producto p4 = prod("PIZZ-004", "Pizza Barbacoa", "Pollo, salsa BBQ y cebolla.", "11.50", pizzas, img.apply("pizz-004"));
                Producto p5 = prod("PIZZ-005", "Pizza Hawaiana", "Jamón y piña.", "10.75", pizzas, img.apply("pizz-005"));
                Producto p6 = prod("PIZZ-006", "Pizza Vegetariana", "Verduras asadas y mozzarella.", "10.25", pizzas, img.apply("pizz-006"));
                Producto p7 = prod("PIZZ-007", "Pizza Diavola", "Salami picante y mozzarella.", "11.25", pizzas, img.apply("pizz-007"));
                Producto p8 = prod("PIZZ-008", "Pizza Prosciutto y Rúcula", "Jamón, rúcula y lascas de parmesano.", "12.00", pizzas, img.apply("pizz-008"));
                Producto p9 = prod("PIZZ-009", "Pizza Carbonara", "Bacon, crema y parmesano.", "11.75", pizzas, img.apply("pizz-009"));
                Producto p10 = prod("PIZZ-010", "Pizza Tonno (Atún y Cebolla)", "Atún, cebolla y mozzarella.", "11.25", pizzas, img.apply("pizz-010"));

                // Bebidas
                Producto d1 = prod("BEB-001", "Coca-Cola", "Refresco frío con hielo.", "2.50", bebidas, img.apply("beb-001"));
                Producto d2 = prod("BEB-002", "Agua Mineral", "Agua fría (botella).", "1.80", bebidas, img.apply("beb-002"));
                Producto d3 = prod("BEB-003", "Cerveza (0,33)", "Cerveza fría (33cl).", "2.80", bebidas, img.apply("beb-003"));
                Producto d4 = prod("BEB-004", "Zumo de Naranja", "Zumo natural bien frío.", "2.90", bebidas, img.apply("beb-004"));
                Producto d5 = prod("BEB-005", "Café Espresso", "Espresso corto e intenso.", "1.60", bebidas, img.apply("beb-005"));

                productoRepository.saveAll(List.of(
                        b1,b2,b3,b4,b5,b6,b7,b8,b9,b10,
                        p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,
                        d1,d2,d3,d4,d5
                ));

                Map<String, Ingrediente> ing = ingredienteRepository.findAll().stream()
                        .filter(x -> x.getNombre() != null)
                        .collect(Collectors.toMap(Ingrediente::getNombre, x -> x, (a, b) -> a));

                recetaBurgerBase(b1, ing);
                recetaBurgerBase(b2, ing);
                recetaBurgerBase(b3, ing);
                recetaBurgerBase(b4, ing);
                recetaBurgerBase(b6, ing);
                recetaBurgerBase(b7, ing);
                recetaBurgerBase(b8, ing);
                recetaBurgerBase(b9, ing);
                recetaBurgerBase(b10, ing);

                recetaBurgerVeggie(b5, ing);

                recetaPizzaBase(p1, ing);
                recetaPizzaBase(p2, ing);
                recetaPizzaBase(p3, ing);
                recetaPizzaBase(p4, ing);
                recetaPizzaBase(p5, ing);
                recetaPizzaBase(p6, ing);
                recetaPizzaBase(p7, ing);
                recetaPizzaBase(p8, ing);
                recetaPizzaBase(p9, ing);
                recetaPizzaBase(p10, ing);

                addNormal(b2, ing.get(QUESO_CHEDDAR));
                addExtra(b2, ing.get(EXTRA_QUESO), "0.80");

                addNormal(b3, ing.get(SALSA_BBQ));
                addNormal(b3, ing.get(BACON));
                addExtra(b3, ing.get(EXTRA_BACON), "1.20");

                setOpcionalPorDefecto(b4, ing.get(CARNE_DE_TERNERA), true);
                addNormal(b4, ing.get(POLLO_CRISPY));
                addNormal(b4, ing.get(MAYONESA));

                addNormal(b6, ing.get(JALAPENOS));
                addExtra(b6, ing.get(EXTRA_JALAPENOS), "0.50");

                addNormal(b7, ing.get(SETAS));
                addNormal(b7, ing.get(MAYO_DE_TRUFA));

                addNormal(p2, ing.get(PEPPERONI));
                addExtra(p2, ing.get(EXTRA_PEPPERONI), "0.90");

                addNormal(p3, ing.get(GORGONZOLA));
                addNormal(p3, ing.get(PARMESANO));
                addNormal(p3, ing.get(CHEDDAR_PIZZA));
                addExtra(p3, ing.get(EXTRA_QUESO), "0.80");

                addNormal(p4, ing.get(SALSA_BBQ));
                addNormal(p4, ing.get(CEBOLLA));
                addNormal(p4, ing.get(POLLO_CRISPY));
                addExtra(p4, ing.get(EXTRA_BACON), "1.20");

                addNormal(p5, ing.get(JAMON));
                addNormal(p5, ing.get(PINA));

                addNormal(p6, ing.get(CHAMPINONES));
                addNormal(p6, ing.get(ACEITUNAS));
                addNormal(p6, ing.get(CEBOLLA));
                addExtra(p6, ing.get(EXTRA_CHAMPINONES), "0.70");

                addNormal(p7, ing.get(SALAMI_PICANTE));
                addExtra(p7, ing.get(EXTRA_JALAPENOS), "0.50");

                addNormal(p8, ing.get(JAMON));
                addNormal(p8, ing.get(RUCULA));
                addNormal(p8, ing.get(PARMESANO));

                addNormal(p9, ing.get(BACON_PIZZA));
                addNormal(p9, ing.get(PARMESANO));

                addNormal(p10, ing.get(ATUN));
                addNormal(p10, ing.get(CEBOLLA));

                productoRepository.saveAll(List.of(
                        b1,b2,b3,b4,b5,b6,b7,b8,b9,b10,
                        p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,
                        d1,d2,d3,d4,d5
                ));

                log.info("25 productos iniciales creados (con ingredientes).");
            }

            // PEDIDOS + PAGOS
            if (pedidoRepository.count() == 0) {
                log.info("Insertando pedidos de demo (recoger + domicilio) ...");

                Cliente cliente = clienteRepository.findByUsername("cliente1")
                        .orElseThrow(() -> new IllegalStateException("cliente1 no existe"));

                Empleado repartidor = empleadoRepository.findByUsername(REPARTIDOR_1)
                        .orElseThrow(() -> new IllegalStateException("repartidor1 no existe"));

                Producto burg1 = productoRepository.findByCodigo("BURG-001")
                        .orElseThrow(() -> new IllegalStateException("BURG-001 no existe"));
                Producto pizz1 = productoRepository.findByCodigo("PIZZ-001")
                        .orElseThrow(() -> new IllegalStateException("PIZZ-001 no existe"));
                Producto beb1 = productoRepository.findByCodigo("BEB-001")
                        .orElseThrow(() -> new IllegalStateException("BEB-001 no existe"));

                Pedido recoger1 = nuevoPedido(cliente, TipoPedidoCliente.RECOGER, null);
                recoger1.setEstado(EstadoPedido.EN_CURSO);
                recoger1.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
                recoger1.setEstadoReparto(EstadoReparto.NO_APLICA);
                addLinea(recoger1, burg1, 1);
                addLinea(recoger1, beb1, 1);

                Pedido recoger2 = nuevoPedido(cliente, TipoPedidoCliente.RECOGER, null);
                recoger2.setEstado(EstadoPedido.EN_COCINA);
                recoger2.setEstadoCocina(EstadoCocina.EN_PREPARACION);
                recoger2.setEstadoReparto(EstadoReparto.NO_APLICA);
                recoger2.setModificadoPor(COCINERO_1);
                recoger2.setFechaUltimaModificacion(LocalDateTime.now().minusMinutes(20));
                addLinea(recoger2, pizz1, 1);

                Pedido recoger3 = nuevoPedido(cliente, TipoPedidoCliente.RECOGER, null);
                recoger3.setEstado(EstadoPedido.EN_COCINA);
                recoger3.setEstadoCocina(EstadoCocina.LISTO);
                recoger3.setEstadoReparto(EstadoReparto.NO_APLICA);
                recoger3.setModificadoPor(COCINERO_1);
                recoger3.setFechaUltimaModificacion(LocalDateTime.now().minusMinutes(10));
                addLinea(recoger3, burg1, 2);

                Pedido dom1 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "Calle Falsa 123");
                dom1.setEstado(EstadoPedido.EN_COCINA);
                dom1.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
                dom1.setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);
                addLinea(dom1, pizz1, 1);
                addLinea(dom1, beb1, 2);

                Pedido dom2 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "Av. Principal 45");
                dom2.setEstado(EstadoPedido.EN_COCINA);
                dom2.setEstadoCocina(EstadoCocina.LISTO);
                dom2.setEstadoReparto(EstadoReparto.PENDIENTE_ASIGNACION);
                dom2.setModificadoPor(COCINERO_1);
                dom2.setFechaUltimaModificacion(LocalDateTime.now().minusMinutes(5));
                addLinea(dom2, burg1, 1);
                addLinea(dom2, beb1, 1);

                Pedido dom3 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "C/ Mayor 9");
                dom3.setEstado(EstadoPedido.EN_COCINA);
                dom3.setEstadoCocina(EstadoCocina.LISTO);
                dom3.setEstadoReparto(EstadoReparto.ASIGNADO);
                dom3.setRepartidor(repartidor);
                dom3.setFechaAsignacionReparto(LocalDateTime.now().minusMinutes(15));
                dom3.setModificadoPor(REPARTIDOR_1);
                dom3.setFechaUltimaModificacion(LocalDateTime.now().minusMinutes(15));
                addLinea(dom3, pizz1, 2);

                Pedido dom4 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "Plaza España 1");
                dom4.setEstado(EstadoPedido.EN_COCINA);
                dom4.setEstadoCocina(EstadoCocina.LISTO);
                dom4.setEstadoReparto(EstadoReparto.EN_REPARTO);
                dom4.setRepartidor(repartidor);
                dom4.setFechaAsignacionReparto(LocalDateTime.now().minusMinutes(30));
                dom4.setFechaSalidaReparto(LocalDateTime.now().minusMinutes(10));
                dom4.setModificadoPor(REPARTIDOR_1);
                dom4.setFechaUltimaModificacion(LocalDateTime.now().minusMinutes(10));
                addLinea(dom4, burg1, 1);
                addLinea(dom4, beb1, 1);

                Pedido dom5 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "C/ Sol 77");
                dom5.setEstado(EstadoPedido.EN_COCINA);
                dom5.setEstadoCocina(EstadoCocina.LISTO);
                dom5.setEstadoReparto(EstadoReparto.ENTREGADO);
                dom5.setRepartidor(repartidor);
                dom5.setFechaAsignacionReparto(LocalDateTime.now().minusHours(1));
                dom5.setFechaSalidaReparto(LocalDateTime.now().minusMinutes(40));
                dom5.setFechaEntrega(LocalDateTime.now().minusMinutes(15));
                dom5.setModificadoPor(REPARTIDOR_1);
                dom5.setFechaUltimaModificacion(LocalDateTime.now().minusMinutes(15));
                addLinea(dom5, pizz1, 1);

                Pedido dom6 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "C/ Luna 5");
                dom6.setEstado(EstadoPedido.EN_COCINA);
                dom6.setEstadoCocina(EstadoCocina.LISTO);
                dom6.setEstadoReparto(EstadoReparto.INCIDENCIA);
                dom6.setRepartidor(repartidor);
                dom6.setIncidenciaReparto("No hay nadie en casa");
                dom6.setFechaAsignacionReparto(LocalDateTime.now().minusHours(2));
                dom6.setModificadoPor(REPARTIDOR_1);
                dom6.setFechaUltimaModificacion(LocalDateTime.now().minusHours(2));
                addLinea(dom6, burg1, 1);

                Pedido cancelado = nuevoPedido(cliente, TipoPedidoCliente.RECOGER, null);
                cancelado.setEstado(EstadoPedido.ANULADO);
                cancelado.setEstadoCocina(EstadoCocina.CANCELADO);
                cancelado.setCanceladoPor("camarero1");
                cancelado.setMotivoCancelacion("Cliente canceló");
                cancelado.setFechaCancelacion(LocalDateTime.now().minusDays(1));
                cancelado.setEstadoReparto(EstadoReparto.NO_APLICA);
                addLinea(cancelado, beb1, 1);

                pedidoRepository.saveAll(List.of(
                        recoger1, recoger2, recoger3,
                        dom1, dom2, dom3, dom4, dom5, dom6,
                        cancelado
                ));

                pedidoRepository.flush();

                crearPago(pagoRepository, pedidoCalculoService, dom2, MetodoPago.TARJETA, EstadoPago.CONFIRMADO);
                crearPago(pagoRepository, pedidoCalculoService, dom3, MetodoPago.PAYPAL, EstadoPago.CONFIRMADO);
                crearPago(pagoRepository, pedidoCalculoService, dom4, MetodoPago.TARJETA, EstadoPago.CONFIRMADO);
                crearPago(pagoRepository, pedidoCalculoService, dom5, MetodoPago.EFECTIVO, EstadoPago.CONFIRMADO);

                crearPago(pagoRepository, pedidoCalculoService, dom1, MetodoPago.TARJETA, EstadoPago.PENDIENTE);
                crearPago(pagoRepository, pedidoCalculoService, dom6, MetodoPago.PAYPAL, EstadoPago.FALLIDO);

                log.info("Pedidos y pagos demo creados.");
            }

        };
    }

    private static void insertarEmpleados(EmpleadoRepository empleadoRepository, String pass) {
        if (empleadoRepository.count() == 0) {
            log.info("Insertando empleados iniciales...");

            empleadoRepository.save(new Empleado(
                    "Camarero Demo", "camarero1", pass, "612345678",
                    "camarero@gmail.com", "Calle del camarero nº1", "CAMARERO", true
            ));

            empleadoRepository.save(new Empleado(
                    "Cocinero Demo", COCINERO_1, pass, "712345678",
                    "cocinero@gmail.com", "Calle del cocinero nº25", "COCINERO", true
            ));

            empleadoRepository.save(new Empleado(
                    "Repartidor Demo", REPARTIDOR_1, pass, "798765432",
                    "repartidor@gmail.com", "Calle del repartidor nº99", "REPARTIDOR", true
            ));

            empleadoRepository.save(new Empleado(
                    "Administrador", "admin1", pass, "698765432",
                    "admin@gmail.com", "Calle del administrador nº10", "ADMIN", true
            ));

            log.info("Empleados iniciales creados.");
        }
    }

    private static Producto prod(String codigo, String nombre, String descripcion, String precio,
                                 Categoria categoria, String imagenUrl) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setDescripcion(descripcion);
        p.setPrecio(new BigDecimal(precio));
        p.setCategoria(categoria);
        p.setImagenUrl(imagenUrl);
        return p;
    }

    private static Ingrediente ing(String nombre, String precioExtra) {
        Ingrediente i = new Ingrediente();
        i.setNombre(nombre);
        i.setPrecioExtra(new BigDecimal(precioExtra));
        return i;
    }

    private static Pedido nuevoPedido(Cliente cliente, TipoPedidoCliente tipo, String direccionEntrega) {
        Pedido p = new Pedido();
        p.setCodigo("PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setCliente(cliente);
        p.setTipoPedido(tipo);

        p.setDireccionEntrega(tipo == TipoPedidoCliente.DOMICILIO ? direccionEntrega : null);

        p.setEstado(EstadoPedido.EN_CURSO);
        p.setEstadoCocina(EstadoCocina.PENDIENTE_ACEPTACION);
        p.setEstadoReparto(
                tipo == TipoPedidoCliente.DOMICILIO
                        ? EstadoReparto.PENDIENTE_ASIGNACION
                        : EstadoReparto.NO_APLICA
        );

        return p;
    }

    private static void addLinea(Pedido pedido, Producto producto, int cantidad) {
        if (pedido.getLineaPedidos() == null) {
            pedido.setLineaPedidos(new LinkedHashSet<>());
        }
        pedido.getLineaPedidos().add(new LineaPedido(pedido, producto, cantidad));
    }

    private static void crearPago(PagoRepository pagoRepo,
                                  PedidoCalculoService calculoService,
                                  Pedido pedido,
                                  MetodoPago metodo,
                                  EstadoPago estado) {
        BigDecimal total = calculoService.calcularTotalPedido(pedido);
        Pago pago = new Pago(pedido, metodo, total);

        if (estado == EstadoPago.CONFIRMADO) {
            pago.confirmar("DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else if (estado == EstadoPago.FALLIDO) {
            pago.fallar("Pago fallido (demo)");
        }
        pagoRepo.save(pago);
    }

    // RECETAS (ProductoIngrediente)

    private static void ensureRecetaList(Producto p) {
        if (p.getIngredientes() == null) {
            p.setIngredientes(new ArrayList<>());
        }
    }

    private static void addNormal(Producto p, Ingrediente i) {
        if (p == null || i == null) return;
        ensureRecetaList(p);
        p.getIngredientes().add(new ProductoIngrediente(p, i, true, true, BigDecimal.ZERO));
    }

    private static void addExtra(Producto p, Ingrediente i, String plus) {
        if (p == null || i == null) return;
        ensureRecetaList(p);
        p.getIngredientes().add(new ProductoIngrediente(p, i, false, true, new BigDecimal(plus)));
    }

    private static void addObligatorio(Producto p, Ingrediente i) {
        if (p == null || i == null) return;
        ensureRecetaList(p);
        p.getIngredientes().add(new ProductoIngrediente(p, i, true, false, BigDecimal.ZERO));
    }

    private static void recetaBurgerBase(Producto p, Map<String, Ingrediente> ing) {
        addObligatorio(p, ing.get(PAN_BRIOCHE));
        addObligatorio(p, ing.get(CARNE_DE_TERNERA));

        addNormal(p, ing.get(QUESO_CHEDDAR));
        addNormal(p, ing.get(LECHUGA));
        addNormal(p, ing.get(TOMATE));
        addNormal(p, ing.get(CEBOLLA));
        addNormal(p, ing.get(PEPINILLO));

        addExtra(p, ing.get(EXTRA_QUESO), "0.80");
        addExtra(p, ing.get(EXTRA_BACON), "1.20");
        addExtra(p, ing.get(EXTRA_JALAPENOS), "0.50");
    }

    private static void recetaBurgerVeggie(Producto p, Map<String, Ingrediente> ing) {
        addObligatorio(p, ing.get(PAN_BRIOCHE));
        addNormal(p, ing.get(TOMATE));
        addNormal(p, ing.get(LECHUGA));
        addNormal(p, ing.get(CEBOLLA));
        addNormal(p, ing.get(PEPINILLO));

        addExtra(p, ing.get(EXTRA_QUESO), "0.80");
        addExtra(p, ing.get(EXTRA_JALAPENOS), "0.50");
    }

    private static void recetaPizzaBase(Producto p, Map<String, Ingrediente> ing) {
        addObligatorio(p, ing.get(TOMATE_PIZZA));
        addObligatorio(p, ing.get(MOZZARELLA));

        addNormal(p, ing.get(CEBOLLA));
        addNormal(p, ing.get(CHAMPINONES));
        addNormal(p, ing.get(ACEITUNAS));

        addExtra(p, ing.get(EXTRA_QUESO), "0.80");
        addExtra(p, ing.get(EXTRA_PEPPERONI), "0.90");
        addExtra(p, ing.get(EXTRA_CHAMPINONES), "0.70");
    }

    private static void setOpcionalPorDefecto(Producto p, Ingrediente i, boolean porDefecto) {
        if (p == null || i == null || p.getIngredientes() == null) return;
        for (ProductoIngrediente pi : p.getIngredientes()) {
            if (pi.getIngrediente() != null
                    && pi.getIngrediente().getId() != null
                    && i.getId() != null
                    && pi.getIngrediente().getId().equals(i.getId())) {
                pi.setOpcional(true);
                pi.setPorDefecto(porDefecto);
            }
        }
    }

    private static void inicializarFeatureDatos(FeatureDatosRepository featureDatosRepository) {

        SecureRandom random = new SecureRandom();

        for (Feature feature : Feature.values()) {

            featureDatosRepository.findByFeature(feature)
                    .ifPresentOrElse(
                            fd -> {
                                // ya existe → no hacemos nada
                            },
                            () -> featureDatosRepository.save(
                                    new FeatureDatos(
                                            feature,
                                            new BigDecimal("100.00"),
                                            generarCodigoFeature(random)
                                    )
                            )
                    );
        }
    }

    private static String generarCodigoFeature(SecureRandom random) {
        int num = random.nextInt(100000);
        return String.format("%05d", num);
    }

}