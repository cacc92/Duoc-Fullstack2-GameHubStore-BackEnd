package cl.gamehub.despachos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;

/** Cada cambio de estado del despacho, con su fecha, para dibujar la línea de seguimiento. */
@Embeddable
public class EventoDespacho {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDespacho estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected EventoDespacho() {
    }

    public EventoDespacho(EstadoDespacho estado, LocalDateTime fecha) {
        this.estado = estado;
        this.fecha = fecha;
    }

    public EstadoDespacho getEstado() { return estado; }
    public LocalDateTime getFecha() { return fecha; }
}
