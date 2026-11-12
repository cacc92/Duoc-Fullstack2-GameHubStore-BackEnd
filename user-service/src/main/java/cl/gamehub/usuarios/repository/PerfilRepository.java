package cl.gamehub.usuarios.repository;

import cl.gamehub.usuarios.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

/** findById, existsById y save de JpaRepository bastan: el perfil se busca siempre por el id de la cuenta. */
public interface PerfilRepository extends JpaRepository<Perfil, Long> {
}
