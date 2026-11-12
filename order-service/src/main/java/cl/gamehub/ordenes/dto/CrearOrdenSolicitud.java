package cl.gamehub.ordenes.dto;

import cl.gamehub.ordenes.model.MetodoPago;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CrearOrdenSolicitud(
        @NotEmpty(message = "El carrito está vacío.") @Valid List<LineaSolicitud> lineas,
        @Schema(example = "GAMER10") String cupon,
        @Schema(example = "DEBITO") @NotNull(message = "Elige un método de pago.") MetodoPago metodoPago,
        @NotNull(message = "Faltan los datos de despacho.") @Valid DatosDespachoDto despacho) {
}
