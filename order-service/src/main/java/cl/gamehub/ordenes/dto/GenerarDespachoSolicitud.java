package cl.gamehub.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Ambos campos son opcionales: sin seguimiento se usa GHX-(70000 + id) y sin fecha, hoy + 3 días. */
public record GenerarDespachoSolicitud(
        @Schema(example = "GHX-70019")
        @Size(max = 30, message = "El número de seguimiento tiene hasta 30 caracteres.") String seguimiento,
        @FutureOrPresent(message = "La fecha estimada no puede estar en el pasado.") LocalDate fechaEstimada) {
}
