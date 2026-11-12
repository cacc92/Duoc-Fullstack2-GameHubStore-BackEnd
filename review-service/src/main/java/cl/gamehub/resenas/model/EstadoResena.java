package cl.gamehub.resenas.model;

/** Las reseñas nuevas esperan moderación; solo las aprobadas se muestran en la tienda. */
public enum EstadoResena {
    PENDIENTE,
    APROBADA,
    OCULTA
}
