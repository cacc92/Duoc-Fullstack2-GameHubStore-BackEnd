package cl.gamehub.comun.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protege las rutas /internal/**, que solo usan otros microservicios (por ejemplo, order-service
 * descontando stock en inventory-service). El Gateway no las enruta, y además cada llamada debe traer
 * la cabecera X-Clave-Interna con el valor de CLAVE_INTERNA. Feign la agrega sola (FeignInternoConfig).
 */
public class ClaveInternaFiltro extends OncePerRequestFilter {

    public static final String CABECERA = "X-Clave-Interna";

    private final byte[] clave;

    public ClaveInternaFiltro(String clave) {
        this.clave = clave.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest solicitud) {
        return !solicitud.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest solicitud, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        String recibida = solicitud.getHeader(CABECERA);
        // MessageDigest.isEqual compara en tiempo constante: no revela cuántos caracteres acertó
        if (recibida == null || !MessageDigest.isEqual(clave, recibida.getBytes(StandardCharsets.UTF_8))) {
            RespuestasSeguridad.escribir(respuesta, 403, "Ruta interna: solo otros microservicios de la tienda pueden usarla.");
            return;
        }
        cadena.doFilter(solicitud, respuesta);
    }
}
