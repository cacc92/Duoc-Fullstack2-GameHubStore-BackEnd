package cl.gamehub.pagos.dto;

import cl.gamehub.pagos.model.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Lo que envía order-service al crear una orden: el pago se simula con el método y el total. */
public record NuevoPagoSolicitud(
        @Schema(example = "19")
        @NotNull(message = "Indica la orden.") Long ordenId,
        @Schema(example = "GH-2026-000019")
        @NotBlank(message = "Indica el número de la orden.")
        @Size(max = 20, message = "El número de la orden tiene hasta 20 caracteres.") String ordenNumero,
        @Schema(example = "3")
        @NotNull(message = "Indica el cliente de la orden.") Long usuarioId,
        @Schema(example = "DEBITO")
        @NotNull(message = "Elige un método de pago.") MetodoPago metodo,
        @Schema(example = "339980", description = "Total de la orden en pesos")
        @NotNull(message = "Indica el monto del pago.")
        @PositiveOrZero(message = "El monto no puede ser negativo.") Integer monto) {
}
