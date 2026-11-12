package cl.gamehub.garantias.dto;

import cl.gamehub.garantias.model.EstadoGarantia;
import cl.gamehub.garantias.model.MotivoGarantia;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Una solicitud de garantía, igual para el cliente y para el operador. */
public record GarantiaDto(
        @Schema(example = "1") Long id,
        @Schema(example = "GAR-0001") String codigo,
        @Schema(example = "10") Long ordenId,
        @Schema(example = "GH-2026-000010") String ordenNumero,
        @Schema(example = "6") Long productoId,
        @Schema(example = "Audífonos Andesia Pulse 7.1") String producto,
        @Schema(example = "Martina Castro") String cliente,
        @Schema(example = "FALLA") MotivoGarantia motivo,
        @Schema(example = "Se corta el audio del lado izquierdo desde la semana pasada, incluso con otro cable.") String descripcion,
        @Schema(example = "EN_REVISION") EstadoGarantia estado,
        @Schema(example = "Se pidió un video de la falla para evaluar el cambio.") String resolucion,
        @Schema(example = "2026-11-09T14:00:00") LocalDateTime creadaEn,
        @Schema(example = "2026-11-10T14:00:00") LocalDateTime actualizadaEn,
        @Schema(example = "2027-05-02", description = "Último día para pedir garantía: 6 meses desde la entrega") LocalDate plazo) {
}
