package cl.gamehub.comun.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** Cuerpo de todas las respuestas de error: { "mensaje": "...", "errores": { "campo": "..." } }. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorRespuesta(String mensaje, Map<String, String> errores) {

    public ErrorRespuesta(String mensaje) {
        this(mensaje, null);
    }
}
