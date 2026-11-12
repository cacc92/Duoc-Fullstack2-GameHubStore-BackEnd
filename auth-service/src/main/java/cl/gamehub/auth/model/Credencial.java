package cl.gamehub.auth.model;

import cl.gamehub.comun.seguridad.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Lo que auth-service necesita para iniciar sesión: correo, hash de la contraseña, rol y si la cuenta
 * está activa. El teléfono y la dirección de despacho son del perfil, que guarda user-service con este
 * mismo id.
 */
@Entity
@Table(name = "credenciales")
public class Credencial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Siempre en minúsculas: Camila@GameHub.test y camila@gamehub.test son la misma cuenta. */
    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false, length = 80)
    private String nombre;

    /** Nunca se guarda la contraseña: solo su hash BCrypt. */
    @Column(nullable = false)
    private String claveHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    protected Credencial() {
    }

    public Credencial(String nombre, String email, String claveHash, Rol rol) {
        this.nombre = nombre.trim();
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.claveHash = claveHash;
        this.rol = rol;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNombre() { return nombre; }
    public String getClaveHash() { return claveHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }

    public void setActivo(boolean activo) { this.activo = activo; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
