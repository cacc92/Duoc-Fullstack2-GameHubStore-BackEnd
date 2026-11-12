package cl.gamehub.usuarios.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.usuarios.dto.NuevoPerfilSolicitud;
import cl.gamehub.usuarios.dto.PerfilDto;
import cl.gamehub.usuarios.dto.PerfilSolicitud;
import cl.gamehub.usuarios.model.Perfil;
import cl.gamehub.usuarios.repository.PerfilRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilService {

    private static final Logger log = LoggerFactory.getLogger(PerfilService.class);

    private final PerfilRepository perfiles;

    public PerfilService(PerfilRepository perfiles) {
        this.perfiles = perfiles;
    }

    /** El perfil de quien llama: el id y el rol salen del token, nunca de la URL. */
    @Transactional(readOnly = true)
    public PerfilDto miPerfil(UsuarioAutenticado yo) {
        return dto(propio(yo.id()), yo.rol());
    }

    @Transactional
    public PerfilDto actualizar(UsuarioAutenticado yo, PerfilSolicitud solicitud) {
        Perfil perfil = propio(yo.id());
        perfil.actualizar(solicitud.nombre(), solicitud.telefono(), solicitud.region(), solicitud.comuna(),
                solicitud.direccion());
        log.info("Perfil {} actualizado", perfil.getId());
        return dto(perfil, yo.rol());
    }

    // ---------- Para otros microservicios ----------

    /**
     * Lo llama auth-service al registrar una cuenta. Como el id viene asignado, save() no distingue un perfil
     * nuevo de uno existente (lo sobrescribiría): por eso primero se revisa que no exista.
     */
    @Transactional
    public PerfilDto crear(NuevoPerfilSolicitud solicitud) {
        if (perfiles.existsById(solicitud.id())) {
            throw ApiException.conflicto("Ya existe un perfil para ese usuario.");
        }
        Perfil perfil = perfiles.save(new Perfil(solicitud.id(), solicitud.nombre(), solicitud.email(),
                solicitud.telefono(), solicitud.region(), solicitud.comuna(), solicitud.direccion()));
        log.info("Perfil creado para el usuario {}", perfil.getId());
        return dto(perfil, null);
    }

    @Transactional(readOnly = true)
    public PerfilDto buscarInterno(Long id) {
        Perfil perfil = perfiles.findById(id).orElseThrow(() -> ApiException.noEncontrado("No encontramos el perfil."));
        return dto(perfil, null);
    }

    private Perfil propio(Long id) {
        return perfiles.findById(id).orElseThrow(() -> ApiException.noEncontrado("No encontramos tu perfil."));
    }

    private static PerfilDto dto(Perfil p, Rol rol) {
        return new PerfilDto(p.getId(), p.getNombre(), p.getEmail(), p.getTelefono(), p.getRegion(), p.getComuna(),
                p.getDireccion(), rol);
    }
}
