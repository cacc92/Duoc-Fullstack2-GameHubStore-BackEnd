package cl.gamehub.inventario.controller;

import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.inventario.dto.MovimientoDto;
import cl.gamehub.inventario.dto.MovimientoSolicitud;
import cl.gamehub.inventario.dto.StockDto;
import cl.gamehub.inventario.service.InventarioService;
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

/** Stock y movimientos del panel del operador. SecurityConfig solo deja entrar a OPERADOR y ADMIN. */
@RestController
@RequestMapping("/api/operador/inventario")
@SecurityRequirement(name = "bearer")
@Tag(name = "Panel del operador: inventario", description = "Requiere rol OPERADOR o ADMIN")
public class OperadorInventarioController {

    private final InventarioService servicio;

    public OperadorInventarioController(InventarioService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Stock actual de todos los productos, activos e inactivos")
    public ResponseEntity<List<StockDto>> stock() {
        return ResponseEntity.ok(servicio.stock());
    }

    @GetMapping("/movimientos")
    @Operation(summary = "Últimos 30 movimientos de stock", description = "Del más reciente al más antiguo.")
    public ResponseEntity<List<MovimientoDto>> movimientos() {
        return ResponseEntity.ok(servicio.movimientos());
    }

    @PostMapping("/movimientos")
    @Operation(summary = "Registra un ingreso, una salida o un ajuste por conteo",
            description = "En AJUSTE, cantidad es el stock contado en bodega. El usuario del movimiento es el del token.")
    @ApiResponse(responseCode = "201", description = "Movimiento registrado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, cantidad cero, stock negativo o ajuste sin diferencia")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<MovimientoDto> registrar(@AuthenticationPrincipal UsuarioAutenticado yo,
                                                   @Valid @RequestBody MovimientoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud, yo.nombre()));
    }
}
