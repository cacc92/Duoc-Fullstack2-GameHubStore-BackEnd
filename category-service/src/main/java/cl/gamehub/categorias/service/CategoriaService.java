package cl.gamehub.categorias.service;

import cl.gamehub.categorias.client.ProductoClient;
import cl.gamehub.categorias.dto.CategoriaDto;
import cl.gamehub.categorias.dto.CategoriaInternaDto;
import cl.gamehub.categorias.dto.CategoriaSolicitud;
import cl.gamehub.categorias.model.Categoria;
import cl.gamehub.categorias.repository.CategoriaRepository;
import cl.gamehub.comun.error.ApiException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private static final Logger log = LoggerFactory.getLogger(CategoriaService.class);

    private final CategoriaRepository categorias;
    private final ProductoClient productos;

    public CategoriaService(CategoriaRepository categorias, ProductoClient productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    /** soloActivos = true en el catálogo público; false en el panel de administración. */
    @Transactional(readOnly = true)
    public List<CategoriaDto> listar(boolean soloActivos) {
        Map<Long, Long> conteo = productos.conteoPorCategoria(soloActivos);
        return categorias.findAllByOrderByIdAsc().stream()
                .map(c -> dto(c, conteo.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Transactional
    public CategoriaDto crear(CategoriaSolicitud solicitud) {
        String nombre = solicitud.nombre().trim();
        if (categorias.existsByNombreIgnoreCase(nombre)) {
            throw ApiException.conflicto("La categoría «" + nombre + "» ya existe.");
        }
        Categoria categoria = categorias.save(new Categoria(nombre));
        log.info("Categoría creada: {} ({})", categoria.getNombre(), categoria.getSlug());
        return dto(categoria, 0);
    }

    @Transactional
    public CategoriaDto renombrar(Long id, CategoriaSolicitud solicitud) {
        Categoria categoria = buscar(id);
        String nombre = solicitud.nombre().trim();
        if (categorias.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw ApiException.conflicto("La categoría «" + nombre + "» ya existe.");
        }
        categoria.renombrar(nombre);
        log.info("Categoría {} renombrada a {}", id, nombre);
        return dto(categoria, productos.conteoPorCategoria(false).getOrDefault(id, 0L));
    }

    /** Regla del caso: una categoría con productos (activos o no) no se elimina. */
    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = buscar(id);
        if (productos.conteoPorCategoria(false).getOrDefault(id, 0L) > 0) {
            throw ApiException.conflicto("No puedes eliminar una categoría que tiene productos.");
        }
        categorias.delete(categoria);
        log.info("Categoría eliminada: {}", categoria.getNombre());
    }

    // ---------- Para otros microservicios ----------

    @Transactional(readOnly = true)
    public List<CategoriaInternaDto> listarInterno() {
        return categorias.findAllByOrderByIdAsc().stream().map(CategoriaService::interno).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaInternaDto buscarInterno(Long id) {
        return interno(buscar(id));
    }

    private Categoria buscar(Long id) {
        return categorias.findById(id).orElseThrow(() -> ApiException.noEncontrado("La categoría no existe."));
    }

    private static CategoriaDto dto(Categoria c, long cantidad) {
        return new CategoriaDto(c.getId(), c.getNombre(), c.getSlug(), cantidad);
    }

    private static CategoriaInternaDto interno(Categoria c) {
        return new CategoriaInternaDto(c.getId(), c.getNombre(), c.getSlug());
    }
}
