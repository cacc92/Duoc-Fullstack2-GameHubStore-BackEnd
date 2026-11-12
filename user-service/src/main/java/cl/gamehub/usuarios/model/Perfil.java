package cl.gamehub.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Datos de contacto y despacho de una cuenta. La cuenta se reparte entre dos servicios: auth-service
 * guarda la credencial y user-service el perfil, los dos con el mismo id y sin compartir tablas.
 */
@Entity
@Table(name = "perfiles")
public class Perfil {

    /** Asignado, sin @GeneratedValue: es el id que la cuenta ya tiene en auth-service. */
    @Id
    private Long id;

    @Column(nullable = false, length = 80)
    private String nombre;

    /** El correo de la cuenta, para mostrarlo junto a los datos de despacho. Aquí no se cambia. */
    @Column(nullable = false, length = 120)
    private String email;

    @Column(length = 9)
    private String telefono;

    @Column(length = 60)
    private String region;

    @Column(length = 60)
    private String comuna;

    @Column(length = 150)
    private String direccion;

    @Column(nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    protected Perfil() {
    }

    public Perfil(Long id, String nombre, String email, String telefono, String region, String comuna,
                  String direccion) {
        this.id = id;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        actualizar(nombre, telefono, region, comuna, direccion);
    }

    /** Cambia los datos de contacto y despacho; el correo se mantiene. */
    public void actualizar(String nombre, String telefono, String region, String comuna, String direccion) {
        this.nombre = nombre.trim();
        this.telefono = telefono.trim();
        this.region = region.trim();
        this.comuna = comuna.trim();
        this.direccion = direccion.trim();
        this.actualizadoEn = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public String getRegion() { return region; }
    public String getComuna() { return comuna; }
    public String getDireccion() { return direccion; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
