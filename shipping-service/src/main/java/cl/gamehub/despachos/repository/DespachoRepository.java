package cl.gamehub.despachos.repository;

import cl.gamehub.despachos.model.Despacho;
import cl.gamehub.despachos.model.EstadoDespacho;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DespachoRepository extends JpaRepository<Despacho, Long> {

    Optional<Despacho> findByOrdenId(Long ordenId);

    Optional<Despacho> findBySeguimiento(String seguimiento);

    boolean existsByOrdenId(Long ordenId);

    boolean existsBySeguimiento(String seguimiento);

    boolean existsBySeguimientoAndIdNot(String seguimiento, Long id);

    List<Despacho> findByOrdenIdInOrderByOrdenIdAsc(Collection<Long> ordenIds);

    List<Despacho> findAllByOrderByCreadoEnDesc();

    List<Despacho> findByEstadoOrderByCreadoEnDesc(EstadoDespacho estado);
}
