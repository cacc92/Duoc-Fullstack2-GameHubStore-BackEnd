package cl.gamehub.promociones.config;

import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.SeguridadBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
            // El visitante valida su cupón en el carrito sin iniciar sesión
            reglas.requestMatchers(HttpMethod.GET, "/api/promociones/**").permitAll()
                  .requestMatchers("/api/admin/**").hasRole("ADMIN")
                  .anyRequest().authenticated();
        });
        return http.build();
    }
}
