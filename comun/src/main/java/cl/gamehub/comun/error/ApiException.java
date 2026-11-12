package cl.gamehub.comun.error;

import org.springframework.http.HttpStatus;

/**
 * Error de negocio con su código HTTP. ManejadorErrores lo convierte en { "mensaje": "..." }, así el
 * frontend siempre muestra un texto que el usuario entiende. También es lo que lanza Feign cuando otro
 * microservicio responde con error: el mensaje remoto llega intacto hasta el navegador.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus estado;

    public ApiException(HttpStatus estado, String mensaje) {
        super(mensaje);
        this.estado = estado;
    }

    public HttpStatus getEstado() {
        return estado;
    }

    /** 400: los datos no cumplen una regla del negocio. */
    public static ApiException regla(String mensaje) {
        return new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** 401: credenciales inválidas. */
    public static ApiException noAutorizado(String mensaje) {
        return new ApiException(HttpStatus.UNAUTHORIZED, mensaje);
    }

    /** 403: el usuario existe, pero no puede hacer esto. */
    public static ApiException prohibido(String mensaje) {
        return new ApiException(HttpStatus.FORBIDDEN, mensaje);
    }

    /** 404: el recurso no existe. */
    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(HttpStatus.NOT_FOUND, mensaje);
    }

    /** 409: choca con el estado actual (stock, duplicados, despacho sin pago). */
    public static ApiException conflicto(String mensaje) {
        return new ApiException(HttpStatus.CONFLICT, mensaje);
    }

    /** 503: otro microservicio no respondió. */
    public static ApiException noDisponible(String mensaje) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, mensaje);
    }
}
