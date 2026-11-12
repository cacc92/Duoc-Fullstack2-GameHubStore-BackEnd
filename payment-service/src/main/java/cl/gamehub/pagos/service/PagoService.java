package cl.gamehub.pagos.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.pagos.dto.NuevoPagoSolicitud;
import cl.gamehub.pagos.dto.PagoDto;
import cl.gamehub.pagos.model.EstadoPago;
import cl.gamehub.pagos.model.Pago;
import cl.gamehub.pagos.repository.PagoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoService {

    /** Cupo de la tarjeta de crédito simulada: sobre este total el pago se rechaza. */
    public static final int CUPO_CREDITO = 1_000_000;

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private static final String APROBADO = "Pago aprobado (simulado).";

    private final PagoRepository pagos;

    public PagoService(PagoRepository pagos) {
        this.pagos = pagos;
    }

    /** Pago simulado: débito aprueba, crédito rechaza sobre el cupo y transferencia queda pendiente. */
    @Transactional
    public PagoDto registrar(NuevoPagoSolicitud s) {
        if (pagos.existsByOrdenId(s.ordenId())) {
            throw ApiException.conflicto("La orden ya tiene un pago registrado.");
        }
        LocalDateTime ahora = LocalDateTime.now();
        Pago pago = switch (s.metodo()) {
            case DEBITO -> nuevo(s, EstadoPago.APROBADO, APROBADO, ahora);
            case CREDITO -> s.monto() > CUPO_CREDITO
                    ? nuevo(s, EstadoPago.RECHAZADO,
                            "Rechazado: el total supera el cupo simulado de " + Formatos.clp(CUPO_CREDITO) + ".", ahora)
                    : nuevo(s, EstadoPago.APROBADO, APROBADO, ahora);
            case TRANSFERENCIA -> nuevo(s, EstadoPago.PENDIENTE, "Esperando que el operador confirme la transferencia.", ahora);
        };
        pago = pagos.save(pago);
        log.info("Pago de la orden {}: {} por {} quedó {}", pago.getOrdenNumero(), pago.getMetodo(),
                Formatos.clp(pago.getMonto()), pago.getEstado());
        return dto(pago);
    }

    /** El operador revisó la cuenta y la transferencia llegó. */
    @Transactional
    public PagoDto confirmar(Long ordenId) {
        Pago pago = buscar(ordenId);
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw ApiException.regla("Solo se pueden confirmar pagos pendientes.");
        }
        pago.cambiar(EstadoPago.APROBADO, "Transferencia confirmada por el operador.", LocalDateTime.now());
        log.info("Transferencia de la orden {} confirmada", pago.getOrdenNumero());
        return dto(pago);
    }

    @Transactional
    public PagoDto anular(Long ordenId) {
        Pago pago = buscar(ordenId);
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw ApiException.regla("Solo se pueden anular pagos pendientes.");
        }
        pago.cambiar(EstadoPago.ANULADO, "Anulado por el operador.", LocalDateTime.now());
        log.info("Pago de la orden {} anulado por el operador", pago.getOrdenNumero());
        return dto(pago);
    }

    /**
     * Compensación: order-service aprobó el pago, pero después no pudo completar la compra (por ejemplo,
     * otro cliente se llevó el último producto). Se anula desde cualquier estado para no cobrar lo que no se vende.
     */
    @Transactional
    public PagoDto revertir(Long ordenId) {
        Pago pago = buscar(ordenId);
        EstadoPago anterior = pago.getEstado();
        pago.cambiar(EstadoPago.ANULADO, "Anulado: no había stock para completar la compra.", LocalDateTime.now());
        log.info("Pago de la orden {} revertido ({} → ANULADO)", pago.getOrdenNumero(), anterior);
        return dto(pago);
    }

    /** order-service pide los pagos de varias órdenes de una vez (por ejemplo, para la bandeja del operador). */
    @Transactional(readOnly = true)
    public List<PagoDto> buscarPorOrdenes(List<Long> ordenIds) {
        if (ordenIds == null || ordenIds.isEmpty()) {
            return List.of();
        }
        return pagos.findByOrdenIdInOrderByOrdenIdAsc(ordenIds).stream().map(PagoService::dto).toList();
    }

    @Transactional(readOnly = true)
    public List<PagoDto> mios(Long usuarioId) {
        return pagos.findByUsuarioIdOrderByFechaDesc(usuarioId).stream().map(PagoService::dto).toList();
    }

    /** estado null: todos los pagos. */
    @Transactional(readOnly = true)
    public List<PagoDto> listar(EstadoPago estado) {
        List<Pago> lista = estado == null ? pagos.findAllByOrderByFechaDesc() : pagos.findByEstadoOrderByFechaDesc(estado);
        return lista.stream().map(PagoService::dto).toList();
    }

    private Pago buscar(Long ordenId) {
        return pagos.findByOrdenId(ordenId).orElseThrow(() -> ApiException.noEncontrado("El pago de la orden no existe."));
    }

    private static Pago nuevo(NuevoPagoSolicitud s, EstadoPago estado, String detalle, LocalDateTime ahora) {
        return new Pago(s.ordenId(), s.ordenNumero().trim(), s.usuarioId(), s.metodo(), s.monto(), estado, detalle, ahora);
    }

    private static PagoDto dto(Pago p) {
        return new PagoDto(p.getId(), p.getOrdenId(), p.getOrdenNumero(), p.getMetodo(), p.getMonto(), p.getEstado(),
                p.getFecha(), p.getDetalle());
    }
}
