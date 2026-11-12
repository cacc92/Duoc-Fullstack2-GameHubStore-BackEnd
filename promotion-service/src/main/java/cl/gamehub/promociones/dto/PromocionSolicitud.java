package cl.gamehub.promociones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/** Cuerpo para crear o editar un cupón. Que el término no sea anterior al inicio lo revisa PromocionService. */
public record PromocionSolicitud(
        @Schema(example = "VERANO20")
        @NotBlank(message = "Escribe el código.")
        @Pattern(regexp = "^[A-Za-z0-9]{4,20}$", message = "El código usa de 4 a 20 letras o números, sin espacios.")
        String codigo,
        @Schema(example = "20")
        @NotNull(message = "Escribe el porcentaje de descuento.")
        @Min(value = 1, message = "El descuento debe ser de al menos 1 %.")
        @Max(value = 90, message = "El descuento no puede superar el 90 %.") Integer porcentaje,
        @Schema(example = "40000")
        @NotNull(message = "Escribe el tope de descuento.")
        @Positive(message = "El tope de descuento debe ser mayor que cero.") Integer tope,
        @Schema(example = "2026-12-01")
        @NotNull(message = "Elige la fecha de inicio.") LocalDate inicio,
        @Schema(example = "2026-12-31")
        @NotNull(message = "Elige la fecha de término.") LocalDate termino,
        @Schema(example = "150")
        @NotNull(message = "Escribe el tope de usos.")
        @Min(value = 1, message = "El tope de usos debe ser al menos 1.") Integer usosMaximos) {
}
