package cl.gamehub.garantias.repository;

import cl.gamehub.garantias.model.EstadoGarantia;
import cl.gamehub.garantias.model.Garantia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GarantiaRepository extends JpaRepository<Garantia, Long> {

    List<Garantia> findByUsuarioIdOrderByCreadaEnDesc(Long usuarioId);

    List<Garantia> findAllByOrderByCreadaEnDesc();

    /** ¿Hay una solicitud en alguno de estos estados para este producto de esta orden? */
    boolean existsByOrdenIdAndProductoIdAndEstadoIn(Long ordenId, Long productoId, Collection<EstadoGarantia> estados);

    long countByEstadoIn(Collection<EstadoGarantia> estados);
}
