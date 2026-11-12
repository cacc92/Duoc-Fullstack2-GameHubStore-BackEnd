package cl.gamehub.despachos.dto;

import cl.gamehub.despachos.model.EstadoDespacho;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * El despacho de una orden con su línea de seguimiento. No incluye la dirección ni el destinatario porque
 * la ruta de seguimiento es pública: con el código basta para ver el estado, no los datos personales.
 */
public record DespachoDto(
        @Schema(example = "1") Long ordenId,
        @Schema(example = "GH-2026-000001") String ordenNumero,
        @Schema(example = "GHX-61873") String seguimiento,
        @Schema(example = "EN_TRANSITO") EstadoDespacho estado,
        @Schema(example = "2026-11-15") LocalDate fechaEstimada,
        @Schema(example = "2026-11-14T18:05:00", description = "Solo si el despacho fue entregado") LocalDateTime entregadoEn,
        List<EventoDto> eventos) {
}
