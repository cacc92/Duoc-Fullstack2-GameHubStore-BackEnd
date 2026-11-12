package cl.gamehub.inventario.repository;

import cl.gamehub.inventario.model.Stock;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    List<Stock> findAllByOrderByProductoIdAsc();

    List<Stock> findByProductoIdInOrderByProductoIdAsc(Collection<Long> productoIds);
}
