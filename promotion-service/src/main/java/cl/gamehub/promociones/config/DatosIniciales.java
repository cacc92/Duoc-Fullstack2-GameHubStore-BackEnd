package cl.gamehub.promociones.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.promociones.model.Promocion;
import cl.gamehub.promociones.repository.PromocionRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga los 3 cupones de ejemplo si la base está vacía (sus id quedan del 1 al 3): uno vigente, uno programado
 * y uno vencido y agotado. Las fechas se calculan desde hoy, así GAMER10 sigue vigente cuando se ejecute el proyecto.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final PromocionRepository promociones;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, PromocionRepository promociones) {
        this.habilitado = habilitado;
        this.promociones = promociones;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || promociones.count() > 0) {
            return;
        }
        LocalDate hoy = DatosDeEjemplo.base().toLocalDate();

        // GAMER10 es el cupón de la orden 5 de ejemplo: su porcentaje y su tope salen de DatosDeEjemplo
        Promocion gamer = new Promocion("GAMER10", DatosDeEjemplo.PORCENTAJE_GAMER10, DatosDeEjemplo.TOPE_GAMER10,
                hoy.minusDays(9), hoy.plusDays(20), 200);
        gamer.setUsos(34);
        promociones.save(gamer);

        promociones.save(new Promocion("SETUP15", 15, 60_000, hoy.plusDays(5), hoy.plusDays(35), 100));

        Promocion cyber = new Promocion("CYBER25", 25, 50_000, hoy.minusDays(40), hoy.minusDays(10), 100);
        cyber.setUsos(100);
        promociones.save(cyber);

        log.info("Datos de ejemplo cargados: {} cupones.", promociones.count());
    }
}
