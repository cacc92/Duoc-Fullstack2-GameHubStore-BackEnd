package cl.gamehub.comun.feign;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.error.ErrorRespuesta;
import cl.gamehub.comun.seguridad.ClaveInternaFiltro;
import feign.RequestInterceptor;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

/**
 * Configuración que comparten todos los @FeignClient del ecosistema:
 * 1. Agrega la cabecera X-Clave-Interna, porque los clientes Feign solo llaman a rutas /internal/**.
 * 2. Convierte una respuesta con error del otro servicio en ApiException con el mismo código y el mismo
 *    mensaje. Así, si promotion-service dice «El cupón CYBER25 venció...», ese texto llega al frontend.
 */
@Configuration
public class FeignInternoConfig {

    private static final Logger log = LoggerFactory.getLogger(FeignInternoConfig.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Bean
    public RequestInterceptor claveInterna(@Value("${app.clave-interna}") String clave) {
        return plantilla -> plantilla.header(ClaveInternaFiltro.CABECERA, clave);
    }

    @Bean
    public ErrorDecoder erroresRemotos() {
        return (metodo, respuesta) -> {
            HttpStatus estado = HttpStatus.resolve(respuesta.status());
            String mensaje = mensaje(respuesta);
            log.warn("{} respondió {}: {}", metodo, respuesta.status(), mensaje);
            return new ApiException(estado == null ? HttpStatus.BAD_GATEWAY : estado, mensaje);
        };
    }

    private static String mensaje(Response respuesta) {
        try {
            if (respuesta.body() != null) {
                String cuerpo = Util.toString(respuesta.body().asReader(StandardCharsets.UTF_8));
                ErrorRespuesta error = JSON.readValue(cuerpo, ErrorRespuesta.class);
                if (error.mensaje() != null && !error.mensaje().isBlank()) {
                    return error.mensaje();
                }
            }
        } catch (IOException | RuntimeException e) {
            // Cuerpo vacío o sin el formato { mensaje }: se usa el texto genérico de abajo
        }
        return switch (respuesta.status()) {
            case 404 -> "No encontramos el recurso en otro servicio de la tienda.";
            // 503 sin cuerpo: el balanceador no encontró ninguna instancia registrada en Eureka
            case 503 -> "Uno de los servicios de la tienda no está disponible. Intenta de nuevo en unos minutos.";
            default -> "Otro servicio de la tienda respondió con un error.";
        };
    }
}
