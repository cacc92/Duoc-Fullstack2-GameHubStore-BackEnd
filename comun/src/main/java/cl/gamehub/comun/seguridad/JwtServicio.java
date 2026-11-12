package cl.gamehub.comun.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Firma y verifica los JWT con HMAC-SHA (jjwt elige HS256, HS384 o HS512 según el largo del secreto).
 * Solo auth-service llama a generar(); los demás servicios usan validar() con el mismo JWT_SECRETO.
 */
@Component
public class JwtServicio {

    private final SecretKey clave;
    private final long minutos;

    public JwtServicio(@Value("${app.jwt.secreto}") String secreto,
                       @Value("${app.jwt.expiracion-minutos:120}") long minutos) {
        // La clave debe tener al menos 32 bytes (256 bits); si es más corta, el servicio no arranca
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.minutos = minutos;
    }

    public String generar(Long id, String email, String nombre, Rol rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("id", id)
                .claim("nombre", nombre)
                .claim("rol", rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutos, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    /** Verifica la firma y el vencimiento. Si el token no sirve, lanza una JwtException. */
    public UsuarioAutenticado validar(String token) {
        Claims claims = Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).getPayload();
        Number id = claims.get("id", Number.class);
        return new UsuarioAutenticado(
                id.longValue(),
                claims.getSubject(),
                claims.get("nombre", String.class),
                Rol.valueOf(claims.get("rol", String.class)));
    }
}
