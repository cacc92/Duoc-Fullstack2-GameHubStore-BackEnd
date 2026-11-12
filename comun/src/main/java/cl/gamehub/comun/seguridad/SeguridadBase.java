package cl.gamehub.comun.seguridad;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Lo que la seguridad de los 11 servicios tiene en común. Cada servicio arma su SecurityConfig así:
 *
 * <pre>
 * SeguridadBase.aplicar(http, jwtServicio, claveInterna);
 * http.authorizeHttpRequests(reglas -&gt; {
 *     SeguridadBase.rutasAbiertas(reglas);
 *     reglas.requestMatchers(...).hasRole("ADMIN")   // reglas propias del servicio
 *           .anyRequest().authenticated();
 * });
 * </pre>
 */
public final class SeguridadBase {

    private SeguridadBase() {
    }

    /** API sin sesión de servidor: el token viaja en cada petición, por eso no hay cookies ni CSRF. */
    public static void aplicar(HttpSecurity http, JwtServicio jwtServicio, String claveInterna) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            // El CORS lo resuelve el Gateway: los servicios no agregan cabeceras CORS (si no, se duplican)
            .cors(cors -> cors.disable())
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(errores -> errores
                .authenticationEntryPoint(RespuestasSeguridad::noAutenticado)
                .accessDeniedHandler(RespuestasSeguridad::sinPermiso))
            .addFilterBefore(new ClaveInternaFiltro(claveInterna), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new JwtFiltro(jwtServicio), UsernamePasswordAuthenticationFilter.class);
    }

    /** Salud, Swagger y rutas internas (estas últimas las protege ClaveInternaFiltro). */
    public static void rutasAbiertas(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry reglas) {
        reglas
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/actuator/health/**", "/actuator/health", "/error").permitAll()
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers("/internal/**").permitAll();
    }
}
