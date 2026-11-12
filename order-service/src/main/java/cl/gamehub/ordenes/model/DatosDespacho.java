package cl.gamehub.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Copia de los datos de entrega al momento de comprar: si el cliente cambia su dirección, la orden no cambia. */
@Embeddable
public class DatosDespacho {

    @Column(length = 80)
    private String destinatario;

    @Column(length = 120)
    private String correo;

    @Column(length = 9)
    private String telefono;

    @Column(length = 60)
    private String region;

    @Column(length = 60)
    private String comuna;

    @Column(length = 150)
    private String direccion;

    @Column(length = 60)
    private String depto;

    @Column(length = 200)
    private String indicaciones;

    protected DatosDespacho() {
    }

    public DatosDespacho(String destinatario, String correo, String telefono, String region, String comuna,
                         String direccion, String depto, String indicaciones) {
        this.destinatario = destinatario;
        this.correo = correo;
        this.telefono = telefono;
        this.region = region;
        this.comuna = comuna;
        this.direccion = direccion;
        this.depto = depto;
        this.indicaciones = indicaciones;
    }

    public String getDestinatario() { return destinatario; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getRegion() { return region; }
    public String getComuna() { return comuna; }
    public String getDireccion() { return direccion; }
    public String getDepto() { return depto; }
    public String getIndicaciones() { return indicaciones; }
}
