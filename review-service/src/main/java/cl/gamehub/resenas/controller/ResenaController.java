package cl.gamehub.resenas.controller;

import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.resenas.dto.CrearResenaSolicitud;
import cl.gamehub.resenas.dto.MiResenaDto;
import cl.gamehub.resenas.dto.ResenaDto;
import cl.gamehub.resenas.service.ResenaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El cliente reseña lo que compró y recibió. SecurityConfig solo deja entrar al rol CLIENTE. */
@RestController
@RequestMapping("/api/resenas")
@SecurityRequirement(name = "bearer")
@Tag(name = "Reseñas del cliente", description = "Requiere rol CLIENTE")
public class ResenaController {

    private final ResenaService servicio;

    public ResenaController(ResenaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @Operation(summary = "Reseña un producto recibido",
            description = "Pregunta a order-service por la orden: debe ser del cliente, traer el producto y estar entregada. "
                    + "Una reseña por producto y orden; queda PENDIENTE hasta que el administrador la apruebe.")
    @ApiResponse(responseCode = "201", description = "Reseña creada, pendiente de moderación")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, el producto no está en la orden o todavía no se entrega")
    @ApiResponse(responseCode = "403", description = "La orden es de otro cliente")
    @ApiResponse(responseCode = "404", description = "La orden no existe (respuesta de order-service)")
    @ApiResponse(responseCode = "409", description = "Ya reseñó este producto en esta orden")
    @ApiResponse(responseCode = "503", description = "order-service no respondió")
    public ResponseEntity<ResenaDto> crear(@AuthenticationPrincipal UsuarioAutenticado yo,
                                           @Valid @RequestBody CrearResenaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(yo, solicitud));
    }

    @GetMapping("/mias")
    @Operation(summary = "Reseñas del cliente con su estado",
            description = "De la más reciente a la más antigua. Mis órdenes las usa para saber qué productos ya reseñó.")
    public ResponseEntity<List<MiResenaDto>> mias(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return ResponseEntity.ok(servicio.mias(yo.id()));
    }
}
