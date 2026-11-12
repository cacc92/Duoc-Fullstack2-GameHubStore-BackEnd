package cl.gamehub.productos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

/**
 * Formulario de crear y editar producto. stock solo se usa al crear: es el stock inicial que se informa a
 * inventory-service. Después, el stock se ajusta desde el panel del operador.
 */
public record ProductoSolicitud(
        @Schema(example = "KB-NIM-MINI")
        @NotBlank(message = "Escribe el SKU.")
        @Size(max = 30, message = "El SKU tiene hasta 30 caracteres.") String sku,
        @Schema(example = "Teclado mecánico Nimbo Mini")
        @NotBlank(message = "Escribe el nombre del producto.")
        @Size(max = 120, message = "El nombre tiene hasta 120 caracteres.") String nombre,
        @Schema(example = "Nimbo")
        @NotBlank(message = "Escribe la marca.")
        @Size(max = 60, message = "La marca tiene hasta 60 caracteres.") String marca,
        @Schema(example = "4")
        @NotNull(message = "Elige una categoría.") Long categoriaId,
        @Schema(example = "49990")
        @NotNull(message = "Escribe el precio.")
        @Positive(message = "El precio debe ser mayor que cero.") Integer precio,
        @Schema(example = "54990", description = "Opcional: si viene, el producto queda en oferta hasta ofertaHasta")
        @Positive(message = "El precio normal debe ser mayor que cero.") Integer precioNormal,
        @Schema(example = "2026-12-01") LocalDate ofertaHasta,
        @Schema(example = "10", description = "Stock inicial (solo al crear)")
        @PositiveOrZero(message = "El stock inicial no puede ser negativo.") Integer stock,
        @Schema(example = "Switches rojos · RGB · USB-C")
        @Size(max = 120, message = "El resumen tiene hasta 120 caracteres.") String resumen,
        @Size(max = 2000, message = "La descripción tiene hasta 2000 caracteres.") String descripcion,
        @Schema(example = "teclado", description = "Vacío: se usa la ilustración de la categoría")
        @Size(max = 255, message = "La imagen tiene hasta 255 caracteres.") String imagen,
        @Valid List<EspecificacionDto> especificaciones) {
}
