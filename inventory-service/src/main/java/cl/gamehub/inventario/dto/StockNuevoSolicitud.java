package cl.gamehub.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Lo que envía product-service al crear un producto. Sin stockInicial, el producto parte con 0 unidades. */
public record StockNuevoSolicitud(
        @Schema(example = "14")
        @NotNull(message = "Indica el id del producto.") Long productoId,
        @Schema(example = "KB-NIM-MINI")
        @NotBlank(message = "Escribe el SKU.")
        @Size(max = 30, message = "El SKU tiene hasta 30 caracteres.") String sku,
        @Schema(example = "Teclado mecánico Nimbo Mini")
        @NotBlank(message = "Escribe el nombre del producto.")
        @Size(max = 120, message = "El nombre tiene hasta 120 caracteres.") String nombre,
        @Schema(example = "true")
        @NotNull(message = "Indica si el producto está activo.") Boolean activo,
        @Schema(example = "10")
        @PositiveOrZero(message = "El stock inicial no puede ser negativo.") Integer stockInicial) {
}
