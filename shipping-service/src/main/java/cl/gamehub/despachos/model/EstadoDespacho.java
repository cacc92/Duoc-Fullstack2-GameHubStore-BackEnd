package cl.gamehub.despachos.model;

/** El orden importa: el despacho solo puede avanzar hacia un estado posterior. */
public enum EstadoDespacho {
    PREPARANDO,
    DESPACHADO,
    EN_TRANSITO,
    ENTREGADO
}
