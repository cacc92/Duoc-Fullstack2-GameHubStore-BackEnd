package cl.gamehub.ordenes.controller;

import cl.gamehub.ordenes.dto.ValidacionCarritoDto;
import cl.gamehub.ordenes.dto.ValidarCarritoSolicitud;
import cl.gamehub.ordenes.service.CarritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Público: el visitante arma su carrito sin iniciar sesión. */
@RestController
@RequestMapping("/api/carrito")
@Tag(name = "Carrito", description = "Validación del carrito contra stock, precios y cupón antes del checkout")
public class CarritoController {

    private final CarritoService servicio;

    public CarritoController(CarritoService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/validar")
    @Operation(summary = "Revisa stock, precios y cupón del carrito",
            description = "Consulta product-service, inventory-service y promotion-service. valido=false si alguna línea "
                    + "supera el stock; un cupón que no sirve deja su motivo en mensajeCupon.")
    @ApiResponse(responseCode = "200", description = "Carrito revisado")
    @ApiResponse(responseCode = "503", description = "Uno de los servicios consultados no respondió")
    public ResponseEntity<ValidacionCarritoDto> validar(@Valid @RequestBody ValidarCarritoSolicitud solicitud) {
        return ResponseEntity.ok(servicio.validar(solicitud));
    }
}
