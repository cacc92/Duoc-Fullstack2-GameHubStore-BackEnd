package cl.gamehub.ordenes.dto;

/** Cifras de órdenes del panel del operador. Las garantías abiertas las cuenta warranty-service. */
public record ResumenOperadorDto(long pagosPorConfirmar, long despachosPorGenerar, long enCurso) {
}
