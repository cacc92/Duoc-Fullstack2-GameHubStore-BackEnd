package cl.gamehub.ordenes.client;

import java.time.LocalDate;

public record NuevoDespachoSolicitud(Long ordenId, String ordenNumero, String seguimiento, LocalDate fechaEstimada,
                                     String destinatario, String direccion, String comuna, String region) {
}
