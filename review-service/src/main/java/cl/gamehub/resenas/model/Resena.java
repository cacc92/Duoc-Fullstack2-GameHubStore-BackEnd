package cl.gamehub.resenas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/**
 * Una reseña por producto y por orden: lo asegura la restricción única de la tabla. La orden, el cliente
 * y el producto viven en otros servicios; aquí se guardan sus id y una copia del nombre del producto y
 * del autor abreviado, así mostrar una reseña no obliga a llamar a nadie.
 */
@Entity
@Table(name = "resenas", uniqueConstraints = @UniqueConstraint(columnNames = {"orden_id", "producto_id"}))
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    /** Nombre del producto tal como aparece en la orden. */
    @Column(nullable = false, length = 120)
    private String producto;

    @Column(name = "orden_id", nullable = false)
    private Long ordenId;

    @Column(nullable = false)
    private Long usuarioId;

    /** "Martina C.": nombre abreviado con Formatos.nombreCorto para no exponer el apellido. */
    @Column(nullable = false, length = 80)
    private String autor;

    @Column(nullable = false)
    private int puntuacion;

    @Column(nullable = false, length = 500)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoResena estado = EstadoResena.PENDIENTE;

    protected Resena() {
    }

    public Resena(Long ordenId, Long usuarioId, String autor, Long productoId, String producto, int puntuacion,
                  String comentario, LocalDateTime fecha) {
        this.ordenId = ordenId;
        this.usuarioId = usuarioId;
        this.autor = autor;
        this.productoId = productoId;
        this.producto = producto;
        this.puntuacion = puntuacion;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    /** El administrador la aprueba (se publica en la tienda) o la oculta. */
    public void moderar(EstadoResena nuevoEstado) {
        this.estado = nuevoEstado;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public String getProducto() { return producto; }
    public Long getOrdenId() { return ordenId; }
    public Long getUsuarioId() { return usuarioId; }
    public String getAutor() { return autor; }
    public int getPuntuacion() { return puntuacion; }
    public String getComentario() { return comentario; }
    public LocalDateTime getFecha() { return fecha; }
    public EstadoResena getEstado() { return estado; }
}
