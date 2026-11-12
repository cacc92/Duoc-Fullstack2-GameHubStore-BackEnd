package cl.gamehub.productos.controller;

import cl.gamehub.comun.util.ActivoSolicitud;
import cl.gamehub.productos.dto.ProductoAdminDto;
import cl.gamehub.productos.dto.ProductoSolicitud;
import cl.gamehub.productos.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mantenedor de productos. SecurityConfig solo deja entrar al rol ADMIN. */
@RestController
@RequestMapping("/api/admin/productos")
@SecurityRequirement(name = "bearer")
@Tag(name = "Administración de productos", description = "Requiere rol ADMIN")
public class AdminProductoController {

    private final ProductoService servicio;

    public AdminProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista todos los productos, activos e inactivos",
            description = "Con los precios guardados, no los vigentes de hoy.")
    public ResponseEntity<List<ProductoAdminDto>> listar() {
        return ResponseEntity.ok(servicio.listarTodos());
    }

    @PostMapping
    @Operation(summary = "Crea un producto",
            description = "El SKU queda en mayúsculas y la categoría se valida en category-service. "
                    + "Después pide a inventory-service que cree el stock inicial; si falla, el producto no se guarda.")
    @ApiResponse(responseCode = "201", description = "Producto creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, categoría inexistente o precio normal menor que el de oferta")
    @ApiResponse(responseCode = "409", description = "Ya existe un producto con ese SKU")
    @ApiResponse(responseCode = "503", description = "category-service o inventory-service no respondió")
    public ResponseEntity<ProductoAdminDto> crear(@Valid @RequestBody ProductoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un producto",
            description = "El stock no se cambia aquí: se ajusta desde el panel del operador (inventory-service).")
    @ApiResponse(responseCode = "400", description = "Datos inválidos, categoría inexistente o precio normal menor que el de oferta")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    @ApiResponse(responseCode = "409", description = "Otro producto ya usa ese SKU")
    public ResponseEntity<ProductoAdminDto> actualizar(@PathVariable Long id, @Valid @RequestBody ProductoSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud));
    }

    @PatchMapping("/{id}/activo")
    @Operation(summary = "Activa o desactiva un producto en la tienda", description = "También avisa a inventory-service.")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<ProductoAdminDto> cambiarActivo(@PathVariable Long id, @Valid @RequestBody ActivoSolicitud solicitud) {
        return ResponseEntity.ok(servicio.cambiarActivo(id, solicitud.activo()));
    }
}
