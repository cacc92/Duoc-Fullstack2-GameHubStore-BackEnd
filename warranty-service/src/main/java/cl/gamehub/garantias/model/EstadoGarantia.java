package cl.gamehub.garantias.model;

import java.util.EnumSet;
import java.util.Set;

/** RESUELTA y RECHAZADA cierran la solicitud y exigen escribir la resolución. */
public enum EstadoGarantia {
    ABIERTA,
    EN_REVISION,
    RESUELTA,
    RECHAZADA;

    /** Los que todavía esperan al operador: los cuenta el panel y no dejan abrir otra solicitud del mismo producto. */
    public static final Set<EstadoGarantia> ABIERTOS = EnumSet.of(ABIERTA, EN_REVISION);

    public boolean cierra() {
        return !ABIERTOS.contains(this);
    }
}
