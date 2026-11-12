package cl.gamehub.despachos.dto;

import cl.gamehub.despachos.model.EstadoDespacho;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Avance del despacho. El seguimiento y la fecha estimada solo cambian si vienen. */
public record ActualizarDespachoSolicitud(
        @Schema(example = "DESPACHADO")
        @NotNull(message = "Elige el nuevo estado del despacho.") EstadoDespacho estado,
        @Schema(example = "GHX-70005")
        @Size(max = 30, message = "El número de seguimiento tiene hasta 30 caracteres.") String seguimiento,
        @Schema(example = "2026-11-15") LocalDate fechaEstimada) {
}
