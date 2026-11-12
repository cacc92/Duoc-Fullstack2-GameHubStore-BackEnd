package cl.gamehub.comun.util;

import jakarta.validation.constraints.NotNull;

/** Cuerpo de los PATCH que activan o desactivan algo: { "activo": true }. */
public record ActivoSolicitud(@NotNull(message = "Indica si queda activo.") Boolean activo) {
}
