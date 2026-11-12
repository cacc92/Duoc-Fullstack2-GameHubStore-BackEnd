package cl.gamehub.comun.seguridad;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Lee la cabecera "Authorization: Bearer <token>". Si el token es válido, la petición queda
 * autenticada con el rol del token (ROLE_ADMIN, ROLE_OPERADOR o ROLE_CLIENTE). Si no, sigue como
 * anónima y la configuración de seguridad de cada servicio decide si responde 401.
 */
public class JwtFiltro extends OncePerRequestFilter {

    public static final String ATRIBUTO_ERROR = "jwt.error";

    private final JwtServicio jwtServicio;

    public JwtFiltro(JwtServicio jwtServicio) {
        this.jwtServicio = jwtServicio;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String cabecera = solicitud.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            try {
                UsuarioAutenticado usuario = jwtServicio.validar(cabecera.substring(7));
                var autoridad = new SimpleGrantedAuthority("ROLE_" + usuario.rol().name());
                var autenticacion = new UsernamePasswordAuthenticationToken(usuario, null, List.of(autoridad));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            } catch (ExpiredJwtException e) {
                // RespuestasSeguridad usa este atributo para decir "tu sesión expiró"
                solicitud.setAttribute(ATRIBUTO_ERROR, "vencido");
            } catch (JwtException | IllegalArgumentException e) {
                solicitud.setAttribute(ATRIBUTO_ERROR, "invalido");
            }
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
