package cl.gamehub.despachos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Lo que envía order-service cuando el operador genera el despacho de una orden pagada. El seguimiento y la
 * fecha estimada son opcionales (DespachoService pone los valores por defecto); los datos de entrega son una
 * copia de los de la orden y también pueden venir vacíos.
 */
public record NuevoDespachoSolicitud(
        @Schema(example = "5")
        @NotNull(message = "Indica la orden.") Long ordenId,
        @Schema(example = "GH-2026-000005")
        @NotBlank(message = "Indica el número de la orden.")
        @Size(max = 20, message = "El número de la orden tiene hasta 20 caracteres.") String ordenNumero,
        @Schema(example = "GHX-70005", description = "Vacío: GHX- seguido de 70000 + el id de la orden")
        @Size(max = 30, message = "El número de seguimiento tiene hasta 30 caracteres.") String seguimiento,
        @Schema(example = "2026-11-14", description = "Vacía: hoy + 3 días")
        @FutureOrPresent(message = "La fecha estimada no puede estar en el pasado.") LocalDate fechaEstimada,
        @Schema(example = "Camila Rojas")
        @Size(max = 80, message = "El destinatario tiene hasta 80 caracteres.") String destinatario,
        @Schema(example = "Av. Los Aromos 1450, depto. 32")
        @Size(max = 150, message = "La dirección tiene hasta 150 caracteres.") String direccion,
        @Schema(example = "Ñuñoa")
        @Size(max = 60, message = "La comuna tiene hasta 60 caracteres.") String comuna,
        @Schema(example = "Región Metropolitana de Santiago")
        @Size(max = 60, message = "La región tiene hasta 60 caracteres.") String region) {
}
