package cl.gamehub.ordenes.client;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** estado: PREPARANDO, DESPACHADO, EN_TRANSITO o ENTREGADO. eventos alimenta la línea de seguimiento. */
public record DespachoDto(Long ordenId, String seguimiento, String estado, LocalDate fechaEstimada,
                          LocalDateTime entregadoEn, List<EventoDto> eventos) {

    public record EventoDto(String estado, LocalDateTime fecha) {
    }
}
