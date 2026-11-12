package cl.gamehub.comun.error;

import feign.FeignException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Traduce cada excepción a un código HTTP y a un mensaje en español con forma { mensaje }. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorRespuesta> negocio(ApiException e) {
        log.warn("{} {}", e.getEstado().value(), e.getMessage());
        return ResponseEntity.status(e.getEstado()).body(new ErrorRespuesta(e.getMessage()));
    }

    /** 400 con el detalle de cada campo que falló (@NotBlank, @Email, @Positive...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> validacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError campo : e.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(campo.getField(), campo.getDefaultMessage());
        }
        String primero = errores.values().stream().findFirst().orElse("Revisa los datos enviados.");
        log.warn("400 validación: {}", errores.keySet());
        return ResponseEntity.badRequest().body(new ErrorRespuesta(primero, errores));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    public ResponseEntity<ErrorRespuesta> solicitudInvalida(Exception e) {
        return ResponseEntity.badRequest().body(new ErrorRespuesta("La solicitud no tiene el formato esperado."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorRespuesta> sinPermiso(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorRespuesta("Tu rol no tiene permiso para realizar esta acción."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> integridad(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorRespuesta("El registro ya existe o lo usan otros datos."));
    }

    /**
     * Otro microservicio no respondió (está caído, no se registró en Eureka o tardó más que el timeout).
     * Las respuestas con error sí llegan como ApiException gracias a FeignErrores.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorRespuesta> servicioRemoto(FeignException e) {
        log.error("Falló la llamada a otro microservicio: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorRespuesta("Uno de los servicios de la tienda no responde. Intenta de nuevo en unos minutos."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRespuesta> rutaInexistente(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta("La ruta solicitada no existe."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> metodo(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorRespuesta("Esa ruta no acepta el método " + e.getMethod() + "."));
    }

    /** 500: el detalle queda en el log del servidor, nunca en la respuesta. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorRespuesta("Ocurrió un error inesperado en el servidor. Intenta de nuevo en unos minutos."));
    }
}
