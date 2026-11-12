package cl.gamehub.inventario.controller;

import cl.gamehub.inventario.dto.DescuentoStockSolicitud;
import cl.gamehub.inventario.dto.DisponibilidadDto;
import cl.gamehub.inventario.dto.ProductoInventarioSolicitud;
import cl.gamehub.inventario.dto.StockDto;
import cl.gamehub.inventario.dto.StockNuevoSolicitud;
import cl.gamehub.inventario.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rutas que usan product-service y order-service. El Gateway no las expone y exigen la cabecera X-Clave-Interna. */
@RestController
@RequestMapping("/internal/inventario")
@Tag(name = "Interno", description = "Solo entre microservicios: requiere X-Clave-Interna y el Gateway no las expone")
public class InternoInventarioController {

    private final InventarioService servicio;

    public InternoInventarioController(InventarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/productos")
    @Operation(summary = "Crea el stock de un producto nuevo (lo llama product-service)",
            description = "Si stockInicial es mayor que cero, registra un INGRESO «Stock inicial del producto».")
    @ApiResponse(responseCode = "201", description = "Stock creado")
    @ApiResponse(responseCode = "409", description = "Ya existe stock para ese producto")
    public ResponseEntity<StockDto> crear(@Valid @RequestBody StockNuevoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    @PutMapping("/productos/{productoId}")
    @Operation(summary = "Actualiza el SKU, el nombre y el estado de un producto (lo llama product-service)")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<StockDto> actualizarProducto(@PathVariable Long productoId,
                                                       @Valid @RequestBody ProductoInventarioSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizarProducto(productoId, solicitud));
    }

    @GetMapping("/stock")
    @Operation(summary = "Stock disponible de varios productos")
    public ResponseEntity<List<DisponibilidadDto>> stock(
            @Parameter(description = "Ids de producto separados por coma", example = "1,2") @RequestParam List<Long> ids) {
        return ResponseEntity.ok(servicio.disponibilidad(ids));
    }

    @PostMapping("/descontar")
    @Operation(summary = "Descuenta el stock de una compra pagada (lo llama order-service)",
            description = "Todo o nada: si una línea supera el stock, no se descuenta ninguna. Registra una SALIDA por línea.")
    @ApiResponse(responseCode = "200", description = "Stock que quedó de cada producto de la compra")
    @ApiResponse(responseCode = "404", description = "Uno de los productos no existe")
    @ApiResponse(responseCode = "409", description = "No hay stock suficiente de un producto")
    public ResponseEntity<List<DisponibilidadDto>> descontar(@Valid @RequestBody DescuentoStockSolicitud solicitud) {
        return ResponseEntity.ok(servicio.descontar(solicitud));
    }
}
