package cl.gamehub.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Lo que envía order-service cuando una compra queda pagada. referencia queda como motivo de cada
 * movimiento de salida, por ejemplo "Venta GH-2026-000019".
 */
public record DescuentoStockSolicitud(
        @Schema(example = "Venta GH-2026-000019")
        @NotBlank(message = "Escribe la referencia del descuento.")
        @Size(max = 120, message = "La referencia tiene hasta 120 caracteres.") String referencia,
        @NotEmpty(message = "Indica al menos una línea.")
        @Valid List<LineaDescuento> lineas) {
}
