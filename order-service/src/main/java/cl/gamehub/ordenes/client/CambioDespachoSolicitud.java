package cl.gamehub.ordenes.client;

import java.time.LocalDate;

public record CambioDespachoSolicitud(String estado, String seguimiento, LocalDate fechaEstimada) {
}
