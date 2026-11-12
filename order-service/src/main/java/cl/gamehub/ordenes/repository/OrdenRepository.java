package cl.gamehub.ordenes.repository;

import cl.gamehub.ordenes.model.EstadoOrden;
import cl.gamehub.ordenes.model.Orden;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    List<Orden> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    List<Orden> findAllByOrderByCreadaEnDesc();

    List<Orden> findByEstadoInOrderByCreadaEnDesc(Collection<EstadoOrden> estados);

    long countByEstadoIn(Collection<EstadoOrden> estados);
}
