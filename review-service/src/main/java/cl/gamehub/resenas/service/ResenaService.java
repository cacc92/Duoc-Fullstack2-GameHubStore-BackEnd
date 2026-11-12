package cl.gamehub.resenas.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.resenas.client.OrdenClient;
import cl.gamehub.resenas.dto.CrearResenaSolicitud;
import cl.gamehub.resenas.dto.LineaOrdenInternaDto;
import cl.gamehub.resenas.dto.MiResenaDto;
import cl.gamehub.resenas.dto.OrdenInternaDto;
import cl.gamehub.resenas.dto.ResenaAdminDto;
import cl.gamehub.resenas.dto.ResenaDto;
import cl.gamehub.resenas.dto.ResumenResenasDto;
import cl.gamehub.resenas.model.EstadoResena;
import cl.gamehub.resenas.model.Resena;
import cl.gamehub.resenas.repository.PromedioProducto;
import cl.gamehub.resenas.repository.ResenaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResenaService {

    private static final Logger log = LoggerFactory.getLogger(ResenaService.class);

    /** order-service informa el estado de la orden como texto: solo se reseña lo que ya llegó al cliente. */
    private static final String ENTREGADA = "ENTREGADA";

    /** Teléfonos chilenos (9 1234 5678) o correos dentro del texto: se avisan al moderar. */
    private static final Pattern CONTACTO =
            Pattern.compile("(\\b9[\\s.-]?\\d{4}[\\s.-]?\\d{4}\\b)|([\\w.+-]+@[\\w-]+\\.[\\w.]+)");

    private final ResenaRepository resenas;
    private final OrdenClient ordenes;

    public ResenaService(ResenaRepository resenas, OrdenClient ordenes) {
        this.resenas = resenas;
        this.ordenes = ordenes;
    }

    /** Solo las aprobadas, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<ResenaDto> publicas(Long productoId) {
        return resenas.findByProductoIdAndEstadoOrderByFechaDesc(productoId, EstadoResena.APROBADA).stream()
                .map(ResenaService::dto).toList();
    }

    /**
     * Estrellas del catálogo: promedio de las reseñas aprobadas y cuántas son. Sin productos (null o
     * lista vacía) trae todos; los productos sin reseñas aprobadas no aparecen.
     */
    @Transactional(readOnly = true)
    public List<ResumenResenasDto> resumen(List<Long> productos) {
        List<PromedioProducto> filas = productos == null || productos.isEmpty()
                ? resenas.promedios(EstadoResena.APROBADA)
                : resenas.promedios(EstadoResena.APROBADA, productos);
        return filas.stream()
                .map(f -> new ResumenResenasDto(f.productoId(), unDecimal(f.promedio()), f.cantidad()))
                .toList();
    }

    /** Solo se reseña un producto comprado, una vez por orden, y después de recibirlo. */
    @Transactional
    public ResenaDto crear(UsuarioAutenticado yo, CrearResenaSolicitud s) {
        // Si la orden no existe, order-service responde 404 «La orden no existe.» y el error sigue de largo
        OrdenInternaDto orden = ordenes.buscar(s.ordenId());
        if (!yo.id().equals(orden.usuarioId())) {
            throw ApiException.prohibido("Solo puedes reseñar tus propias compras.");
        }
        LineaOrdenInternaDto linea = orden.lineas().stream()
                .filter(l -> s.productoId().equals(l.productoId())).findFirst()
                .orElseThrow(() -> ApiException.regla("Solo puedes reseñar productos que compraste en esta orden."));
        if (!ENTREGADA.equals(orden.estado())) {
            throw ApiException.regla("Podrás reseñar este producto cuando lo recibas.");
        }
        if (resenas.existsByOrdenIdAndProductoId(orden.id(), linea.productoId())) {
            throw ApiException.conflicto("Ya reseñaste este producto en esta orden.");
        }
        Resena resena = resenas.save(new Resena(orden.id(), yo.id(), Formatos.nombreCorto(yo.nombre()),
                linea.productoId(), linea.nombre(), s.puntuacion(), s.comentario().trim(), LocalDateTime.now()));
        log.info("Reseña {} creada por el usuario {}: orden {}, producto {}, {} estrellas. Queda pendiente de moderación.",
                resena.getId(), yo.id(), orden.numero(), linea.productoId(), resena.getPuntuacion());
        return dto(resena);
    }

    /** Las reseñas del cliente con su estado, de la más reciente a la más antigua. */
    @Transactional(readOnly = true)
    public List<MiResenaDto> mias(Long usuarioId) {
        return resenas.findByUsuarioIdOrderByFechaDesc(usuarioId).stream().map(ResenaService::mia).toList();
    }

    // ---------- Moderación ----------

    /** estado null trae todas. */
    @Transactional(readOnly = true)
    public List<ResenaAdminDto> listar(EstadoResena estado) {
        List<Resena> lista = estado == null
                ? resenas.findAllByOrderByFechaDesc()
                : resenas.findByEstadoOrderByFechaDesc(estado);
        return lista.stream().map(ResenaService::admin).toList();
    }

    @Transactional
    public ResenaAdminDto moderar(Long id, EstadoResena estado) {
        if (estado == EstadoResena.PENDIENTE) {
            throw ApiException.regla("Elige aprobar u ocultar la reseña.");
        }
        Resena resena = resenas.findById(id).orElseThrow(() -> ApiException.noEncontrado("La reseña no existe."));
        resena.moderar(estado);
        log.info("Reseña {} moderada: {}", id, estado);
        return admin(resena);
    }

    /** 4,666… → 4,7: el mismo redondeo que usa el frontend para mostrar el promedio. */
    private static double unDecimal(double valor) {
        return Math.round(valor * 10) / 10.0;
    }

    private static ResenaDto dto(Resena r) {
        return new ResenaDto(r.getId(), r.getAutor(), r.getPuntuacion(), r.getComentario(), r.getFecha(), r.getEstado());
    }

    private static MiResenaDto mia(Resena r) {
        return new MiResenaDto(r.getId(), r.getOrdenId(), r.getProductoId(), r.getProducto(), r.getPuntuacion(),
                r.getComentario(), r.getFecha(), r.getEstado());
    }

    private static ResenaAdminDto admin(Resena r) {
        String alerta = CONTACTO.matcher(r.getComentario()).find() ? "Contiene datos de contacto" : null;
        return new ResenaAdminDto(r.getId(), r.getProductoId(), r.getProducto(), r.getAutor(), r.getPuntuacion(),
                r.getComentario(), r.getFecha(), r.getEstado(), alerta);
    }
}
