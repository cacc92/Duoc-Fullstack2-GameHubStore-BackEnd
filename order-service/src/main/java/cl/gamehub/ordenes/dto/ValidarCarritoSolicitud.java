package cl.gamehub.ordenes.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** El carrito vive en el navegador; aquí solo se revisa contra el stock, los precios y el cupón. */
public record ValidarCarritoSolicitud(
        @NotNull(message = "Faltan las líneas del carrito.") @Valid List<LineaSolicitud> lineas,
        @Schema(example = "GAMER10") String cupon) {
}
