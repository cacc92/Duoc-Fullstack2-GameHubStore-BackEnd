package cl.gamehub.despachos.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.despachos.dto.ActualizarDespachoSolicitud;
import cl.gamehub.despachos.dto.DespachoDto;
import cl.gamehub.despachos.dto.EventoDto;
import cl.gamehub.despachos.dto.NuevoDespachoSolicitud;
import cl.gamehub.despachos.model.Despacho;
import cl.gamehub.despachos.model.EstadoDespacho;
import cl.gamehub.despachos.repository.DespachoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DespachoService {

    private static final Logger log = LoggerFactory.getLogger(DespachoService.class);

    private final DespachoRepository despachos;

    public DespachoService(DespachoRepository despachos) {
        this.despachos = despachos;
    }

    /**
     * Genera el despacho de una orden. Que la orden esté pagada lo revisa order-service antes de llamar aquí.
     * Sin seguimiento se usa GHX- y 70000 + el id de la orden; sin fecha estimada, hoy + 3 días.
     */
    @Transactional
    public DespachoDto generar(NuevoDespachoSolicitud s) {
        if (despachos.existsByOrdenId(s.ordenId())) {
            throw ApiException.conflicto("Esta orden ya tiene un despacho.");
        }
        String seguimiento = s.seguimiento() == null || s.seguimiento().isBlank()
                ? "GHX-" + (70000 + s.ordenId())
                : s.seguimiento().trim().toUpperCase(Locale.ROOT);
        // La columna es única: mejor un mensaje claro que el error genérico de la base
        if (despachos.existsBySeguimiento(seguimiento)) {
            throw ApiException.conflicto("Ya existe un despacho con el seguimiento " + seguimiento + ".");
        }
        LocalDate estimada = s.fechaEstimada() != null ? s.fechaEstimada() : LocalDate.now().plusDays(3);
        Despacho despacho = despachos.save(new Despacho(s.ordenId(), s.ordenNumero().trim(), seguimiento, estimada,
                Formatos.vacioANulo(s.destinatario()), Formatos.vacioANulo(s.direccion()),
                Formatos.vacioANulo(s.comuna()), Formatos.vacioANulo(s.region()), LocalDateTime.now()));
        log.info("Despacho {} generado para la orden {}, llegada estimada el {}", seguimiento, despacho.getOrdenNumero(),
                Formatos.fecha(estimada));
        return dto(despacho);
    }

    /** El despacho solo avanza: Preparando → Despachado → En tránsito → Entregado. */
    @Transactional
    public DespachoDto actualizar(Long ordenId, ActualizarDespachoSolicitud s) {
        Despacho despacho = despachos.findByOrdenId(ordenId)
                .orElseThrow(() -> ApiException.regla("Primero genera el despacho de esta orden."));
        // ordinal() es la posición en el enum: PREPARANDO 0, DESPACHADO 1, EN_TRANSITO 2, ENTREGADO 3
        if (s.estado().ordinal() <= despacho.getEstado().ordinal()) {
            throw ApiException.regla("El despacho solo avanza: elige un estado posterior al actual.");
        }
        if (s.seguimiento() != null && !s.seguimiento().isBlank()) {
            String seguimiento = s.seguimiento().trim().toUpperCase(Locale.ROOT);
            if (despachos.existsBySeguimientoAndIdNot(seguimiento, despacho.getId())) {
                throw ApiException.conflicto("Ya existe otro despacho con el seguimiento " + seguimiento + ".");
            }
            despacho.setSeguimiento(seguimiento);
        }
        if (s.fechaEstimada() != null) {
            despacho.setFechaEstimada(s.fechaEstimada());
        }
        EstadoDespacho anterior = despacho.getEstado();
        despacho.avanzar(s.estado(), LocalDateTime.now());
        log.info("Despacho {} de la orden {}: {} → {}", despacho.getSeguimiento(), despacho.getOrdenNumero(), anterior,
                s.estado());
        return dto(despacho);
    }

    /** Seguimiento público: no distingue mayúsculas ni espacios alrededor del código. */
    @Transactional(readOnly = true)
    public DespachoDto seguimiento(String codigo) {
        String limpio = codigo.trim().toUpperCase(Locale.ROOT);
        return despachos.findBySeguimiento(limpio).map(DespachoService::dto).orElseThrow(() ->
                ApiException.noEncontrado("No encontramos un despacho con el seguimiento " + limpio + "."));
    }

    /** order-service pide los despachos de varias órdenes de una vez; las órdenes sin despacho se omiten. */
    @Transactional(readOnly = true)
    public List<DespachoDto> buscarPorOrdenes(List<Long> ordenIds) {
        if (ordenIds == null || ordenIds.isEmpty()) {
            return List.of();
        }
        return despachos.findByOrdenIdInOrderByOrdenIdAsc(ordenIds).stream().map(DespachoService::dto).toList();
    }

    /** estado null: todos los despachos, del más nuevo al más antiguo. */
    @Transactional(readOnly = true)
    public List<DespachoDto> listar(EstadoDespacho estado) {
        List<Despacho> lista = estado == null
                ? despachos.findAllByOrderByCreadoEnDesc()
                : despachos.findByEstadoOrderByCreadoEnDesc(estado);
        return lista.stream().map(DespachoService::dto).toList();
    }

    private static DespachoDto dto(Despacho d) {
        return new DespachoDto(d.getOrdenId(), d.getOrdenNumero(), d.getSeguimiento(), d.getEstado(),
                d.getFechaEstimada(), d.getEntregadoEn(),
                d.getEventos().stream().map(e -> new EventoDto(e.getEstado(), e.getFecha())).toList());
    }
}
