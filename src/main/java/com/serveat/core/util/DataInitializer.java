package com.serveat.core.util;

import com.serveat.domain.menu.Categoria;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.*;
import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.menu.CategoriaRepository;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.repository.seguridad.FeatureActivaRepository;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.repository.usuario.EmpleadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Profile("dev")
@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(EmpleadoRepository empleadoRepository,
                                   ClienteRepository clienteRepository,
                                   CategoriaRepository categoriaRepository,
                                   ProductoRepository productoRepository,
                                   FeatureActivaRepository featureActivaRepository,
                                   PedidoRepository pedidoRepository,
                                   PagoRepository pagoRepository) {

        return args -> {

            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            String rawPassword = System.getenv("DEMO_PASSWORD");

            if (rawPassword == null || rawPassword.isBlank()) {
                throw new IllegalStateException("La variable de entorno DEMO_PASSWORD no está definida");
            }

            String pass = encoder.encode(rawPassword);

            // EMPLEADOS
            insertarEmpleados(empleadoRepository, pass);

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
                cliente.setRol("CLIENTE");
                clienteRepository.save(cliente);

                Cliente cliente2 = new Cliente();
                cliente2.setNombre("Cliente Test");
                cliente2.setEmail("cliente2@demo.com");
                cliente2.setDireccion("Dirección cliente test");
                cliente2.setTelefono("600123456");
                cliente2.setUsername("cliente2");
                cliente2.setPassword(pass);
                cliente2.setRol("CLIENTE");
                clienteRepository.save(cliente2);

                Cliente cliente3 = new Cliente();
                cliente3.setNombre("Cliente Prueba");
                cliente3.setEmail("prueba@prueba.com");
                cliente3.setDireccion("Dirección cliente prueba");
                cliente3.setTelefono("123654987");
                cliente3.setUsername("prueba");
                cliente3.setPassword(pass);
                cliente3.setRol("CLIENTE");
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

            // PRODUCTOS
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

                log.info("25 productos iniciales creados.");
            }

            // PEDIDOS + PAGOS
            if (pedidoRepository.count() == 0) {
                log.info("Insertando pedidos de demo (recoger + domicilio) ...");

                Cliente cliente = clienteRepository.findByUsername("cliente1")
                        .orElseThrow(() -> new IllegalStateException("cliente1 no existe"));

                Empleado repartidor = empleadoRepository.findByUsername("repartidor1")
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
                recoger2.marcarModificado("cocinero1");
                addLinea(recoger2, pizz1, 1);

                Pedido recoger3 = nuevoPedido(cliente, TipoPedidoCliente.RECOGER, null);
                recoger3.setEstado(EstadoPedido.EN_COCINA);
                recoger3.setEstadoCocina(EstadoCocina.LISTO);
                recoger3.setEstadoReparto(EstadoReparto.NO_APLICA);
                recoger3.marcarModificado("cocinero1");
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
                dom2.marcarModificado("cocinero1");
                addLinea(dom2, burg1, 1);
                addLinea(dom2, beb1, 1);

                Pedido dom3 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "C/ Mayor 9");
                dom3.setEstado(EstadoPedido.EN_COCINA);
                dom3.setEstadoCocina(EstadoCocina.LISTO);
                dom3.setEstadoReparto(EstadoReparto.ASIGNADO);
                dom3.setRepartidor(repartidor);
                dom3.setFechaAsignacionReparto(LocalDateTime.now().minusMinutes(15));
                dom3.marcarModificado("repartidor1");
                addLinea(dom3, pizz1, 2);

                Pedido dom4 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "Plaza España 1");
                dom4.setEstado(EstadoPedido.EN_COCINA);
                dom4.setEstadoCocina(EstadoCocina.LISTO);
                dom4.setEstadoReparto(EstadoReparto.EN_REPARTO);
                dom4.setRepartidor(repartidor);
                dom4.setFechaAsignacionReparto(LocalDateTime.now().minusMinutes(30));
                dom4.setFechaSalidaReparto(LocalDateTime.now().minusMinutes(10));
                dom4.marcarModificado("repartidor1");
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
                dom5.marcarModificado("repartidor1");
                addLinea(dom5, pizz1, 1);

                Pedido dom6 = nuevoPedido(cliente, TipoPedidoCliente.DOMICILIO, "C/ Luna 5");
                dom6.setEstado(EstadoPedido.EN_COCINA);
                dom6.setEstadoCocina(EstadoCocina.LISTO);
                dom6.setEstadoReparto(EstadoReparto.INCIDENCIA);
                dom6.setRepartidor(repartidor);
                dom6.setIncidenciaReparto("No hay nadie en casa");
                dom6.setFechaAsignacionReparto(LocalDateTime.now().minusHours(2));
                dom6.marcarModificado("repartidor1");
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

                // aseguramos IDs antes de crear pagos
                pedidoRepository.flush();

                crearPago(pagoRepository, dom2, MetodoPago.TARJETA, EstadoPago.CONFIRMADO);
                crearPago(pagoRepository, dom3, MetodoPago.PAYPAL, EstadoPago.CONFIRMADO);
                crearPago(pagoRepository, dom4, MetodoPago.TARJETA, EstadoPago.CONFIRMADO);
                crearPago(pagoRepository, dom5, MetodoPago.EFECTIVO, EstadoPago.CONFIRMADO);

                crearPago(pagoRepository, dom1, MetodoPago.TARJETA, EstadoPago.PENDIENTE);
                crearPago(pagoRepository, dom6, MetodoPago.PAYPAL, EstadoPago.FALLIDO);

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
                    "Cocinero Demo", "cocinero1", pass, "712345678",
                    "cocinero@gmail.com", "Calle del cocinero nº25", "COCINERO", true
            ));

            empleadoRepository.save(new Empleado(
                    "Repartidor Demo", "repartidor1", pass, "798765432",
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

    private static Pedido nuevoPedido(Cliente cliente, TipoPedidoCliente tipo, String direccionEntrega) {
        Pedido p = new Pedido();
        p.setCodigo("PED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        p.setCliente(cliente);
        p.setTipoPedido(tipo);

        //  Solo domicilio lleva dirección
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
        pedido.getLineaPedidos().add(new LineaPedido(pedido, producto, cantidad));
    }

    private static void crearPago(PagoRepository pagoRepo, Pedido pedido, MetodoPago metodo, EstadoPago estado) {
        BigDecimal total = pedido.calcularPrecioTotal();
        Pago pago = new Pago(pedido, metodo, total);

        if (estado == EstadoPago.CONFIRMADO) {
            pago.confirmar("DEMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        } else if (estado == EstadoPago.FALLIDO) {
            pago.fallar("Pago fallido (demo)");
        }
        pagoRepo.save(pago);
    }
}