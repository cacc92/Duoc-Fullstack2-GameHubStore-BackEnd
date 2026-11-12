package cl.gamehub.garantias.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.garantias.client.OrdenClient;
import cl.gamehub.garantias.dto.CrearGarantiaSolicitud;
import cl.gamehub.garantias.dto.GarantiaDto;
import cl.gamehub.garantias.dto.LineaOrdenInternaDto;
import cl.gamehub.garantias.dto.OrdenInternaDto;
import cl.gamehub.garantias.dto.ResolverGarantiaSolicitud;
import cl.gamehub.garantias.dto.ResumenGarantiasDto;
import cl.gamehub.garantias.model.EstadoGarantia;
import cl.gamehub.garantias.model.Garantia;
import cl.gamehub.garantias.repository.GarantiaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GarantiaService {

    private static final Logger log = LoggerFactory.getLogger(GarantiaService.class);

    /** order-service informa el estado de la orden como texto: la garantía corre desde la entrega. */
    private static final String ENTREGADA = "ENTREGADA";

    private final GarantiaRepository garantias;
    private final OrdenClient ordenes;

    public GarantiaService(GarantiaRepository garantias, OrdenClient ordenes) {
        this.garantias = garantias;
        this.ordenes = ordenes;
    }

    /** Exige motivo, que el producto esté entregado y que siga dentro del plazo de 6 meses. */
    @Transactional
    public GarantiaDto crear(UsuarioAutenticado yo, CrearGarantiaSolicitud s) {
        // Si la orden no existe, order-service responde 404 «La orden no existe.» y el error sigue de largo
        OrdenInternaDto orden = ordenes.buscar(s.ordenId());
        if (!yo.id().equals(orden.usuarioId())) {
            throw ApiException.prohibido("Solo puedes pedir garantía de tus propias compras.");
        }
        LineaOrdenInternaDto linea = orden.lineas().stream()
                .filter(l -> s.productoId().equals(l.productoId())).findFirst()
                .orElseThrow(() -> ApiException.regla("El producto no pertenece a esta orden."));
        if (!ENTREGADA.equals(orden.estado()) || orden.entregadaEn() == null) {
            throw ApiException.regla("La garantía se puede pedir desde que recibes el producto.");
        }
        LocalDate plazo = Garantia.plazoDesde(orden.entregadaEn());
        if (LocalDate.now().isAfter(plazo)) {
            throw ApiException.regla("El plazo de garantía venció el " + Formatos.fecha(plazo) + ".");
        }
        if (garantias.existsByOrdenIdAndProductoIdAndEstadoIn(orden.id(), linea.productoId(), EstadoGarantia.ABIERTOS)) {
            throw ApiException.conflicto("Ya hay una solicitud abierta para este producto.");
        }
        Garantia garantia = garantias.save(new Garantia(orden.id(), orden.numero(), yo.id(), yo.nombre(),
                linea.productoId(), linea.nombre(), s.motivo(), s.descripcion().trim(), plazo, LocalDateTime.now()));
        garantia.asignarCodigo();
        log.info("Garantía {} creada por el usuario {}: orden {}, producto {}, motivo {}.",
                garantia.getCodigo(), yo.id(), orden.numero(), linea.productoId(), garantia.getMotivo());
        return dto(garantia);
    }

    /** Las solicitudes del cliente, de la más nueva a la más antigua. */
    @Transactional(readOnly = true)
    public List<GarantiaDto> mias(Long usuarioId) {
        return garantias.findByUsuarioIdOrderByCreadaEnDesc(usuarioId).stream().map(GarantiaService::dto).toList();
    }

    /** Para el operador: primero las abiertas o en revisión, después las cerradas. */
    @Transactional(readOnly = true)
    public List<GarantiaDto> todas() {
        // sorted es estable: dentro de cada grupo se mantiene el orden por fecha que entregó la base
        return garantias.findAllByOrderByCreadaEnDesc().stream()
                .sorted(Comparator.comparing(Garantia::cerrada))
                .map(GarantiaService::dto).toList();
    }

    @Transactional(readOnly = true)
    public ResumenGarantiasDto resumen() {
        return new ResumenGarantiasDto(garantias.countByEstadoIn(EstadoGarantia.ABIERTOS));
    }

    /** Regla del caso: una garantía no se cierra sin resolución. */
    @Transactional
    public GarantiaDto resolver(Long id, ResolverGarantiaSolicitud s) {
        Garantia garantia = garantias.findById(id).orElseThrow(() -> ApiException.noEncontrado("La garantía no existe."));
        if (garantia.cerrada()) {
            throw ApiException.regla("Esta garantía ya está cerrada.");
        }
        if (s.estado() == EstadoGarantia.ABIERTA) {
            throw ApiException.regla("Elige «En revisión», «Resuelta» o «Rechazada».");
        }
        String resolucion = s.resolucion() == null ? "" : s.resolucion().trim();
        if (s.estado().cierra() && resolucion.isEmpty()) {
            throw ApiException.regla("Una garantía no se cierra sin resolución. Escribe qué se hizo.");
        }
        // EN_REVISION sin texto conserva la resolución anterior
        garantia.actualizar(s.estado(), resolucion.isEmpty() ? garantia.getResolucion() : resolucion, LocalDateTime.now());
        log.info("Garantía {} pasó a {}.", garantia.getCodigo(), garantia.getEstado());
        return dto(garantia);
    }

    private static GarantiaDto dto(Garantia g) {
        return new GarantiaDto(g.getId(), g.getCodigo(), g.getOrdenId(), g.getOrdenNumero(), g.getProductoId(),
                g.getProducto(), g.getCliente(), g.getMotivo(), g.getDescripcion(), g.getEstado(), g.getResolucion(),
                g.getCreadaEn(), g.getActualizadaEn(), g.getPlazo());
    }
}
