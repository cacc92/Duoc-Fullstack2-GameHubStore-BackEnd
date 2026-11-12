package cl.gamehub.garantias.dto;

import cl.gamehub.garantias.model.EstadoGarantia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** resolucion es opcional para EN_REVISION y obligatoria para cerrar (RESUELTA o RECHAZADA). */
public record ResolverGarantiaSolicitud(
        @Schema(example = "RESUELTA")
        @NotNull(message = "Elige el nuevo estado.") EstadoGarantia estado,
        @Schema(example = "Se cambiaron los audífonos por una unidad nueva.")
        @Size(max = 600, message = "La resolución tiene hasta 600 caracteres.") String resolucion) {
}
