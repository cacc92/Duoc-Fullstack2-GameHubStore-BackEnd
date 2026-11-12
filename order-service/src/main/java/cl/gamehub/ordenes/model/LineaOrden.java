package cl.gamehub.ordenes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Línea de la orden. El producto vive en product-service, así que aquí se guarda su id y una copia del
 * nombre, la imagen y el precio al momento de comprar: si después cambian en el catálogo, la orden no cambia.
 */
@Entity
@Table(name = "lineas_orden")
public class LineaOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Orden orden;

    @Column(nullable = false)
    private Long productoId;

    @Column(nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String imagen;

    @Column(nullable = false)
    private int precioUnitario;

    @Column(nullable = false)
    private int cantidad;

    protected LineaOrden() {
    }

    LineaOrden(Orden orden, Long productoId, String sku, String nombre, String imagen, int precioUnitario, int cantidad) {
        this.orden = orden;
        this.productoId = productoId;
        this.sku = sku;
        this.nombre = nombre;
        this.imagen = imagen;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
    }

    public int getSubtotal() {
        return precioUnitario * cantidad;
    }

    public Long getId() { return id; }
    public Orden getOrden() { return orden; }
    public Long getProductoId() { return productoId; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public String getImagen() { return imagen; }
    public int getPrecioUnitario() { return precioUnitario; }
    public int getCantidad() { return cantidad; }
}
