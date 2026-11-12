package cl.gamehub.inventario.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Una fila de la tabla de stock del panel del operador. id es el id del producto. */
public record StockDto(
        @Schema(example = "2") Long id,
        @Schema(example = "GPU-VOL-X70") String sku,
        @Schema(example = "Tarjeta gráfica Voltra X70 OC 12 GB") String nombre,
        @Schema(example = "12") int stock,
        @Schema(example = "true") boolean activo) {
}
