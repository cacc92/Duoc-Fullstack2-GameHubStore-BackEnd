package cl.gamehub.categorias.model;

import cl.gamehub.comun.util.Formatos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nombre;

    /** Versión para URL: /catalogo?categoria=tarjetas-graficas */
    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    protected Categoria() {
    }

    public Categoria(String nombre) {
        renombrar(nombre);
    }

    public void renombrar(String nombre) {
        this.nombre = nombre.trim();
        this.slug = Formatos.slug(nombre);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getSlug() { return slug; }
}
