package cl.gamehub.productos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Fila de la tabla de especificaciones técnicas: clave "Memoria", valor "12 GB GDDR6X". */
@Embeddable
public class Especificacion {

    @Column(nullable = false, length = 60)
    private String clave;

    @Column(nullable = false, length = 160)
    private String valor;

    protected Especificacion() {
    }

    public Especificacion(String clave, String valor) {
        this.clave = clave;
        this.valor = valor;
    }

    public String getClave() { return clave; }
    public String getValor() { return valor; }
}
