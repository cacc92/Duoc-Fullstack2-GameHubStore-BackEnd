package cl.gamehub.pagos.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.OrdenEjemplo;
import cl.gamehub.pagos.model.EstadoPago;
import cl.gamehub.pagos.model.MetodoPago;
import cl.gamehub.pagos.model.Pago;
import cl.gamehub.pagos.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga el pago de cada una de las 18 órdenes de ejemplo si la base está vacía. Montos, estados y fechas
 * salen de DatosDeEjemplo, así calzan con las órdenes que carga order-service.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final PagoRepository pagos;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, PagoRepository pagos) {
        this.habilitado = habilitado;
        this.pagos = pagos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || pagos.count() > 0) {
            return;
        }
        for (OrdenEjemplo o : DatosDeEjemplo.ordenes(DatosDeEjemplo.base())) {
            EstadoPago estado = EstadoPago.valueOf(o.estadoPago());
            Pago pago = new Pago(o.id(), o.numero(), o.usuarioId(), MetodoPago.valueOf(o.metodoPago()), o.total(),
                    estado, o.detallePago(), o.creadaEn());
            // fecha es el último cambio: una transferencia aprobada se confirmó horas después de comprar
            pago.cambiar(estado, o.detallePago(), o.fechaPago());
            pagos.save(pago);
        }
        log.info("Datos de ejemplo cargados: {} pagos.", pagos.count());
    }
}
