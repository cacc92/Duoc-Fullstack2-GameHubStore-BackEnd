package cl.gamehub.promociones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/** Fila del mantenedor de promociones, con el estado calculado para hoy. */
public record PromocionDto(
        @Schema(example = "1") Long id,
        @Schema(example = "GAMER10") String codigo,
        @Schema(example = "10") int porcentaje,
        @Schema(example = "30000") int tope,
        @Schema(example = "2026-11-02") LocalDate inicio,
        @Schema(example = "2026-12-01") LocalDate termino,
        @Schema(example = "200") int usosMaximos,
        @Schema(example = "34") int usos,
        @Schema(example = "true") boolean activa,
        @Schema(example = "VIGENTE", allowableValues = {"VIGENTE", "PROGRAMADA", "VENCIDA", "AGOTADA", "INACTIVA"})
        String estado) {
}
