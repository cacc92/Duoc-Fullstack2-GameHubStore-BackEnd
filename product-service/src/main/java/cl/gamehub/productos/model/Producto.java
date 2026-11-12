package cl.gamehub.productos.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Producto del catálogo. No tiene stock: las unidades disponibles las guarda inventory-service. */
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 60)
    private String marca;

    /**
     * Id de la categoría en category-service. Es un número y no un @ManyToOne: la categoría está en la
     * base de otro microservicio, así que aquí no hay llave foránea que la proteja.
     */
    @Column(nullable = false)
    private Long categoriaId;

    /** Línea corta para la tarjeta: "12 GB GDDR6X · PCIe 4.0". */
    @Column(length = 120)
    private String resumen;

    @Column(length = 2000)
    private String descripcion;

    /** Precio que se cobra (en pesos, sin decimales). */
    @Column(nullable = false)
    private int precio;

    /** Precio sin descuento. Si existe y es mayor que precio, el producto está en oferta. */
    private Integer precioNormal;

    /** Último día de la oferta. Después de esa fecha se vuelve a cobrar el precio normal. */
    private LocalDate ofertaHasta;

    @Column(nullable = false)
    private boolean activo = true;

    /** Ilustración ("gpu", "notebook"...) o URL de una foto. */
    @Column(length = 255)
    private String imagen;

    @ElementCollection
    @CollectionTable(name = "producto_especificaciones", joinColumns = @JoinColumn(name = "producto_id"))
    @OrderColumn(name = "posicion")
    private List<Especificacion> especificaciones = new ArrayList<>();

    protected Producto() {
    }

    public Producto(String sku, String nombre, String marca, Long categoriaId, int precio) {
        this.sku = sku;
        this.nombre = nombre;
        this.marca = marca;
        this.categoriaId = categoriaId;
        this.precio = precio;
    }

    /** Precio que se cobra hoy: si la oferta venció, vuelve el precio normal. */
    public int precioVigente(LocalDate hoy) {
        return ofertaVencida(hoy) ? precioNormal : precio;
    }

    /** Precio tachado que se muestra junto al de oferta, o null si hoy no hay oferta. */
    public Integer precioNormalVigente(LocalDate hoy) {
        return enOferta(hoy) ? precioNormal : null;
    }

    public boolean enOferta(LocalDate hoy) {
        return precioNormal != null && precioNormal > precio && !ofertaVencida(hoy);
    }

    private boolean ofertaVencida(LocalDate hoy) {
        return precioNormal != null && ofertaHasta != null && hoy.isAfter(ofertaHasta);
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public String getMarca() { return marca; }
    public Long getCategoriaId() { return categoriaId; }
    public String getResumen() { return resumen; }
    public String getDescripcion() { return descripcion; }
    public int getPrecio() { return precio; }
    public Integer getPrecioNormal() { return precioNormal; }
    public LocalDate getOfertaHasta() { return ofertaHasta; }
    public boolean isActivo() { return activo; }
    public String getImagen() { return imagen; }
    public List<Especificacion> getEspecificaciones() { return especificaciones; }

    public void setSku(String sku) { this.sku = sku; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }
    public void setResumen(String resumen) { this.resumen = resumen; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setPrecio(int precio) { this.precio = precio; }
    public void setPrecioNormal(Integer precioNormal) { this.precioNormal = precioNormal; }
    public void setOfertaHasta(LocalDate ofertaHasta) { this.ofertaHasta = ofertaHasta; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setImagen(String imagen) { this.imagen = imagen; }
}
