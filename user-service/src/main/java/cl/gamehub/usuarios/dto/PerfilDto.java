package cl.gamehub.usuarios.dto;

import cl.gamehub.comun.seguridad.Rol;
import io.swagger.v3.oas.annotations.media.Schema;

/** Datos de contacto y despacho; el checkout los usa para precargar el formulario. */
public record PerfilDto(
        @Schema(example = "3") Long id,
        @Schema(example = "Camila Rojas") String nombre,
        @Schema(example = "camila@gamehub.test") String email,
        @Schema(example = "912345678") String telefono,
        @Schema(example = "Región Metropolitana de Santiago") String region,
        @Schema(example = "Ñuñoa") String comuna,
        @Schema(example = "Av. Los Aromos 1450, depto. 32") String direccion,
        @Schema(example = "CLIENTE", description = "Sale del token; es null en las rutas internas") Rol rol) {
}
