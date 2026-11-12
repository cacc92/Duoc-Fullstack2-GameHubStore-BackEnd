package cl.gamehub.promociones.controller;

import cl.gamehub.promociones.dto.CuponDto;
import cl.gamehub.promociones.service.PromocionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ruta pública: el visitante prueba su cupón en el carrito sin iniciar sesión. */
@RestController
@RequestMapping("/api/promociones")
@Tag(name = "Promociones", description = "Validación de cupones de descuento")
public class PromocionController {

    private final PromocionService servicio;

    public PromocionController(PromocionService servicio) {
        this.servicio = servicio;
    }

    /** codigo es opcional en la URL para que, si falta, la respuesta sea «Escribe un código de cupón.». */
    @GetMapping("/validar")
    @Operation(summary = "Valida un cupón",
            description = "No distingue mayúsculas. Devuelve el porcentaje y el tope para que el carrito calcule el descuento.")
    @ApiResponse(responseCode = "200", description = "El cupón sirve hoy")
    @ApiResponse(responseCode = "400", description = "Falta el código, o el cupón está desactivado, vencido, aún no rige o se agotó")
    @ApiResponse(responseCode = "404", description = "El cupón no existe")
    public ResponseEntity<CuponDto> validar(
            @Parameter(example = "GAMER10") @RequestParam(required = false) String codigo) {
        return ResponseEntity.ok(servicio.validar(codigo));
    }
}
