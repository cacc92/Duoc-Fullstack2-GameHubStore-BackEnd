package cl.gamehub.garantias.config;

import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.SeguridadBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Reglas de acceso de este servicio. El frontend oculta lo que un rol no puede hacer, pero la validación
 * real ocurre aquí: cada microservicio vuelve a revisar el token y el rol en cada llamada.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtServicio jwtServicio,
                                               @Value("${app.clave-interna}") String claveInterna) throws Exception {
        SeguridadBase.aplicar(http, jwtServicio, claveInterna);
        http.authorizeHttpRequests(reglas -> {
            SeguridadBase.rutasAbiertas(reglas);
            reglas.requestMatchers("/api/operador/**").hasAnyRole("OPERADOR", "ADMIN")
                  .requestMatchers("/api/garantias/**").hasRole("CLIENTE")
                  .anyRequest().authenticated();
        });
        return http.build();
    }
}
