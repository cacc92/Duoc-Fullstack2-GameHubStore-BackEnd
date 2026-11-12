package cl.gamehub.resenas.repository;

import cl.gamehub.resenas.model.EstadoResena;
import cl.gamehub.resenas.model.Resena;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByProductoIdAndEstadoOrderByFechaDesc(Long productoId, EstadoResena estado);

    List<Resena> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    List<Resena> findByEstadoOrderByFechaDesc(EstadoResena estado);

    List<Resena> findAllByOrderByFechaDesc();

    boolean existsByOrdenIdAndProductoId(Long ordenId, Long productoId);

    /** Promedio y cantidad de reseñas de cada producto, contando solo las del estado indicado. */
    @Query("select new cl.gamehub.resenas.repository.PromedioProducto(r.productoId, avg(r.puntuacion), count(r)) "
            + "from Resena r where r.estado = :estado group by r.productoId order by r.productoId")
    List<PromedioProducto> promedios(@Param("estado") EstadoResena estado);

    /** Lo mismo, pero solo de los productos pedidos (el catálogo pregunta por los que está mostrando). */
    @Query("select new cl.gamehub.resenas.repository.PromedioProducto(r.productoId, avg(r.puntuacion), count(r)) "
            + "from Resena r where r.estado = :estado and r.productoId in :productos "
            + "group by r.productoId order by r.productoId")
    List<PromedioProducto> promedios(@Param("estado") EstadoResena estado,
                                     @Param("productos") Collection<Long> productos);
}
