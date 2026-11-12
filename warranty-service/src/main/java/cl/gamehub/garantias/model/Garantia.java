package cl.gamehub.garantias.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Solicitud de garantía. La orden, el producto y el cliente viven en otros servicios: aquí se guardan sus
 * id y una copia del número de orden y de los nombres, así la bandeja del operador no llama a nadie.
 */
@Entity
@Table(name = "garantias")
public class Garantia {

    /** Regla del caso: la garantía dura 6 meses desde que se entrega la orden. */
    public static final int MESES_GARANTIA = 6;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código visible: GAR-0001. Se asigna después de guardar, cuando la base ya dio el id. */
    @Column(unique = true, length = 12)
    private String codigo;

    @Column(nullable = false)
    private Long ordenId;

    /** GH-2026-000010, el número que el cliente ve en Mis órdenes. */
    @Column(nullable = false, length = 20)
    private String ordenNumero;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 120)
    private String producto;

    @Column(nullable = false)
    private Long usuarioId;

    /** Nombre completo del cliente: el operador lo necesita para contactarlo. */
    @Column(nullable = false, length = 80)
    private String cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private MotivoGarantia motivo;

    @Column(nullable = false, length = 600)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoGarantia estado = EstadoGarantia.ABIERTA;

    @Column(length = 600)
    private String resolucion;

    /** Último día para pedir garantía de este producto. */
    @Column(nullable = false)
    private LocalDate plazo;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    private LocalDateTime actualizadaEn;

    protected Garantia() {
    }

    public Garantia(Long ordenId, String ordenNumero, Long usuarioId, String cliente, Long productoId, String producto,
                    MotivoGarantia motivo, String descripcion, LocalDate plazo, LocalDateTime creadaEn) {
        this.ordenId = ordenId;
        this.ordenNumero = ordenNumero;
        this.usuarioId = usuarioId;
        this.cliente = cliente;
        this.productoId = productoId;
        this.producto = producto;
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.plazo = plazo;
        this.creadaEn = creadaEn;
    }

    /** El plazo vence MESES_GARANTIA meses después del día de la entrega. */
    public static LocalDate plazoDesde(LocalDateTime entregadaEn) {
        return entregadaEn.toLocalDate().plusMonths(MESES_GARANTIA);
    }

    /** GAR-0001: se llama después de guardar, porque usa el id que asignó la base. */
    public void asignarCodigo() {
        this.codigo = String.format("GAR-%04d", id);
    }

    public boolean cerrada() {
        return estado.cierra();
    }

    public void actualizar(EstadoGarantia nuevoEstado, String nuevaResolucion, LocalDateTime fecha) {
        this.estado = nuevoEstado;
        this.resolucion = nuevaResolucion;
        this.actualizadaEn = fecha;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public Long getOrdenId() { return ordenId; }
    public String getOrdenNumero() { return ordenNumero; }
    public Long getProductoId() { return productoId; }
    public String getProducto() { return producto; }
    public Long getUsuarioId() { return usuarioId; }
    public String getCliente() { return cliente; }
    public MotivoGarantia getMotivo() { return motivo; }
    public String getDescripcion() { return descripcion; }
    public EstadoGarantia getEstado() { return estado; }
    public String getResolucion() { return resolucion; }
    public LocalDate getPlazo() { return plazo; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public LocalDateTime getActualizadaEn() { return actualizadaEn; }
}
