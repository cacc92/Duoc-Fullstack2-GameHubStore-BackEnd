package cl.gamehub.usuarios.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.UsuarioEjemplo;
import cl.gamehub.usuarios.model.Perfil;
import cl.gamehub.usuarios.repository.PerfilRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga los 17 perfiles de ejemplo si la base está vacía, cada uno con el id de su cuenta en auth-service
 * (del 1 al 17). La fecha de actualización es la del registro de la cuenta.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final PerfilRepository perfiles;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, PerfilRepository perfiles) {
        this.habilitado = habilitado;
        this.perfiles = perfiles;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || perfiles.count() > 0) {
            return;
        }
        LocalDateTime base = DatosDeEjemplo.base();
        for (UsuarioEjemplo u : DatosDeEjemplo.USUARIOS) {
            Perfil perfil = new Perfil(u.id(), u.nombre(), u.email(), u.telefono(), u.region(), u.comuna(),
                    u.direccion());
            perfil.setActualizadoEn(base.minusDays(u.diasDesdeRegistro()));
            perfiles.save(perfil);
        }
        log.info("Datos de ejemplo cargados: {} perfiles.", perfiles.count());
    }
}
