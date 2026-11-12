package cl.gamehub.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Una línea de la compra: cuántas unidades de qué producto. */
public record LineaDescuento(
        @Schema(example = "2")
        @NotNull(message = "Indica el producto de cada línea.") Long productoId,
        @Schema(example = "1")
        @NotNull(message = "Indica la cantidad de cada línea.")
        @Positive(message = "La cantidad de cada línea debe ser mayor que cero.") Integer cantidad) {
}
