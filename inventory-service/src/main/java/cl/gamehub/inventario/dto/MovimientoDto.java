package cl.gamehub.inventario.dto;

import cl.gamehub.inventario.model.TipoMovimiento;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** Un movimiento de stock tal como lo muestra el historial del panel del operador. */
public record MovimientoDto(
        @Schema(example = "6") Long id,
        @Schema(example = "2026-11-11T14:05:00") LocalDateTime fecha,
        @Schema(example = "10") Long productoId,
        @Schema(example = "SSD NVMe Tectra Flux 1 TB") String producto,
        @Schema(example = "INGRESO") TipoMovimiento tipo,
        @Schema(example = "20", description = "Con signo: negativo si salió mercadería") int cantidad,
        @Schema(example = "Recepción de proveedor") String motivo,
        @Schema(example = "Luis Soto") String usuario,
        @Schema(example = "50") int stockResultante) {
}
