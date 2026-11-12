package cl.gamehub.productos.repository;

import cl.gamehub.productos.model.Producto;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByActivoTrueOrderByIdAsc();

    List<Producto> findByActivoTrueAndCategoriaIdOrderByIdAsc(Long categoriaId);

    List<Producto> findAllByOrderByIdAsc();

    List<Producto> findByIdInOrderByIdAsc(Collection<Long> ids);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    /** Cuántos productos tiene cada categoría. Con soloActivos = false cuenta también los inactivos. */
    @Query("""
            select p.categoriaId as categoriaId, count(p) as cantidad
            from Producto p
            where p.activo = true or :soloActivos = false
            group by p.categoriaId""")
    List<ConteoCategoria> contarPorCategoria(@Param("soloActivos") boolean soloActivos);

    /** Una fila del conteo. Spring Data la arma con los alias de la consulta (categoriaId, cantidad). */
    interface ConteoCategoria {

        Long getCategoriaId();

        Long getCantidad();
    }
}
