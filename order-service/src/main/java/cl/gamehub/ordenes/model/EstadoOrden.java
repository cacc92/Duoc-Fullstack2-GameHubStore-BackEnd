package cl.gamehub.ordenes.model;

/**
 * Estado visible de la orden. Sale del pago (payment-service) y del despacho (shipping-service):
 * order-service lo actualiza cada vez que coordina un cambio en esos servicios.
 */
public enum EstadoOrden {
    PENDIENTE_PAGO,
    PAGADA,
    EN_PREPARACION,
    DESPACHADA,
    EN_TRANSITO,
    ENTREGADA,
    RECHAZADA,
    ANULADA;

    /** Estados en que la orden ya tiene un despacho generado. */
    public boolean tieneDespacho() {
        return this == EN_PREPARACION || this == DESPACHADA || this == EN_TRANSITO || this == ENTREGADA;
    }

    /** Estado de la orden según el estado del pago que informa payment-service. */
    public static EstadoOrden desdePago(String estadoPago) {
        return switch (estadoPago) {
            case "APROBADO" -> PAGADA;
            case "RECHAZADO" -> RECHAZADA;
            case "ANULADO" -> ANULADA;
            default -> PENDIENTE_PAGO;
        };
    }

    /** Estado de la orden según el estado del despacho que informa shipping-service. */
    public static EstadoOrden desdeDespacho(String estadoDespacho) {
        return switch (estadoDespacho) {
            case "PREPARANDO" -> EN_PREPARACION;
            case "DESPACHADO" -> DESPACHADA;
            case "EN_TRANSITO" -> EN_TRANSITO;
            default -> ENTREGADA;
        };
    }
}
