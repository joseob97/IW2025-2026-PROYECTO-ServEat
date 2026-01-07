package com.serveat.core.util;

import com.serveat.domain.menu.Ingrediente;
import com.serveat.domain.menu.Producto;
import com.serveat.domain.pago.EstadoPago;
import com.serveat.domain.pago.MetodoPago;
import com.serveat.domain.pago.Pago;
import com.serveat.domain.pedido.*;
import com.serveat.domain.usuario.Cliente;
import com.serveat.domain.usuario.Empleado;
import com.serveat.repository.menu.IngredienteRepository;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pago.PagoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.repository.usuario.ClienteRepository;
import com.serveat.repository.usuario.EmpleadoRepository;
import com.serveat.service.pedido.PedidoCalculoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

@Profile("seed2025")
@Configuration
public class DataInitializerPedidos2025 {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerPedidos2025.class);

    // Reutiliza constantes del DataInitializer (son public static final)
    private static final String EXTRA_QUESO = DataInitializer.EXTRA_QUESO;
    private static final String EXTRA_BACON = DataInitializer.EXTRA_BACON;
    private static final String EXTRA_JALAPENOS = DataInitializer.EXTRA_JALAPENOS;

    private static final String REPARTIDOR_1 = DataInitializer.REPARTIDOR_1;
    private static final String COCINERO_1 = DataInitializer.COCINERO_1;

    @Bean
    CommandLineRunner seedPedidos2025(EmpleadoRepository empleadoRepository,
                                      ClienteRepository clienteRepository,
                                      ProductoRepository productoRepository,
                                      IngredienteRepository ingredienteRepository,
                                      PedidoRepository pedidoRepository,
                                      PagoRepository pagoRepository,
                                      PedidoCalculoService pedidoCalculoService) {

        return args -> insertarPedidosFinalizados2025(
                empleadoRepository,
                clienteRepository,
                productoRepository,
                ingredienteRepository,
                pedidoRepository,
                pagoRepository,
                pedidoCalculoService
        );
    }

    private static void insertarPedidosFinalizados2025(EmpleadoRepository empleadoRepository,
                                                       ClienteRepository clienteRepository,
                                                       ProductoRepository productoRepository,
                                                       IngredienteRepository ingredienteRepository,
                                                       PedidoRepository pedidoRepository,
                                                       PagoRepository pagoRepository,
                                                       PedidoCalculoService pedidoCalculoService) {

        Cliente cliente = clienteRepository.findByUsername("cliente1")
                .orElseThrow(() -> new IllegalStateException("cliente1 no existe"));

        Empleado repartidor = empleadoRepository.findByUsername(REPARTIDOR_1)
                .orElseThrow(() -> new IllegalStateException("repartidor1 no existe"));

        Producto burg = productoRepository.findByCodigo("BURG-001")
                .orElseThrow(() -> new IllegalStateException("BURG-001 no existe"));
        Producto pizz = productoRepository.findByCodigo("PIZZ-001")
                .orElseThrow(() -> new IllegalStateException("PIZZ-001 no existe"));
        Producto beb = productoRepository.findByCodigo("BEB-001")
                .orElseThrow(() -> new IllegalStateException("BEB-001 no existe"));

        Ingrediente extraQueso = ingredienteRepository.findAll().stream()
                .filter(i -> EXTRA_QUESO.equals(i.getNombre()))
                .filter(i -> i.getPrecioExtra() != null && i.getPrecioExtra().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe ingrediente EXTRA_QUESO con precioExtra > 0"));

        Ingrediente extraBacon = ingredienteRepository.findAll().stream()
                .filter(i -> EXTRA_BACON.equals(i.getNombre()))
                .filter(i -> i.getPrecioExtra() != null && i.getPrecioExtra().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe ingrediente EXTRA_BACON con precioExtra > 0"));

        Ingrediente extraJal = ingredienteRepository.findAll().stream()
                .filter(i -> EXTRA_JALAPENOS.equals(i.getNombre()))
                .filter(i -> i.getPrecioExtra() != null && i.getPrecioExtra().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No existe ingrediente EXTRA_JALAPENOS con precioExtra > 0"));

        List<Ingrediente> extras = List.of(extraQueso, extraBacon, extraJal);

        int creados = 0;

        for (int mes = 1; mes <= 12; mes++) {
            for (int n = 1; n <= 10; n++) {

                String codigo = String.format("PED-2025-%02d-%02d", mes, n);

                if (pedidoRepository.findByCodigo(codigo).isPresent()) continue;

                boolean domicilio = (n % 2 == 0);
                LocalDateTime fecha = LocalDateTime.of(
                        2025,
                        mes,
                        Math.min(25, 2 + n),
                        12 + (n % 8),
                        (n * 5) % 60
                );

                Pedido p = new Pedido();
                p.setCodigo(codigo);
                p.setCliente(cliente);
                p.setTipoPedido(domicilio ? TipoPedidoCliente.DOMICILIO : TipoPedidoCliente.RECOGER);
                p.setDireccionEntrega(domicilio ? ("Calle Histórica " + mes + " #" + n) : null);

                p.setEstado(EstadoPedido.EN_CURSO);
                p.setEstadoCocina(EstadoCocina.LISTO);

                if (domicilio) {
                    p.setEstadoReparto(EstadoReparto.ENTREGADO);
                    p.setRepartidor(repartidor);

                    p.setFechaAsignacionReparto(fecha.plusMinutes(10));
                    p.setFechaSalidaReparto(fecha.plusMinutes(25));
                    p.setFechaEntrega(fecha.plusMinutes(55));

                    p.setModificadoPor(REPARTIDOR_1);
                    p.setFechaUltimaModificacion(fecha.plusMinutes(55));
                } else {
                    p.setEstadoReparto(EstadoReparto.NO_APLICA);
                    p.setModificadoPor(COCINERO_1);
                    p.setFechaUltimaModificacion(fecha.plusMinutes(35));
                }

                p.setFechaCreacion(fecha);

                if (p.getLineaPedidos() == null) p.setLineaPedidos(new LinkedHashSet<>());
                p.getLineaPedidos().add(new LineaPedido(p, burg, 1 + (n % 2))); // 1 o 2
                p.getLineaPedidos().add(new LineaPedido(p, beb, 1));
                if (n % 3 == 0) p.getLineaPedidos().add(new LineaPedido(p, pizz, 1));

                if (n % 5 == 0 || n % 4 == 0) {
                    LineaPedido primera = p.getLineaPedidos().iterator().next();

                    Ingrediente elegido = extras.get((mes + n) % extras.size());
                    int extraCant = 1 + ((mes + n) % 2);

                    LineaPedidoIngrediente lpi = new LineaPedidoIngrediente(
                            primera,
                            elegido,
                            true,
                            extraCant,
                            elegido.getPrecioExtra()
                    );

                    primera.getIngredientes().add(lpi);
                }

                pedidoRepository.save(p);

                MetodoPago metodo = switch ((mes + n) % 3) {
                    case 0 -> MetodoPago.TARJETA;
                    case 1 -> MetodoPago.PAYPAL;
                    default -> MetodoPago.EFECTIVO;
                };

                crearPago(pagoRepository, pedidoCalculoService, p, metodo, EstadoPago.CONFIRMADO);

                creados++;
            }
        }

        if (creados > 0) pedidoRepository.flush();

        log.info("✅ Seed pedidos históricos 2025: creados {}", creados);
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
}