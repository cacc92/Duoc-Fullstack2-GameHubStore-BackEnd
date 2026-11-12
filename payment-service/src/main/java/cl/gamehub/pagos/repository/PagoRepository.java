package cl.gamehub.pagos.repository;

import cl.gamehub.pagos.model.EstadoPago;
import cl.gamehub.pagos.model.Pago;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByOrdenId(Long ordenId);

    boolean existsByOrdenId(Long ordenId);

    List<Pago> findByOrdenIdInOrderByOrdenIdAsc(Collection<Long> ordenIds);

    List<Pago> findByUsuarioIdOrderByFechaDesc(Long usuarioId);

    List<Pago> findAllByOrderByFechaDesc();

    List<Pago> findByEstadoOrderByFechaDesc(EstadoPago estado);
}
