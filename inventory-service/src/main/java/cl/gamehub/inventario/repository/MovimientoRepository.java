package cl.gamehub.inventario.repository;

import cl.gamehub.inventario.model.Movimiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    /** Los 30 más recientes; si dos tienen la misma fecha, primero el último registrado. */
    List<Movimiento> findTop30ByOrderByFechaDescIdDesc();
}
