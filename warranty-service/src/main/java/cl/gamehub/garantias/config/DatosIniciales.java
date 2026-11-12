package cl.gamehub.garantias.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.garantias.model.EstadoGarantia;
import cl.gamehub.garantias.model.Garantia;
import cl.gamehub.garantias.model.MotivoGarantia;
import cl.gamehub.garantias.repository.GarantiaRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga las 3 garantías de ejemplo si la base está vacía (GAR-0001 a GAR-0003): una abierta, una
 * resuelta y una en revisión. El número de orden, el cliente y el plazo salen de DatosDeEjemplo, así
 * calzan con lo que tiene order-service.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final GarantiaRepository garantias;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, GarantiaRepository garantias) {
        this.habilitado = habilitado;
        this.garantias = garantias;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || garantias.count() > 0) {
            return;
        }
        LocalDateTime base = DatosDeEjemplo.base();

        // Martina: los audífonos de la orden 10 cortan el audio; la solicitud recién llega al operador
        garantia(base, 10, 6, MotivoGarantia.FALLA,
                "Se corta el audio del lado izquierdo desde la semana pasada, incluso con otro cable.",
                base.minusDays(2), EstadoGarantia.ABIERTA, null);
        // Sofía: el monitor se cambió por uno nuevo (después lo reseñó con 5 estrellas en review-service)
        garantia(base, 14, 7, MotivoGarantia.FALLA, "La pantalla parpadea al usar 170 Hz con el cable incluido.",
                base.minusDays(30), EstadoGarantia.RESUELTA,
                "Se reemplazó por una unidad nueva y se envió un cable DisplayPort certificado.");
        // Diego: el operador pidió más antecedentes antes de decidir
        garantia(base, 15, 4, MotivoGarantia.FALLA, "La barra espaciadora se traba después de unas horas de uso.",
                base.minusDays(4), EstadoGarantia.EN_REVISION, "Se pidió un video de la falla para evaluar el cambio.");

        log.info("Datos de ejemplo cargados: {} garantías.", garantias.count());
    }

    /** Las que no están abiertas se actualizaron un día después de creadas. */
    private void garantia(LocalDateTime base, long ordenId, long productoId, MotivoGarantia motivo, String descripcion,
                          LocalDateTime creadaEn, EstadoGarantia estado, String resolucion) {
        DatosDeEjemplo.OrdenEjemplo orden = DatosDeEjemplo.orden(base, ordenId);
        Garantia garantia = new Garantia(ordenId, orden.numero(), orden.usuarioId(), orden.cliente().nombre(), productoId,
                DatosDeEjemplo.producto(productoId).nombre(), motivo, descripcion, Garantia.plazoDesde(orden.entregadaEn()),
                creadaEn);
        if (estado != EstadoGarantia.ABIERTA) {
            garantia.actualizar(estado, resolucion, creadaEn.plusDays(1));
        }
        garantias.save(garantia).asignarCodigo();
    }
}
