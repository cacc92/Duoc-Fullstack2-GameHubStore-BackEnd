package cl.gamehub.ordenes.model;

/** Estados que el operador puede pedir para un despacho. shipping-service valida que solo avance. */
public enum EstadoDespacho {
    PREPARANDO,
    DESPACHADO,
    EN_TRANSITO,
    ENTREGADO
}
