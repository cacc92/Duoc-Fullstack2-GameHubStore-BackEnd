package cl.gamehub.productos.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.productos.client.CategoriaClient;
import cl.gamehub.productos.client.InventarioClient;
import cl.gamehub.productos.dto.CategoriaInternaDto;
import cl.gamehub.productos.dto.EspecificacionDto;
import cl.gamehub.productos.dto.ProductoAdminDto;
import cl.gamehub.productos.dto.ProductoDetalleDto;
import cl.gamehub.productos.dto.ProductoInternoDto;
import cl.gamehub.productos.dto.ProductoInventarioSolicitud;
import cl.gamehub.productos.dto.ProductoResumenDto;
import cl.gamehub.productos.dto.ProductoSolicitud;
import cl.gamehub.productos.dto.StockNuevoSolicitud;
import cl.gamehub.productos.model.Especificacion;
import cl.gamehub.productos.model.Producto;
import cl.gamehub.productos.repository.ProductoRepository;
import cl.gamehub.productos.repository.ProductoRepository.ConteoCategoria;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    /** Por si category-service ya no tiene la categoría de un producto: el producto se muestra igual, sin ella. */
    private static final CategoriaInternaDto SIN_CATEGORIA = new CategoriaInternaDto(null, null, null);

    private final ProductoRepository productos;
    private final CategoriaClient categorias;
    private final InventarioClient inventario;

    public ProductoService(ProductoRepository productos, CategoriaClient categorias, InventarioClient inventario) {
        this.productos = productos;
        this.categorias = categorias;
        this.inventario = inventario;
    }

    // ---------- Catálogo público ----------

    /** Solo productos activos. El filtro por marca, precio y orden lo hace la vista del catálogo. */
    @Transactional(readOnly = true)
    public List<ProductoResumenDto> listar(String categoria) {
        // Una sola llamada a category-service trae el nombre y el slug de todas las categorías
        Map<Long, CategoriaInternaDto> porId = categoriasPorId();
        List<Producto> lista;
        if (categoria == null || categoria.isBlank()) {
            lista = productos.findByActivoTrueOrderByIdAsc();
        } else {
            CategoriaInternaDto elegida = porId.values().stream()
                    .filter(c -> categoria.trim().equals(c.slug()))
                    .findFirst()
                    .orElse(null);
            if (elegida == null) {
                return List.of(); // slug desconocido: no hay productos que mostrar
            }
            lista = productos.findByActivoTrueAndCategoriaIdOrderByIdAsc(elegida.id());
        }
        LocalDate hoy = LocalDate.now();
        return lista.stream()
                .map(p -> resumen(p, porId.getOrDefault(p.getCategoriaId(), SIN_CATEGORIA), hoy))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductoDetalleDto detalle(Long id) {
        Producto p = productos.findById(id).filter(Producto::isActivo)
                .orElseThrow(() -> ApiException.noEncontrado("El producto no existe o ya no está disponible."));
        CategoriaInternaDto c = categoriasPorId().getOrDefault(p.getCategoriaId(), SIN_CATEGORIA);
        LocalDate hoy = LocalDate.now();
        return new ProductoDetalleDto(p.getId(), p.getSku(), p.getNombre(), p.getMarca(), c.nombre(), c.slug(),
                p.precioVigente(hoy), p.precioNormalVigente(hoy), oferta(p, hoy), p.getImagen(), p.getResumen(),
                p.getDescripcion(), especificaciones(p));
    }

    // ---------- Administración ----------

    /** Todos los productos, activos e inactivos, con los precios guardados (no los vigentes de hoy). */
    @Transactional(readOnly = true)
    public List<ProductoAdminDto> listarTodos() {
        Map<Long, CategoriaInternaDto> porId = categoriasPorId();
        return productos.findAllByOrderByIdAsc().stream()
                .map(p -> admin(p, porId.getOrDefault(p.getCategoriaId(), SIN_CATEGORIA).nombre()))
                .toList();
    }

    /**
     * Guarda el producto y después le pide a inventory-service que cree su stock. Todo ocurre dentro de una
     * transacción: si inventory-service responde con error, la excepción revierte el producto guardado.
     */
    @Transactional
    public ProductoAdminDto crear(ProductoSolicitud s) {
        String sku = s.sku().trim().toUpperCase(Locale.ROOT);
        if (productos.existsBySkuIgnoreCase(sku)) {
            throw ApiException.conflicto("Ya existe un producto con el SKU " + sku + ".");
        }
        CategoriaInternaDto categoria = categoriaElegida(s.categoriaId());
        validarPrecios(s);
        Producto p = new Producto(sku, s.nombre().trim(), s.marca().trim(), categoria.id(), s.precio());
        completar(p, s, categoria);
        p = productos.save(p);
        int stockInicial = s.stock() == null ? 0 : s.stock();
        inventario.crearStock(new StockNuevoSolicitud(p.getId(), p.getSku(), p.getNombre(), p.isActivo(), stockInicial));
        log.info("Producto creado: {} {} en {} (stock inicial {})", p.getId(), sku, categoria.nombre(), stockInicial);
        return admin(p, categoria.nombre());
    }

    /** El stock no se toca aquí: se ajusta desde el panel del operador y queda registrado en inventory-service. */
    @Transactional
    public ProductoAdminDto actualizar(Long id, ProductoSolicitud s) {
        Producto p = buscar(id);
        String sku = s.sku().trim().toUpperCase(Locale.ROOT);
        if (productos.existsBySkuIgnoreCaseAndIdNot(sku, id)) {
            throw ApiException.conflicto("Ya existe otro producto con el SKU " + sku + ".");
        }
        CategoriaInternaDto categoria = categoriaElegida(s.categoriaId());
        validarPrecios(s);
        p.setSku(sku);
        p.setNombre(s.nombre().trim());
        p.setMarca(s.marca().trim());
        p.setPrecio(s.precio());
        completar(p, s, categoria);
        avisarInventario(p);
        log.info("Producto actualizado: {} {}", id, sku);
        return admin(p, categoria.nombre());
    }

    @Transactional
    public ProductoAdminDto cambiarActivo(Long id, boolean activo) {
        Producto p = buscar(id);
        // Primero lo que puede fallar sin efectos (leer la categoría) y al final el aviso a inventory-service
        String categoria = categoriasPorId().getOrDefault(p.getCategoriaId(), SIN_CATEGORIA).nombre();
        p.setActivo(activo);
        avisarInventario(p);
        log.info("Producto {} {}", id, activo ? "activado" : "desactivado");
        return admin(p, categoria);
    }

    // ---------- Para otros microservicios ----------

    /** Para order-service: precio vigente hoy. Los id que no existen se omiten; los inactivos vienen con activo = false. */
    @Transactional(readOnly = true)
    public List<ProductoInternoDto> buscarVarios(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now();
        return productos.findByIdInOrderByIdAsc(ids).stream()
                .map(p -> new ProductoInternoDto(p.getId(), p.getSku(), p.getNombre(), p.getImagen(),
                        p.precioVigente(hoy), p.precioNormalVigente(hoy), p.isActivo()))
                .toList();
    }

    /** Para category-service: { id de categoría: cantidad de productos }. */
    @Transactional(readOnly = true)
    public Map<Long, Long> conteoPorCategoria(boolean soloActivos) {
        Map<Long, Long> conteo = new TreeMap<>();
        for (ConteoCategoria fila : productos.contarPorCategoria(soloActivos)) {
            conteo.put(fila.getCategoriaId(), fila.getCantidad());
        }
        return conteo;
    }

    // ---------- Apoyo ----------

    private Producto buscar(Long id) {
        return productos.findById(id).orElseThrow(() -> ApiException.noEncontrado("El producto no existe."));
    }

    /**
     * Nombre y slug de todas las categorías con una sola llamada a category-service. Para mostrar nombres se
     * usa esta lista y no buscar(id): si category-service ya no tuviera una categoría, el producto se ve igual.
     */
    private Map<Long, CategoriaInternaDto> categoriasPorId() {
        Map<Long, CategoriaInternaDto> porId = new HashMap<>();
        for (CategoriaInternaDto c : categorias.listar()) {
            porId.put(c.id(), c);
        }
        return porId;
    }

    /**
     * La categoría elegida en el formulario se valida en category-service. Si responde 404, para este
     * formulario es un dato inválido (400); cualquier otro error (por ejemplo, 503) se informa tal cual.
     */
    private CategoriaInternaDto categoriaElegida(Long id) {
        try {
            return categorias.buscar(id);
        } catch (ApiException e) {
            if (e.getEstado() == HttpStatus.NOT_FOUND) {
                throw ApiException.regla("La categoría elegida no existe.");
            }
            throw e;
        }
    }

    private static void validarPrecios(ProductoSolicitud s) {
        if (s.precioNormal() != null && s.precioNormal() <= s.precio()) {
            throw ApiException.regla("El precio normal debe ser mayor que el precio de oferta.");
        }
    }

    /** Lo que crear y actualizar hacen igual: oferta, textos, imagen y ficha técnica. */
    private static void completar(Producto p, ProductoSolicitud s, CategoriaInternaDto categoria) {
        p.setCategoriaId(categoria.id());
        p.setPrecioNormal(s.precioNormal());
        p.setOfertaHasta(s.precioNormal() == null ? null : s.ofertaHasta());
        p.setResumen(Formatos.vacioANulo(s.resumen()));
        p.setDescripcion(Formatos.vacioANulo(s.descripcion()));
        p.setImagen(s.imagen() == null || s.imagen().isBlank() ? imagenPorDefecto(categoria.slug()) : s.imagen().trim());
        p.getEspecificaciones().clear();
        if (s.especificaciones() != null) {
            s.especificaciones().forEach(e -> p.getEspecificaciones().add(new Especificacion(e.clave().trim(), e.valor().trim())));
        }
    }

    /** inventory-service guarda una copia del SKU, el nombre y el estado: se le avisa de cada cambio. */
    private void avisarInventario(Producto p) {
        inventario.actualizarProducto(p.getId(), new ProductoInventarioSolicitud(p.getSku(), p.getNombre(), p.isActivo()));
    }

    private static ProductoResumenDto resumen(Producto p, CategoriaInternaDto c, LocalDate hoy) {
        return new ProductoResumenDto(p.getId(), p.getSku(), p.getNombre(), p.getMarca(), c.nombre(), c.slug(),
                p.precioVigente(hoy), p.precioNormalVigente(hoy), oferta(p, hoy), p.getImagen(), p.getResumen());
    }

    private static ProductoAdminDto admin(Producto p, String categoria) {
        return new ProductoAdminDto(p.getId(), p.getSku(), p.getNombre(), p.getMarca(), p.getCategoriaId(), categoria,
                p.getPrecio(), p.getPrecioNormal(), p.getOfertaHasta(), p.isActivo(), p.getImagen(), p.getResumen(),
                p.getDescripcion(), especificaciones(p));
    }

    private static LocalDate oferta(Producto p, LocalDate hoy) {
        return p.enOferta(hoy) ? p.getOfertaHasta() : null;
    }

    private static List<EspecificacionDto> especificaciones(Producto p) {
        return p.getEspecificaciones().stream().map(e -> new EspecificacionDto(e.getClave(), e.getValor())).toList();
    }

    /** Ilustración que usa el frontend mientras el producto no tenga foto propia. */
    private static String imagenPorDefecto(String slug) {
        return switch (slug) {
            case "notebooks" -> "notebook";
            case "procesadores" -> "cpu";
            case "perifericos" -> "teclado";
            case "consolas" -> "consola";
            case "monitores" -> "monitor";
            case "accesorios" -> "ssd";
            default -> "gpu";
        };
    }
}
