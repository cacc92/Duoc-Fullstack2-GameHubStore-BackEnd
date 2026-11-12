package cl.gamehub.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LineaSolicitud(
        @Schema(example = "4") @NotNull(message = "Falta el producto.") Long productoId,
        @Schema(example = "1") @NotNull(message = "Falta la cantidad.")
        @Min(value = 1, message = "La cantidad debe ser al menos 1.") Integer cantidad) {
}
