package cl.gamehub.despachos.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.EventoEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.OrdenEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.UsuarioEjemplo;
import cl.gamehub.despachos.model.Despacho;
import cl.gamehub.despachos.model.EstadoDespacho;
import cl.gamehub.despachos.repository.DespachoRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga un despacho por cada orden de ejemplo que tiene seguimiento (13 de las 18) si la base está vacía.
 * Estados, fechas y eventos salen de DatosDeEjemplo, así calzan con las órdenes que carga order-service.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final DespachoRepository despachos;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, DespachoRepository despachos) {
        this.habilitado = habilitado;
        this.despachos = despachos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || despachos.count() > 0) {
            return;
        }
        for (OrdenEjemplo o : DatosDeEjemplo.ordenes(DatosDeEjemplo.base())) {
            if (o.seguimiento() == null) {
                continue;
            }
            List<EventoEjemplo> eventos = o.eventos();
            UsuarioEjemplo cliente = o.cliente();
            // El constructor deja el primer evento (PREPARANDO); creadoEn es la fecha de ese evento
            Despacho despacho = new Despacho(o.id(), o.numero(), o.seguimiento(), o.fechaEstimada(), cliente.nombre(),
                    cliente.direccion(), cliente.comuna(), cliente.region(), eventos.getFirst().fecha());
            // Los demás eventos avanzan el despacho hasta despachoHasta; ENTREGADO fija entregadoEn
            for (EventoEjemplo evento : eventos.subList(1, eventos.size())) {
                despacho.avanzar(EstadoDespacho.valueOf(evento.estado()), evento.fecha());
            }
            despachos.save(despacho);
        }
        log.info("Datos de ejemplo cargados: {} despachos.", despachos.count());
    }
}
