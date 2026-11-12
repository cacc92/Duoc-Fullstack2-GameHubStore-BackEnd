package cl.gamehub.despachos.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Despacho de una orden. La orden vive en order-service: aquí se guarda su id, su número y una copia de los
 * datos de entrega (sin relación JPA, porque cada microservicio tiene su propia base).
 */
@Entity
@Table(name = "despachos")
public class Despacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Una orden tiene un solo despacho. */
    @Column(nullable = false, unique = true)
    private Long ordenId;

    @Column(nullable = false, length = 20)
    private String ordenNumero;

    /** Código con que el cliente sigue su pedido: GHX-61873. Siempre en mayúsculas. */
    @Column(nullable = false, unique = true, length = 30)
    private String seguimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDespacho estado;

    @Column(nullable = false)
    private LocalDate fechaEstimada;

    /** Solo cuando llega a ENTREGADO. */
    private LocalDateTime entregadoEn;

    @Column(length = 80)
    private String destinatario;

    @Column(length = 150)
    private String direccion;

    @Column(length = 60)
    private String comuna;

    @Column(length = 60)
    private String region;

    @Column(nullable = false)
    private LocalDateTime creadoEn;

    @ElementCollection
    @CollectionTable(name = "despacho_eventos", joinColumns = @JoinColumn(name = "despacho_id"))
    @OrderColumn(name = "posicion")
    private List<EventoDespacho> eventos = new ArrayList<>();

    protected Despacho() {
    }

    /** Todo despacho nace en PREPARANDO, con ese primer evento en la fecha de creación. */
    public Despacho(Long ordenId, String ordenNumero, String seguimiento, LocalDate fechaEstimada, String destinatario,
                    String direccion, String comuna, String region, LocalDateTime creadoEn) {
        this.ordenId = ordenId;
        this.ordenNumero = ordenNumero;
        this.seguimiento = seguimiento;
        this.fechaEstimada = fechaEstimada;
        this.destinatario = destinatario;
        this.direccion = direccion;
        this.comuna = comuna;
        this.region = region;
        this.creadoEn = creadoEn;
        this.estado = EstadoDespacho.PREPARANDO;
        eventos.add(new EventoDespacho(EstadoDespacho.PREPARANDO, creadoEn));
    }

    /** Cambia el estado y deja el evento. Que solo avance lo revisa DespachoService antes de llamar aquí. */
    public void avanzar(EstadoDespacho nuevoEstado, LocalDateTime fecha) {
        this.estado = nuevoEstado;
        if (nuevoEstado == EstadoDespacho.ENTREGADO) {
            this.entregadoEn = fecha;
        }
        eventos.add(new EventoDespacho(nuevoEstado, fecha));
    }

    public Long getId() { return id; }
    public Long getOrdenId() { return ordenId; }
    public String getOrdenNumero() { return ordenNumero; }
    public String getSeguimiento() { return seguimiento; }
    public EstadoDespacho getEstado() { return estado; }
    public LocalDate getFechaEstimada() { return fechaEstimada; }
    public LocalDateTime getEntregadoEn() { return entregadoEn; }
    public String getDestinatario() { return destinatario; }
    public String getDireccion() { return direccion; }
    public String getComuna() { return comuna; }
    public String getRegion() { return region; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public List<EventoDespacho> getEventos() { return eventos; }

    public void setSeguimiento(String seguimiento) { this.seguimiento = seguimiento; }
    public void setFechaEstimada(LocalDate fechaEstimada) { this.fechaEstimada = fechaEstimada; }
}
