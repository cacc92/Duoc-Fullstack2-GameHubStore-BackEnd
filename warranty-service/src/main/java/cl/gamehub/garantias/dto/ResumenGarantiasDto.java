package cl.gamehub.garantias.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Contador del panel del operador: solicitudes que esperan respuesta (ABIERTA o EN_REVISION). */
public record ResumenGarantiasDto(@Schema(example = "2") long abiertas) {
}
