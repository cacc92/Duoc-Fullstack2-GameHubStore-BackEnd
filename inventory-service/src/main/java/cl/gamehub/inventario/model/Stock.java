package cl.gamehub.inventario.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Unidades disponibles de un producto. sku, nombre y activo son una copia de product-service para que el
 * panel del operador no tenga que preguntarle; product-service la actualiza cada vez que cambian.
 */
@Entity
@Table(name = "stock")
public class Stock {

    /** El mismo id del producto en product-service: se asigna, no se genera. */
    @Id
    private Long productoId;

    @Column(nullable = false, length = 30)
    private String sku;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private boolean activo = true;

    /** Unidades en bodega. Nunca queda negativo. */
    @Column(nullable = false)
    private int cantidad;

    protected Stock() {
    }

    public Stock(Long productoId, String sku, String nombre, boolean activo, int cantidad) {
        this.productoId = productoId;
        this.sku = sku;
        this.nombre = nombre;
        this.activo = activo;
        this.cantidad = cantidad;
    }

    /** product-service avisó que cambiaron los datos del producto. */
    public void actualizarProducto(String sku, String nombre, boolean activo) {
        this.sku = sku;
        this.nombre = nombre;
        this.activo = activo;
    }

    public Long getProductoId() { return productoId; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
    public int getCantidad() { return cantidad; }

    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
}
