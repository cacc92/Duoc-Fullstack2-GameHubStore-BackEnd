package cl.gamehub.pagos.dto;

import cl.gamehub.pagos.model.EstadoPago;
import cl.gamehub.pagos.model.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** El pago de una orden. order-service lo muestra dentro del detalle de la orden. */
public record PagoDto(
        @Schema(example = "1") Long id,
        @Schema(example = "1") Long ordenId,
        @Schema(example = "GH-2026-000001") String ordenNumero,
        @Schema(example = "DEBITO") MetodoPago metodo,
        @Schema(example = "339980", description = "Total de la orden en pesos") int monto,
        @Schema(example = "APROBADO") EstadoPago estado,
        @Schema(example = "2026-11-11T14:05:00", description = "Fecha del último cambio de estado") LocalDateTime fecha,
        @Schema(example = "Pago aprobado (simulado).") String detalle) {
}
