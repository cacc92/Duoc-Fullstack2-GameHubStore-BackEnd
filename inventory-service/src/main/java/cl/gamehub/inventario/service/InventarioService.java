package cl.gamehub.inventario.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.inventario.dto.DescuentoStockSolicitud;
import cl.gamehub.inventario.dto.DisponibilidadDto;
import cl.gamehub.inventario.dto.LineaDescuento;
import cl.gamehub.inventario.dto.MovimientoDto;
import cl.gamehub.inventario.dto.MovimientoSolicitud;
import cl.gamehub.inventario.dto.ProductoInventarioSolicitud;
import cl.gamehub.inventario.dto.StockDto;
import cl.gamehub.inventario.dto.StockNuevoSolicitud;
import cl.gamehub.inventario.model.Movimiento;
import cl.gamehub.inventario.model.Stock;
import cl.gamehub.inventario.model.TipoMovimiento;
import cl.gamehub.inventario.repository.MovimientoRepository;
import cl.gamehub.inventario.repository.StockRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventarioService {

    /** Usuario que aparece en los movimientos automáticos: stock inicial y ventas. */
    public static final String SISTEMA = "Sistema";

    private static final Logger log = LoggerFactory.getLogger(InventarioService.class);

    private final StockRepository stocks;
    private final MovimientoRepository movimientos;

    public InventarioService(StockRepository stocks, MovimientoRepository movimientos) {
        this.stocks = stocks;
        this.movimientos = movimientos;
    }

    // ---------- Catálogo ----------

    /** Stock disponible de los productos pedidos; productoIds null significa todos. */
    @Transactional(readOnly = true)
    public List<DisponibilidadDto> disponibilidad(List<Long> productoIds) {
        if (productoIds == null) {
            return stocks.findAllByOrderByProductoIdAsc().stream().map(InventarioService::disponibilidadDto).toList();
        }
        if (productoIds.isEmpty()) {
            return List.of();
        }
        return stocks.findByProductoIdInOrderByProductoIdAsc(productoIds).stream()
                .map(InventarioService::disponibilidadDto)
                .toList();
    }

    // ---------- Panel del operador ----------

    @Transactional(readOnly = true)
    public List<StockDto> stock() {
        return stocks.findAllByOrderByProductoIdAsc().stream().map(InventarioService::stockDto).toList();
    }

    @Transactional(readOnly = true)
    public List<MovimientoDto> movimientos() {
        return movimientos.findTop30ByOrderByFechaDescIdDesc().stream().map(InventarioService::movimientoDto).toList();
    }

    /** Ingreso, salida o ajuste por conteo que registra el operador. El stock nunca puede quedar negativo. */
    @Transactional
    public MovimientoDto registrar(MovimientoSolicitud s, String usuario) {
        Stock stock = buscar(s.productoId());
        int cantidad = s.cantidad();
        if (s.tipo() != TipoMovimiento.AJUSTE && cantidad == 0) {
            throw ApiException.regla("La cantidad debe ser mayor que cero.");
        }
        // En un AJUSTE, cantidad es lo que se contó en bodega: el movimiento es la diferencia con el sistema
        int delta = switch (s.tipo()) {
            case INGRESO -> cantidad;
            case SALIDA -> -cantidad;
            case AJUSTE -> cantidad - stock.getCantidad();
        };
        int nuevo = stock.getCantidad() + delta;
        if (nuevo < 0) {
            throw ApiException.regla("La salida deja el stock en " + nuevo + ". El stock no puede quedar negativo.");
        }
        if (delta == 0) {
            throw ApiException.regla("El conteo coincide con el stock actual: no hay nada que ajustar.");
        }
        stock.setCantidad(nuevo);
        Movimiento m = movimientos.save(new Movimiento(stock, s.tipo(), delta, s.motivo().trim(), usuario, ahora()));
        log.info("{} de {} unidades en {} por {}: quedan {}", s.tipo(), delta, stock.getSku(), usuario, nuevo);
        return movimientoDto(m);
    }

    // ---------- Para otros microservicios ----------

    /** product-service crea el registro de stock de cada producto nuevo. */
    @Transactional
    public StockDto crear(StockNuevoSolicitud s) {
        if (stocks.existsById(s.productoId())) {
            throw ApiException.conflicto("Ya existe stock para el producto " + s.productoId() + ".");
        }
        int inicial = s.stockInicial() == null ? 0 : s.stockInicial();
        Stock stock = stocks.save(new Stock(s.productoId(), s.sku().trim(), s.nombre().trim(), s.activo(), inicial));
        if (inicial > 0) {
            movimientos.save(new Movimiento(stock, TipoMovimiento.INGRESO, inicial, "Stock inicial del producto",
                    SISTEMA, ahora()));
        }
        log.info("Stock creado para el producto {} ({}): {} unidades", s.productoId(), stock.getSku(), inicial);
        return stockDto(stock);
    }

    /** product-service avisa que cambiaron el SKU, el nombre o el estado de un producto. */
    @Transactional
    public StockDto actualizarProducto(Long productoId, ProductoInventarioSolicitud s) {
        Stock stock = buscar(productoId);
        stock.actualizarProducto(s.sku().trim(), s.nombre().trim(), s.activo());
        log.info("Producto {} actualizado en inventario: {} ({})", productoId, stock.getSku(),
                stock.isActivo() ? "activo" : "inactivo");
        return stockDto(stock);
    }

    /**
     * order-service descuenta el stock de una compra pagada. Es todo o nada: primero se revisan todas las
     * líneas y, si una no alcanza, no se descuenta ninguna. Después se registra una SALIDA por línea.
     */
    @Transactional
    public List<DisponibilidadDto> descontar(DescuentoStockSolicitud s) {
        Map<Long, Stock> involucrados = new LinkedHashMap<>();
        Map<Long, Integer> pedido = new HashMap<>();
        for (LineaDescuento linea : s.lineas()) {
            Stock stock = involucrados.computeIfAbsent(linea.productoId(), this::buscar);
            // Si el mismo producto viene en dos líneas, se suman antes de comparar con el stock
            int total = pedido.merge(linea.productoId(), linea.cantidad(), Integer::sum);
            if (total > stock.getCantidad()) {
                throw ApiException.conflicto("No hay stock suficiente de " + stock.getNombre() + ".");
            }
        }
        String referencia = s.referencia().trim();
        LocalDateTime fecha = ahora();
        for (LineaDescuento linea : s.lineas()) {
            Stock stock = involucrados.get(linea.productoId());
            stock.setCantidad(stock.getCantidad() - linea.cantidad());
            movimientos.save(new Movimiento(stock, TipoMovimiento.SALIDA, -linea.cantidad(), referencia, SISTEMA, fecha));
        }
        log.info("Stock descontado por «{}»: {} líneas", referencia, s.lineas().size());
        return involucrados.values().stream().map(InventarioService::disponibilidadDto).toList();
    }

    // ---------- Apoyo ----------

    private Stock buscar(Long productoId) {
        return stocks.findById(productoId).orElseThrow(() -> ApiException.noEncontrado("El producto no existe."));
    }

    /** Fecha de los movimientos nuevos, sin fracciones de segundo (en JSON: 2026-11-11T14:05:00). */
    private static LocalDateTime ahora() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    private static DisponibilidadDto disponibilidadDto(Stock s) {
        return new DisponibilidadDto(s.getProductoId(), s.getCantidad());
    }

    private static StockDto stockDto(Stock s) {
        return new StockDto(s.getProductoId(), s.getSku(), s.getNombre(), s.getCantidad(), s.isActivo());
    }

    private static MovimientoDto movimientoDto(Movimiento m) {
        return new MovimientoDto(m.getId(), m.getFecha(), m.getProductoId(), m.getProducto(), m.getTipo(),
                m.getCantidad(), m.getMotivo(), m.getUsuario(), m.getStockResultante());
    }
}
