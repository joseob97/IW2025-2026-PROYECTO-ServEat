package com.serveat.service.pedido.impl;

import com.serveat.domain.menu.Producto;
import com.serveat.domain.menu.ProductoIngrediente;
import com.serveat.domain.pedido.LineaPedido;
import com.serveat.domain.pedido.LineaPedidoIngrediente;
import com.serveat.domain.pedido.Pedido;
import com.serveat.repository.menu.ProductoRepository;
import com.serveat.repository.pedido.PedidoRepository;
import com.serveat.service.pedido.PedidoCarritoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PedidoCarritoServiceImpl implements PedidoCarritoService {

    private final ProductoRepository productoRepo;
    private final PedidoRepository pedidoRepo;

    public PedidoCarritoServiceImpl(ProductoRepository productoRepo,
                                    PedidoRepository pedidoRepo) {
        this.productoRepo = productoRepo;
        this.pedidoRepo = pedidoRepo;
    }

    @Override
    public Pedido agregarProducto(Pedido carrito, Producto producto, int cantidad) {
        validarCarrito(carrito);
        if (producto == null) throw new IllegalArgumentException("Producto inválido");
        if (cantidad <= 0) throw new IllegalArgumentException("Cantidad inválida");

        // ✅ CAMBIO: carrito.getLineaPedidos() ahora es Set, pero stream() funciona igual
        // Agrupa SOLO líneas SIN personalización
        LineaPedido existente = carrito.getLineaPedidos().stream()
                .filter(lp -> lp.getProducto() != null
                        && lp.getProducto().getCodigo() != null
                        && lp.getProducto().getCodigo().equals(producto.getCodigo())
                        && (lp.getIngredientes() == null || lp.getIngredientes().isEmpty()))
                .findFirst()
                .orElse(null);

        if (existente != null) {
            existente.setCantidad(existente.getCantidad() + cantidad);
            return carrito;
        }

        carrito.getLineaPedidos().add(new LineaPedido(carrito, producto, cantidad));
        return carrito;
    }

    @Override
    public Pedido agregarProductoPersonalizado(
            Pedido carrito,
            String codigoProducto,
            int cantidad,
            Map<UUID, Boolean> incluidoPorIngrediente,
            Map<UUID, Integer> extraPorIngrediente
    ) {
        validarCarrito(carrito);
        if (codigoProducto == null || codigoProducto.isBlank()) throw new IllegalArgumentException("Producto inválido");
        if (cantidad <= 0) throw new IllegalArgumentException("Cantidad inválida");

        Producto producto = productoRepo.findWithIngredientesByCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));

        // Si no hay receta, se comporta como añadir normal
        if (producto.getIngredientes() == null || producto.getIngredientes().isEmpty()) {
            return agregarProducto(carrito, producto, cantidad);
        }

        List<LineaPedidoIngrediente> seleccion = construirSeleccionSnapshot(
                producto.getIngredientes(),
                incluidoPorIngrediente,
                extraPorIngrediente
        );

        // ✅ CAMBIO: mismaPersonalizacion acepta Collection (Set o List)
        // Intenta agrupar SOLO si misma personalización
        LineaPedido candidata = carrito.getLineaPedidos().stream()
                .filter(lp -> lp.getProducto() != null
                        && codigoProducto.equals(lp.getProducto().getCodigo())
                        && mismaPersonalizacion(lp.getIngredientes(), seleccion))
                .findFirst()
                .orElse(null);

        if (candidata != null) {
            candidata.setCantidad(candidata.getCantidad() + cantidad);
            return carrito;
        }

        // Crear nueva línea personalizada
        LineaPedido nueva = new LineaPedido(carrito, producto, cantidad);

        // ✅ CAMBIO: nueva.getIngredientes() es Set, add() igual funciona
        for (LineaPedidoIngrediente sel : seleccion) {
            nueva.getIngredientes().add(new LineaPedidoIngrediente(
                    nueva,
                    sel.getIngrediente(),
                    sel.isIncluido(),
                    sel.getExtraCantidad(),
                    sel.getPrecioExtra()
            ));
        }

        carrito.getLineaPedidos().add(nueva);
        return carrito;
    }

    @Override
    public Pedido actualizarCantidadLinea(Pedido carrito, String codigoLinea, int nuevaCantidad) {
        validarCarrito(carrito);
        if (codigoLinea == null || codigoLinea.isBlank()) throw new IllegalArgumentException("Línea inválida");

        LineaPedido lp = carrito.getLineaPedidos().stream()
                .filter(l -> codigoLinea.equals(l.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Línea no encontrada"));

        if (nuevaCantidad <= 0) {
            carrito.getLineaPedidos().remove(lp);
            return carrito;
        }

        lp.setCantidad(nuevaCantidad);
        return carrito;
    }

    @Override
    public Pedido eliminarLinea(Pedido carrito, String codigoLinea) {
        validarCarrito(carrito);
        if (codigoLinea == null || codigoLinea.isBlank()) throw new IllegalArgumentException("Línea inválida");

        boolean removed = carrito.getLineaPedidos().removeIf(l -> codigoLinea.equals(l.getCodigo()));
        if (!removed) throw new IllegalArgumentException("Línea no encontrada");

        return carrito;
    }

    @Override
    public void volcarCarritoEnPedido(String codigoPedido, Pedido carrito) {
        if (codigoPedido == null || codigoPedido.isBlank()) throw new IllegalArgumentException("Código pedido inválido");

        // ✅ CAMBIO: lineaPedidos ahora Set -> isEmpty() ok
        if (carrito == null || carrito.getLineaPedidos() == null || carrito.getLineaPedidos().isEmpty()) {
            throw new IllegalArgumentException("El pedido no puede estar vacío");
        }

        Pedido pedido = pedidoRepo.findWithDetalleByCodigo(codigoPedido)
                .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));

        // ✅ sigue igual (Set clear)
        pedido.getLineaPedidos().clear();

        // ✅ CAMBIO: iteras sobre Set, ok
        for (LineaPedido lp : carrito.getLineaPedidos()) {
            if (lp.getProducto() == null || lp.getProducto().getCodigo() == null) continue;

            Producto producto = productoRepo.findByCodigo(lp.getProducto().getCodigo())
                    .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + lp.getProducto().getCodigo()));

            LineaPedido nueva = new LineaPedido(pedido, producto, lp.getCantidad());

            // ✅ CAMBIO: lp.getIngredientes() ahora Set -> sigue ok
            if (lp.getIngredientes() != null && !lp.getIngredientes().isEmpty()) {
                for (LineaPedidoIngrediente sel : lp.getIngredientes()) {
                    if (sel == null || sel.getIngrediente() == null) continue;

                    nueva.getIngredientes().add(new LineaPedidoIngrediente(
                            nueva,
                            sel.getIngrediente(),
                            sel.isIncluido(),
                            sel.getExtraCantidad(),
                            sel.getPrecioExtra()
                    ));
                }
            }

            pedido.getLineaPedidos().add(nueva);
        }

        pedidoRepo.save(pedido);
    }

    // ----------------- helpers -----------------

    private void validarCarrito(Pedido carrito) {
        if (carrito == null) throw new IllegalArgumentException("Carrito inválido");

        // ✅ CAMBIO: ahora es Set; antes era new ArrayList<>()
        if (carrito.getLineaPedidos() == null) {
            carrito.setLineaPedidos(new LinkedHashSet<>()); // <-- clave
        }
    }

    private List<LineaPedidoIngrediente> construirSeleccionSnapshot(
            List<ProductoIngrediente> receta,
            Map<UUID, Boolean> incluidoPorIngrediente,
            Map<UUID, Integer> extraPorIngrediente
    ) {
        List<LineaPedidoIngrediente> seleccion = new ArrayList<>();

        Map<UUID, ProductoIngrediente> byId = receta.stream()
                .filter(pi -> pi.getIngrediente() != null && pi.getIngrediente().getId() != null)
                .collect(Collectors.toMap(pi -> pi.getIngrediente().getId(), pi -> pi, (a, b) -> a));

        for (var entry : byId.entrySet()) {
            UUID ingId = entry.getKey();
            ProductoIngrediente pi = entry.getValue();

            boolean incluido = (incluidoPorIngrediente != null && incluidoPorIngrediente.containsKey(ingId))
                    ? Boolean.TRUE.equals(incluidoPorIngrediente.get(ingId))
                    : pi.isPorDefecto();

            int extra = (extraPorIngrediente != null && extraPorIngrediente.containsKey(ingId))
                    ? Math.max(extraPorIngrediente.get(ingId), 0)
                    : 0;

            // no opcional -> bloqueado
            if (!pi.isOpcional()) {
                incluido = pi.isPorDefecto();
                extra = 0;
            }

            seleccion.add(new LineaPedidoIngrediente(
                    null,
                    pi.getIngrediente(),
                    incluido,
                    extra,
                    pi.getPrecioExtra()
            ));
        }

        // orden estable
        seleccion.sort(Comparator.comparing(li -> li.getIngrediente().getId().toString()));
        return seleccion;
    }

    /**
     * OPCIÓN A:
     * - Acepta Collection para que funcione con Set o List.
     * - Compara por ingredienteId: incluido + extraCantidad + precioExtra.
     */
    private boolean mismaPersonalizacion(Collection<LineaPedidoIngrediente> a,
                                         Collection<LineaPedidoIngrediente> b) {

        if (a == null) a = List.of();
        if (b == null) b = List.of();
        if (a.size() != b.size()) return false;

        Map<UUID, String> ma = a.stream()
                .filter(x -> x != null && x.getIngrediente() != null && x.getIngrediente().getId() != null)
                .collect(Collectors.toMap(
                        x -> x.getIngrediente().getId(),
                        x -> x.isIncluido() + "|" + Math.max(x.getExtraCantidad(), 0) + "|" +
                                (x.getPrecioExtra() == null ? "0" : x.getPrecioExtra().toPlainString()),
                        (x1, x2) -> x1
                ));

        Map<UUID, String> mb = b.stream()
                .filter(x -> x != null && x.getIngrediente() != null && x.getIngrediente().getId() != null)
                .collect(Collectors.toMap(
                        x -> x.getIngrediente().getId(),
                        x -> x.isIncluido() + "|" + Math.max(x.getExtraCantidad(), 0) + "|" +
                                (x.getPrecioExtra() == null ? "0" : x.getPrecioExtra().toPlainString()),
                        (x1, x2) -> x1
                ));

        return ma.equals(mb);
    }
}