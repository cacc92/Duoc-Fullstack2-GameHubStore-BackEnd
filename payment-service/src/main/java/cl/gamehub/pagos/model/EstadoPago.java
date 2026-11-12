package cl.gamehub.pagos.model;

/** PENDIENTE solo ocurre con transferencias: el operador la confirma (APROBADO) o la anula (ANULADO). */
public enum EstadoPago {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    ANULADO
}
