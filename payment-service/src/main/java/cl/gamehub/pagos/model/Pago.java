package cl.gamehub.pagos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * Pago simulado de una orden. La orden vive en order-service: aquí solo se guarda su id y su número
 * (sin relación JPA, porque cada microservicio tiene su propia base).
 */
@Entity
@Table(name = "pagos")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Una orden tiene un solo pago. */
    @Column(nullable = false, unique = true)
    private Long ordenId;

    @Column(nullable = false, length = 20)
    private String ordenNumero;

    /** Dueño de la orden (id de auth-service): con él se arma «Mis pagos». */
    @Column(nullable = false)
    private Long usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodo;

    @Column(nullable = false)
    private int monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(nullable = false, length = 160)
    private String detalle;

    /** Fecha del último cambio de estado. */
    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    protected Pago() {
    }

    public Pago(Long ordenId, String ordenNumero, Long usuarioId, MetodoPago metodo, int monto, EstadoPago estado,
                String detalle, LocalDateTime creadoEn) {
        this.ordenId = ordenId;
        this.ordenNumero = ordenNumero;
        this.usuarioId = usuarioId;
        this.metodo = metodo;
        this.monto = monto;
        this.estado = estado;
        this.detalle = detalle;
        this.fecha = creadoEn;
        this.creadoEn = creadoEn;
    }

    public void cambiar(EstadoPago estado, String detalle, LocalDateTime fecha) {
        this.estado = estado;
        this.detalle = detalle;
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getOrdenId() { return ordenId; }
    public String getOrdenNumero() { return ordenNumero; }
    public Long getUsuarioId() { return usuarioId; }
    public MetodoPago getMetodo() { return metodo; }
    public int getMonto() { return monto; }
    public EstadoPago getEstado() { return estado; }
    public String getDetalle() { return detalle; }
    public LocalDateTime getFecha() { return fecha; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
}
