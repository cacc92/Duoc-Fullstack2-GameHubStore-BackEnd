package cl.gamehub.ordenes.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de compra. El cliente se guarda por id (vive en auth-service) con una copia de su nombre; el
 * pago y el despacho viven en payment-service y shipping-service, y aquí queda el estado que resulta
 * de ellos. La tabla se llama "ordenes" porque "order" es palabra reservada en SQL.
 */
@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Número visible para el cliente: GH-2026-000123. Se asigna después de guardar. */
    @Column(unique = true, length = 20)
    private String numero;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 80)
    private String cliente;

    @Column(nullable = false)
    private LocalDateTime creadaEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoOrden estado = EstadoOrden.PENDIENTE_PAGO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(nullable = false)
    private int subtotal;

    @Column(nullable = false)
    private int descuento;

    @Column(nullable = false)
    private int total;

    @Column(length = 30)
    private String cupon;

    @Embedded
    private DatosDespacho datosDespacho;

    /** Cuándo se entregó: desde aquí corre el plazo de garantía. */
    private LocalDateTime entregadaEn;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LineaOrden> lineas = new ArrayList<>();

    protected Orden() {
    }

    public Orden(Long usuarioId, String cliente, DatosDespacho datosDespacho, MetodoPago metodoPago, LocalDateTime creadaEn) {
        this.usuarioId = usuarioId;
        this.cliente = cliente;
        this.datosDespacho = datosDespacho;
        this.metodoPago = metodoPago;
        this.creadaEn = creadaEn;
    }

    public void agregarLinea(Long productoId, String sku, String nombre, String imagen, int precioUnitario, int cantidad) {
        lineas.add(new LineaOrden(this, productoId, sku, nombre, imagen, precioUnitario, cantidad));
    }

    public void fijarTotales(int subtotal, int descuento, String cupon) {
        this.subtotal = subtotal;
        this.descuento = descuento;
        this.total = Math.max(subtotal - descuento, 0);
        this.cupon = cupon;
    }

    /** GH-2026-000123: se llama después de guardar, cuando la orden ya tiene id. */
    public void asignarNumero() {
        this.numero = "GH-" + creadaEn.getYear() + "-" + String.format("%06d", id);
    }

    /** payment-service informó un estado de pago nuevo. */
    public void aplicarPago(String estadoPago) {
        this.estado = EstadoOrden.desdePago(estadoPago);
    }

    /** shipping-service informó un estado de despacho nuevo. */
    public void aplicarDespacho(String estadoDespacho, LocalDateTime entregadoEn) {
        this.estado = EstadoOrden.desdeDespacho(estadoDespacho);
        if (this.estado == EstadoOrden.ENTREGADA) {
            this.entregadaEn = entregadoEn;
        }
    }

    /** Solo para los datos de ejemplo, que ya traen el estado final de cada orden. */
    public void restaurar(EstadoOrden estado, LocalDateTime entregadaEn) {
        this.estado = estado;
        this.entregadaEn = entregadaEn;
    }

    public int getUnidades() {
        return lineas.stream().mapToInt(LineaOrden::getCantidad).sum();
    }

    public Long getId() { return id; }
    public String getNumero() { return numero; }
    public Long getUsuarioId() { return usuarioId; }
    public String getCliente() { return cliente; }
    public LocalDateTime getCreadaEn() { return creadaEn; }
    public EstadoOrden getEstado() { return estado; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public int getSubtotal() { return subtotal; }
    public int getDescuento() { return descuento; }
    public int getTotal() { return total; }
    public String getCupon() { return cupon; }
    public DatosDespacho getDatosDespacho() { return datosDespacho; }
    public LocalDateTime getEntregadaEn() { return entregadaEn; }
    public List<LineaOrden> getLineas() { return lineas; }
}
