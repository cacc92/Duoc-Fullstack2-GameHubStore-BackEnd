package cl.gamehub.inventario.dto;

import cl.gamehub.inventario.model.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** En AJUSTE, cantidad es el stock contado en bodega; en INGRESO y SALIDA, las unidades que se mueven. */
public record MovimientoSolicitud(
        @Schema(example = "10")
        @NotNull(message = "Elige un producto.") Long productoId,
        @Schema(example = "INGRESO")
        @NotNull(message = "Elige el tipo de movimiento.") TipoMovimiento tipo,
        @Schema(example = "20")
        @NotNull(message = "Escribe la cantidad.")
        @PositiveOrZero(message = "La cantidad no puede ser negativa.") Integer cantidad,
        @Schema(example = "Recepción de proveedor")
        @NotBlank(message = "Escribe el motivo del movimiento.")
        @Size(max = 120, message = "El motivo tiene hasta 120 caracteres.") String motivo) {
}
