package cl.gamehub.auth.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign hacia user-service, que guarda el perfil (teléfono y dirección de despacho) con el mismo id
 * que la credencial. La cabecera X-Clave-Interna la agrega FeignInternoConfig (comun) y, si user-service
 * responde con error, llega aquí como ApiException con el mismo código y el mismo mensaje.
 */
@FeignClient(name = "user-service", contextId = "userClient", path = "/internal/perfiles")
public interface UserClient {

    /** Crea el perfil de una cuenta recién registrada. auth-service no usa el perfil que responde. */
    @PostMapping
    void crearPerfil(@RequestBody NuevoPerfilSolicitud perfil);
}
