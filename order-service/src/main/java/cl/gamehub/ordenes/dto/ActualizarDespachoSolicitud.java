package cl.gamehub.ordenes.dto;

import cl.gamehub.ordenes.model.EstadoDespacho;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ActualizarDespachoSolicitud(
        @Schema(example = "DESPACHADO") @NotNull(message = "Elige el nuevo estado del despacho.") EstadoDespacho estado,
        @Size(max = 30, message = "El número de seguimiento tiene hasta 30 caracteres.") String seguimiento,
        LocalDate fechaEstimada) {
}
