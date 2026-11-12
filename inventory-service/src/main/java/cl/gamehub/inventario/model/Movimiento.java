package cl.gamehub.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Registro de cada cambio de stock: quién, cuándo, por qué y cómo quedó. */
@Entity
@Table(name = "movimientos")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productoId;

    /** Nombre del producto cuando se registró el movimiento. */
    @Column(nullable = false, length = 120)
    private String producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoMovimiento tipo;

    /** Con signo: positivo si entra mercadería, negativo si sale. */
    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false, length = 120)
    private String motivo;

    @Column(nullable = false, length = 80)
    private String usuario;

    @Column(nullable = false)
    private int stockResultante;

    @Column(nullable = false)
    private LocalDateTime fecha;

    protected Movimiento() {
    }

    /** Se crea después de cambiar el stock: stockResultante es la cantidad con que quedó el producto. */
    public Movimiento(Stock stock, TipoMovimiento tipo, int cantidad, String motivo, String usuario, LocalDateTime fecha) {
        this.productoId = stock.getProductoId();
        this.producto = stock.getNombre();
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.usuario = usuario;
        this.stockResultante = stock.getCantidad();
        this.fecha = fecha;
    }

    public Long getId() { return id; }
    public Long getProductoId() { return productoId; }
    public String getProducto() { return producto; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public String getMotivo() { return motivo; }
    public String getUsuario() { return usuario; }
    public int getStockResultante() { return stockResultante; }
    public LocalDateTime getFecha() { return fecha; }
}
