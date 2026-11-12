package cl.gamehub.comun.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

/** Respuestas 401 y 403 con el mismo formato { mensaje } que usa el resto de la API. */
public final class RespuestasSeguridad {

    private RespuestasSeguridad() {
    }

    /** 401: no hay token o el token ya no sirve. */
    public static void noAutenticado(HttpServletRequest solicitud, HttpServletResponse respuesta,
                                     AuthenticationException e) throws IOException {
        boolean vencido = "vencido".equals(solicitud.getAttribute(JwtFiltro.ATRIBUTO_ERROR));
        escribir(respuesta, 401, vencido
                ? "Tu sesión expiró. Vuelve a iniciar sesión."
                : "Debes iniciar sesión para realizar esta acción.");
    }

    /** 403: hay sesión, pero el rol no alcanza. */
    public static void sinPermiso(HttpServletRequest solicitud, HttpServletResponse respuesta,
                                  AccessDeniedException e) throws IOException {
        escribir(respuesta, 403, "Tu rol no tiene permiso para realizar esta acción.");
    }

    static void escribir(HttpServletResponse respuesta, int estado, String mensaje) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json;charset=UTF-8");
        respuesta.getWriter().write("{\"mensaje\":\"" + mensaje.replace("\"", "\\\"") + "\"}");
    }
}
