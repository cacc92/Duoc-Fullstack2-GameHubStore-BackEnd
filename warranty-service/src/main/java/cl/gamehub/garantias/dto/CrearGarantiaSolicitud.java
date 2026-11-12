package cl.gamehub.garantias.dto;

import cl.gamehub.garantias.model.MotivoGarantia;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearGarantiaSolicitud(
        @Schema(example = "10")
        @NotNull(message = "Falta la orden.") Long ordenId,
        @Schema(example = "2")
        @NotNull(message = "Falta el producto.") Long productoId,
        @Schema(example = "FALLA")
        @NotNull(message = "Elige el motivo de la solicitud.") MotivoGarantia motivo,
        @Schema(example = "La tarjeta se apaga sola a los pocos minutos de empezar a jugar.")
        @NotBlank(message = "Describe el problema.")
        @Size(min = 20, max = 600, message = "Describe el problema con al menos 20 caracteres.") String descripcion) {
}
