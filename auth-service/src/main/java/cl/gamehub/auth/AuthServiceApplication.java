package cl.gamehub.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * auth-service: credenciales (correo, hash de la contraseña, rol y estado), inicio de sesión, registro y
 * emisión del JWT que después validan los demás servicios. Al registrar una cuenta le pide a user-service
 * que cree el perfil con el mismo id.
 * scanBasePackages = "cl.gamehub" incluye también las clases del módulo comun (errores, JWT, Swagger).
 */
@SpringBootApplication(scanBasePackages = "cl.gamehub")
@EnableFeignClients
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
