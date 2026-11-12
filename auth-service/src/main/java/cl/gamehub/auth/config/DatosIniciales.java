package cl.gamehub.auth.config;

import cl.gamehub.auth.model.Credencial;
import cl.gamehub.auth.repository.CredencialRepository;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.UsuarioEjemplo;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga las 17 cuentas de ejemplo si la base está vacía. Se insertan en orden para que sus id queden del
 * 1 al 17, los mismos que usan user-service (perfiles) y order-service (órdenes de cada cliente).
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    /** Contraseña de todas las cuentas de ejemplo. */
    public static final String CLAVE_PRUEBA = "clave123";

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final CredencialRepository credenciales;
    private final PasswordEncoder codificador;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, CredencialRepository credenciales,
                          PasswordEncoder codificador) {
        this.habilitado = habilitado;
        this.credenciales = credenciales;
        this.codificador = codificador;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || credenciales.count() > 0) {
            return;
        }
        LocalDateTime base = DatosDeEjemplo.base();
        // BCrypt es lento a propósito: el hash se calcula una vez y lo comparten las 17 cuentas
        String hash = codificador.encode(CLAVE_PRUEBA);
        for (UsuarioEjemplo u : DatosDeEjemplo.USUARIOS) {
            Credencial credencial = new Credencial(u.nombre(), u.email(), hash, Rol.valueOf(u.rol()));
            credencial.setActivo(u.activo());
            credencial.setCreadoEn(base.minusDays(u.diasDesdeRegistro()));
            credenciales.save(credencial);
        }
        log.info("Datos de ejemplo cargados: {} cuentas.", credenciales.count());
    }
}
