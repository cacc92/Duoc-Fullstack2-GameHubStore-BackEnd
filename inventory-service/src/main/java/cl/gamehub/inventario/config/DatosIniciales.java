package cl.gamehub.inventario.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.inventario.model.Movimiento;
import cl.gamehub.inventario.model.Stock;
import cl.gamehub.inventario.model.TipoMovimiento;
import cl.gamehub.inventario.repository.MovimientoRepository;
import cl.gamehub.inventario.repository.StockRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Si la base está vacía, carga el stock de los 13 productos de ejemplo (con el mismo id que tienen en
 * product-service) y los 5 movimientos recientes del monolito, con fechas calculadas desde DatosDeEjemplo.base().
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final StockRepository stocks;
    private final MovimientoRepository movimientos;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, StockRepository stocks,
                          MovimientoRepository movimientos) {
        this.habilitado = habilitado;
        this.stocks = stocks;
        this.movimientos = movimientos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || stocks.count() > 0) {
            return;
        }
        DatosDeEjemplo.PRODUCTOS.forEach(p -> stocks.save(new Stock(p.id(), p.sku(), p.nombre(), p.activo(), p.stock())));
        LocalDateTime base = DatosDeEjemplo.base();
        movimiento("SSD-TEC-FLX1", TipoMovimiento.INGRESO, 20, "Recepción de proveedor", "Luis Soto",
                base.minusDays(1).withHour(17).withMinute(30));
        movimiento("HS-AND-P71", TipoMovimiento.SALIDA, -1, "Merma: caja dañada en bodega", "Ana Torres",
                base.minusDays(1).withHour(12).withMinute(10));
        movimiento("CON-AUS-GO", TipoMovimiento.SALIDA, -1, "Venta en tienda", "Luis Soto",
                base.minusDays(2).withHour(16).withMinute(45));
        movimiento("KB-NIM-TKL", TipoMovimiento.INGRESO, 12, "Recepción de proveedor", "Luis Soto",
                base.minusDays(2).withHour(9).withMinute(20));
        movimiento("MS-NIM-AERO", TipoMovimiento.SALIDA, -1, "Venta en tienda", "Luis Soto", base.minusHours(5));
        log.info("Datos de ejemplo cargados: stock de {} productos y {} movimientos.", stocks.count(), movimientos.count());
    }

    /** Como en el monolito, stockResultante es el stock actual del producto. */
    private void movimiento(String sku, TipoMovimiento tipo, int cantidad, String motivo, String usuario,
                            LocalDateTime fecha) {
        long productoId = DatosDeEjemplo.PRODUCTOS.stream().filter(p -> p.sku().equals(sku)).findFirst().orElseThrow().id();
        Stock stock = stocks.findById(productoId).orElseThrow();
        movimientos.save(new Movimiento(stock, tipo, cantidad, motivo, usuario, fecha));
    }
}
