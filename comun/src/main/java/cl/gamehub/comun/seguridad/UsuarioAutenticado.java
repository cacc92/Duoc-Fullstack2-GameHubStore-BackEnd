package cl.gamehub.comun.seguridad;

/**
 * Datos del usuario que vienen dentro del token. JwtFiltro los deja en el contexto de seguridad y los
 * controladores los reciben con @AuthenticationPrincipal. Ningún servicio necesita consultar a
 * auth-service para saber quién llama: el token ya lo dice y su firma lo garantiza.
 */
public record UsuarioAutenticado(Long id, String email, String nombre, Rol rol) {
}
