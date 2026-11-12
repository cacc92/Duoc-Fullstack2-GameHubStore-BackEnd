package cl.gamehub.promociones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Lo que necesita el carrito (y order-service) para calcular el descuento: porcentaje con tope en pesos. */
public record CuponDto(
        @Schema(example = "GAMER10") String codigo,
        @Schema(example = "10") int porcentaje,
        @Schema(example = "30000", description = "Descuento máximo en pesos") int tope,
        @Schema(example = "2026-11-02") LocalDate inicio,
        @Schema(example = "2026-12-01") LocalDate termino) {
}
