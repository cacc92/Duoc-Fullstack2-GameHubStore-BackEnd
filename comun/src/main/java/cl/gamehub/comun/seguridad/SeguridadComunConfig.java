package cl.gamehub.comun.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Sin este bean, Spring Boot crea un usuario "user" con una contraseña aleatoria en cada servicio.
 * Aquí nadie inicia sesión con usuario y contraseña de Spring: el login lo hace auth-service y el
 * resto de los servicios solo confía en el JWT.
 */
@Configuration
public class SeguridadComunConfig {

    @Bean
    public UserDetailsService sinUsuariosLocales() {
        return nombre -> {
            throw new UsernameNotFoundException("Este servicio no tiene usuarios locales: usa el token de auth-service.");
        };
    }
}
