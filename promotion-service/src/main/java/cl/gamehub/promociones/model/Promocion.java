package cl.gamehub.promociones.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** Cupón de descuento por porcentaje, con tope en pesos, vigencia y tope de usos. */
@Entity
@Table(name = "promociones")
public class Promocion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Siempre en mayúsculas: GAMER10. */
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false)
    private int porcentaje;

    /** Descuento máximo en pesos. */
    @Column(nullable = false)
    private int tope;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column(nullable = false)
    private LocalDate termino;

    @Column(nullable = false)
    private int usosMaximos;

    @Column(nullable = false)
    private int usos;

    @Column(nullable = false)
    private boolean activa = true;

    protected Promocion() {
    }

    public Promocion(String codigo, int porcentaje, int tope, LocalDate inicio, LocalDate termino, int usosMaximos) {
        this.codigo = codigo;
        this.porcentaje = porcentaje;
        this.tope = tope;
        this.inicio = inicio;
        this.termino = termino;
        this.usosMaximos = usosMaximos;
    }

    /** Porcentaje del subtotal, sin pasar del tope ni del subtotal: el total nunca queda negativo. */
    public int calcularDescuento(int subtotal) {
        if (subtotal <= 0) {
            return 0;
        }
        int descuento = (int) Math.round(subtotal * (double) porcentaje / 100);
        return Math.min(Math.min(descuento, tope), subtotal);
    }

    public void registrarUso() {
        usos++;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public int getPorcentaje() { return porcentaje; }
    public int getTope() { return tope; }
    public LocalDate getInicio() { return inicio; }
    public LocalDate getTermino() { return termino; }
    public int getUsosMaximos() { return usosMaximos; }
    public int getUsos() { return usos; }
    public boolean isActiva() { return activa; }

    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setPorcentaje(int porcentaje) { this.porcentaje = porcentaje; }
    public void setTope(int tope) { this.tope = tope; }
    public void setInicio(LocalDate inicio) { this.inicio = inicio; }
    public void setTermino(LocalDate termino) { this.termino = termino; }
    public void setUsosMaximos(int usosMaximos) { this.usosMaximos = usosMaximos; }
    public void setUsos(int usos) { this.usos = usos; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
