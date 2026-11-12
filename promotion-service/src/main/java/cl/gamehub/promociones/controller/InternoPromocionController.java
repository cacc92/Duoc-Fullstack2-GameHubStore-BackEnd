package cl.gamehub.promociones.controller;

import cl.gamehub.promociones.dto.CuponDto;
import cl.gamehub.promociones.service.PromocionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rutas que usa order-service al crear una orden con cupón. El Gateway no las expone y exigen la cabecera
 * X-Clave-Interna. Los errores (cupón vencido, agotado...) llegan a order-service con el mismo código y mensaje.
 */
@RestController
@RequestMapping("/internal/promociones")
@Tag(name = "Interno", description = "Solo entre microservicios: requiere X-Clave-Interna y el Gateway no las expone")
public class InternoPromocionController {

    private final PromocionService servicio;

    public InternoPromocionController(PromocionService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/vigente")
    @Operation(summary = "El cupón si sirve hoy, con las mismas reglas y mensajes de /api/promociones/validar")
    @ApiResponse(responseCode = "400", description = "Falta el código, o el cupón está desactivado, vencido, aún no rige o se agotó")
    @ApiResponse(responseCode = "404", description = "El cupón no existe")
    public ResponseEntity<CuponDto> vigente(@RequestParam(required = false) String codigo) {
        return ResponseEntity.ok(servicio.validar(codigo));
    }

    @PostMapping("/{codigo}/usos")
    @Operation(summary = "Vuelve a validar el cupón y le suma un uso")
    @ApiResponse(responseCode = "400", description = "El cupón ya no sirve: no se cuenta el uso")
    @ApiResponse(responseCode = "404", description = "El cupón no existe")
    public ResponseEntity<CuponDto> registrarUso(@PathVariable String codigo) {
        return ResponseEntity.ok(servicio.registrarUso(codigo));
    }
}
