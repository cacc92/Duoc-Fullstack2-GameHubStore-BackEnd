package cl.gamehub.auth.client;

/** Lo que user-service necesita para crear el perfil. El id es el de la credencial recién guardada. */
public record NuevoPerfilSolicitud(Long id, String nombre, String email, String telefono, String region,
                                   String comuna, String direccion) {
}
