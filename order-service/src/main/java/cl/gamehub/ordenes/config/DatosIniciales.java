package cl.gamehub.ordenes.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.OrdenEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.ProductoEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.UsuarioEjemplo;
import cl.gamehub.ordenes.model.DatosDespacho;
import cl.gamehub.ordenes.model.EstadoOrden;
import cl.gamehub.ordenes.model.MetodoPago;
import cl.gamehub.ordenes.model.Orden;
import cl.gamehub.ordenes.repository.OrdenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga las 18 órdenes de ejemplo si la base está vacía (id 1 a 18). Sus pagos y despachos los cargan
 * payment-service y shipping-service desde los mismos datos (DatosDeEjemplo), así todo calza.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final OrdenRepository ordenes;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, OrdenRepository ordenes) {
        this.habilitado = habilitado;
        this.ordenes = ordenes;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || ordenes.count() > 0) {
            return;
        }
        for (OrdenEjemplo ejemplo : DatosDeEjemplo.ordenes(DatosDeEjemplo.base())) {
            UsuarioEjemplo c = ejemplo.cliente();
            Orden orden = new Orden(c.id(), c.nombre(), new DatosDespacho(c.nombre(), c.email(), c.telefono(), c.region(),
                    c.comuna(), c.direccion(), null, null), MetodoPago.valueOf(ejemplo.metodoPago()), ejemplo.creadaEn());
            ejemplo.lineas().forEach(l -> {
                ProductoEjemplo p = DatosDeEjemplo.producto(l.productoId());
                orden.agregarLinea(p.id(), p.sku(), p.nombre(), p.imagen(), p.precio(), l.cantidad());
            });
            orden.fijarTotales(ejemplo.subtotal(), ejemplo.descuento(), ejemplo.cupon());
            orden.restaurar(EstadoOrden.valueOf(ejemplo.estadoOrden()), ejemplo.entregadaEn());
            ordenes.save(orden).asignarNumero();
        }
        log.info("Datos de ejemplo cargados: {} órdenes.", ordenes.count());
    }
}
