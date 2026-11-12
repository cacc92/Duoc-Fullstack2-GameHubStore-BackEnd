package cl.gamehub.ordenes.config;

import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.SeguridadBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** El visitante valida su carrito; comprar exige rol CLIENTE; la bandeja, OPERADOR o ADMIN. */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http, JwtServicio jwtServicio,
                                               @Value("${app.clave-interna}") String claveInterna) throws Exception {
        SeguridadBase.aplicar(http, jwtServicio, claveInterna);
        http.authorizeHttpRequests(reglas -> {
            SeguridadBase.rutasAbiertas(reglas);
            reglas.requestMatchers(HttpMethod.POST, "/api/carrito/validar").permitAll()
                  .requestMatchers("/api/ordenes/**").hasRole("CLIENTE")
                  .requestMatchers("/api/operador/**").hasAnyRole("OPERADOR", "ADMIN")
                  .anyRequest().authenticated();
        });
        return http.build();
    }
}
