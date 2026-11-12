package cl.gamehub.ordenes.client;

public record NuevoPagoSolicitud(Long ordenId, String ordenNumero, Long usuarioId, String metodo, int monto) {
}
