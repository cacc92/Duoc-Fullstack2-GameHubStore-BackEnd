package cl.gamehub.resenas.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearResenaSolicitud(
        @Schema(example = "1")
        @NotNull(message = "Falta la orden.") Long ordenId,
        @Schema(example = "6")
        @NotNull(message = "Falta el producto.") Long productoId,
        @Schema(example = "5")
        @NotNull(message = "Elige una puntuación.")
        @Min(value = 1, message = "La puntuación va de 1 a 5.")
        @Max(value = 5, message = "La puntuación va de 1 a 5.") Integer puntuacion,
        @Schema(example = "Suenan muy bien y son cómodos para jugar varias horas.")
        @NotBlank(message = "Escribe tu reseña.")
        @Size(min = 10, max = 500, message = "La reseña debe tener entre 10 y 500 caracteres.") String comentario) {
}
