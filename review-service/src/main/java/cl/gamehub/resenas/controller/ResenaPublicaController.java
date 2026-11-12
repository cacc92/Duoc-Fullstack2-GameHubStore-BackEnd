package cl.gamehub.resenas.controller;

import cl.gamehub.resenas.dto.ResenaDto;
import cl.gamehub.resenas.dto.ResumenResenasDto;
import cl.gamehub.resenas.service.ResenaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rutas públicas: el visitante ve las reseñas aprobadas y las estrellas de cada producto sin iniciar sesión. */
@RestController
@RequestMapping("/api")
@Tag(name = "Reseñas públicas", description = "Solo muestran las reseñas que aprobó el administrador")
public class ResenaPublicaController {

    private final ResenaService servicio;

    public ResenaPublicaController(ResenaService servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/productos/{productoId}/resenas")
    @Operation(summary = "Reseñas aprobadas de un producto",
            description = "De la más reciente a la más antigua. El Gateway envía esta ruta a review-service antes que a product-service.")
    @ApiResponse(responseCode = "200", description = "Lista vacía si el producto todavía no tiene reseñas aprobadas")
    public ResponseEntity<List<ResenaDto>> deProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(servicio.publicas(productoId));
    }

    @GetMapping("/resenas/resumen")
    @Operation(summary = "Puntuación promedio y cantidad de reseñas por producto",
            description = "Cuenta solo las aprobadas y redondea el promedio a un decimal. Sin el parámetro productos "
                    + "trae todos; los productos sin reseñas aprobadas no aparecen en la lista.")
    @ApiResponse(responseCode = "200", description = "Un elemento por producto con reseñas aprobadas, ordenados por id")
    public ResponseEntity<List<ResumenResenasDto>> resumen(
            @Parameter(description = "Id de los productos separados por coma", example = "1,2,3")
            @RequestParam(required = false) List<Long> productos) {
        return ResponseEntity.ok(servicio.resumen(productos));
    }
}
