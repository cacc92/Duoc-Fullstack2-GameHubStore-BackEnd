package cl.gamehub.productos.controller;

import cl.gamehub.productos.dto.ProductoDetalleDto;
import cl.gamehub.productos.dto.ProductoResumenDto;
import cl.gamehub.productos.service.ProductoService;
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

/** Rutas públicas: el visitante ve el catálogo sin iniciar sesión. */
@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Catálogo público: productos activos con su precio vigente")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista los productos activos",
            description = "Precio vigente hoy; precioNormal y ofertaHasta solo si la oferta sigue vigente. "
                    + "El stock lo entrega inventory-service y las reseñas, review-service.")
    @ApiResponse(responseCode = "200", description = "Productos ordenados por id (lista vacía si la categoría no existe)")
    @ApiResponse(responseCode = "503", description = "category-service no respondió")
    public ResponseEntity<List<ProductoResumenDto>> listar(
            @Parameter(description = "Slug de la categoría", example = "tarjetas-graficas")
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(servicio.listar(categoria));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un producto", description = "Incluye la descripción y la ficha técnica.")
    @ApiResponse(responseCode = "200", description = "Producto activo")
    @ApiResponse(responseCode = "404", description = "El producto no existe o está inactivo")
    public ResponseEntity<ProductoDetalleDto> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(servicio.detalle(id));
    }
}
