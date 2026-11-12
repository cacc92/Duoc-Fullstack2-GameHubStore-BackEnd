package cl.gamehub.despachos.dto;

import cl.gamehub.despachos.model.EstadoDespacho;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Un paso de la línea de seguimiento. */
public record EventoDto(
        @Schema(example = "DESPACHADO") EstadoDespacho estado,
        @Schema(example = "2026-11-12T14:05:00") LocalDateTime fecha) {
}
