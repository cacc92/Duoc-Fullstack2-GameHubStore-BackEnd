package cl.gamehub.auth.repository;

import cl.gamehub.auth.model.Credencial;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CredencialRepository extends JpaRepository<Credencial, Long> {

    Optional<Credencial> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Para el panel de administración: por rol (ADMIN, CLIENTE, OPERADOR) y, dentro de cada rol, por nombre. */
    List<Credencial> findAllByOrderByRolAscNombreAsc();
}
