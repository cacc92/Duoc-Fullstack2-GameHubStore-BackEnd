package cl.gamehub.ordenes.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.ordenes.client.CambioDespachoSolicitud;
import cl.gamehub.ordenes.client.DespachoClient;
import cl.gamehub.ordenes.client.DespachoDto;
import cl.gamehub.ordenes.client.InventarioClient;
import cl.gamehub.ordenes.client.NuevoDespachoSolicitud;
import cl.gamehub.ordenes.client.PagoClient;
import cl.gamehub.ordenes.client.PagoDto;
import cl.gamehub.ordenes.dto.ActualizarDespachoSolicitud;
import cl.gamehub.ordenes.dto.GenerarDespachoSolicitud;
import cl.gamehub.ordenes.dto.OrdenDetalleDto;
import cl.gamehub.ordenes.dto.OrdenOperadorDto;
import cl.gamehub.ordenes.dto.ResumenOperadorDto;
import cl.gamehub.ordenes.model.DatosDespacho;
import cl.gamehub.ordenes.model.EstadoOrden;
import cl.gamehub.ordenes.model.LineaOrden;
import cl.gamehub.ordenes.model.Orden;
import cl.gamehub.ordenes.repository.OrdenRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operación diaria: confirmar o anular pagos y generar y avanzar despachos, coordinando con sus servicios. */
@Service
public class OperacionService {

    private static final Logger log = LoggerFactory.getLogger(OperacionService.class);
    private static final List<EstadoOrden> EN_CURSO =
            List.of(EstadoOrden.EN_PREPARACION, EstadoOrden.DESPACHADA, EstadoOrden.EN_TRANSITO);

    private final OrdenRepository ordenes;
    private final OrdenService ordenService;
    private final InventarioClient inventario;
    private final PagoClient pagos;
    private final DespachoClient despachos;

    public OperacionService(OrdenRepository ordenes, OrdenService ordenService, InventarioClient inventario,
                            PagoClient pagos, DespachoClient despachos) {
        this.ordenes = ordenes;
        this.ordenService = ordenService;
        this.inventario = inventario;
        this.pagos = pagos;
        this.despachos = despachos;
    }

    @Transactional(readOnly = true)
    public ResumenOperadorDto resumen() {
        return new ResumenOperadorDto(
                ordenes.countByEstadoIn(List.of(EstadoOrden.PENDIENTE_PAGO)),
                ordenes.countByEstadoIn(List.of(EstadoOrden.PAGADA)),
                ordenes.countByEstadoIn(EN_CURSO));
    }

    /** filtro: PENDIENTE_PAGO, POR_DESPACHAR, EN_CURSO, ENTREGADAS, CERRADAS o vacío para todas. */
    @Transactional(readOnly = true)
    public List<OrdenOperadorDto> listar(String filtro) {
        String clave = filtro == null ? "" : filtro.trim().toUpperCase(Locale.ROOT);
        List<Orden> lista = switch (clave) {
            case "PENDIENTE_PAGO" -> ordenes.findByEstadoInOrderByCreadaEnDesc(List.of(EstadoOrden.PENDIENTE_PAGO));
            case "POR_DESPACHAR" -> ordenes.findByEstadoInOrderByCreadaEnDesc(List.of(EstadoOrden.PAGADA));
            case "EN_CURSO" -> ordenes.findByEstadoInOrderByCreadaEnDesc(EN_CURSO);
            case "ENTREGADAS" -> ordenes.findByEstadoInOrderByCreadaEnDesc(List.of(EstadoOrden.ENTREGADA));
            case "CERRADAS" -> ordenes.findByEstadoInOrderByCreadaEnDesc(List.of(EstadoOrden.RECHAZADA, EstadoOrden.ANULADA));
            default -> ordenes.findAllByOrderByCreadaEnDesc();
        };
        Map<Long, PagoDto> pago = ordenService.pagosDe(lista);
        Map<Long, DespachoDto> despacho = ordenService.despachosDe(lista);
        return lista.stream().map(o -> {
            PagoDto p = pago.get(o.getId());
            return dto(o, p == null ? null : p.estado(), despacho.get(o.getId()));
        }).toList();
    }

    @Transactional(readOnly = true)
    public OrdenDetalleDto detalle(Long id) {
        return ordenService.detalle(ordenService.buscar(id));
    }

    /**
     * Confirma una transferencia: revisa el stock, aprueba el pago en payment-service y recién ahí descuenta.
     * noRollbackFor: si el descuento falla al final, la orden queda anulada igual que su pago revertido.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public OrdenOperadorDto confirmarPago(Long id) {
        Orden orden = ordenService.buscar(id);
        if (orden.getEstado() != EstadoOrden.PENDIENTE_PAGO) {
            throw ApiException.regla("Solo se pueden confirmar pagos pendientes.");
        }
        Map<Long, Integer> stock = CarritoService.stockPorId(
                inventario.stock(orden.getLineas().stream().map(LineaOrden::getProductoId).toList()));
        for (LineaOrden linea : orden.getLineas()) {
            if (linea.getCantidad() > stock.getOrDefault(linea.getProductoId(), 0)) {
                throw ApiException.conflicto("No hay stock suficiente de " + linea.getNombre() + " para confirmar el pago.");
            }
        }
        PagoDto pago = pagos.confirmar(id);
        orden.aplicarPago(pago.estado());
        try {
            ordenService.confirmarEfectos(orden);
        } catch (ApiException e) {
            orden.aplicarPago("ANULADO");
            throw ApiException.conflicto(e.getMessage() + " El pago quedó anulado.");
        }
        log.info("Pago de {} confirmado por el operador", orden.getNumero());
        return dto(orden, pago.estado(), null);
    }

    @Transactional
    public OrdenOperadorDto anularPago(Long id) {
        Orden orden = ordenService.buscar(id);
        if (orden.getEstado() != EstadoOrden.PENDIENTE_PAGO) {
            throw ApiException.regla("Solo se pueden anular pagos pendientes.");
        }
        PagoDto pago = pagos.anular(id);
        orden.aplicarPago(pago.estado());
        log.info("Pago de {} anulado por el operador", orden.getNumero());
        return dto(orden, pago.estado(), null);
    }

    /** Regla del caso: no se despacha una orden no pagada. */
    @Transactional
    public OrdenOperadorDto generarDespacho(Long id, GenerarDespachoSolicitud solicitud) {
        Orden orden = ordenService.buscar(id);
        if (orden.getEstado().tieneDespacho()) {
            throw ApiException.conflicto("Esta orden ya tiene un despacho.");
        }
        if (orden.getEstado() != EstadoOrden.PAGADA) {
            throw ApiException.conflicto("No se despacha una orden no pagada.");
        }
        DatosDespacho d = orden.getDatosDespacho();
        DespachoDto despacho = despachos.crear(new NuevoDespachoSolicitud(orden.getId(), orden.getNumero(),
                solicitud.seguimiento(), solicitud.fechaEstimada(), d.getDestinatario(), d.getDireccion(), d.getComuna(),
                d.getRegion()));
        orden.aplicarDespacho(despacho.estado(), despacho.entregadoEn());
        log.info("Despacho {} generado para {}", despacho.seguimiento(), orden.getNumero());
        return dto(orden, "APROBADO", despacho);
    }

    /** El despacho solo avanza (lo valida shipping-service); la orden toma el estado que corresponde. */
    @Transactional
    public OrdenOperadorDto actualizarDespacho(Long id, ActualizarDespachoSolicitud solicitud) {
        Orden orden = ordenService.buscar(id);
        if (!orden.getEstado().tieneDespacho()) {
            throw ApiException.regla("Primero genera el despacho de esta orden.");
        }
        DespachoDto despacho = despachos.actualizar(id, new CambioDespachoSolicitud(solicitud.estado().name(),
                solicitud.seguimiento(), solicitud.fechaEstimada()));
        orden.aplicarDespacho(despacho.estado(), despacho.entregadoEn());
        log.info("Despacho de {} pasó a {}", orden.getNumero(), despacho.estado());
        return dto(orden, "APROBADO", despacho);
    }

    private static OrdenOperadorDto dto(Orden o, String estadoPago, DespachoDto d) {
        return new OrdenOperadorDto(o.getId(), o.getNumero(), o.getCliente(), o.getCreadaEn(), o.getTotal(),
                o.getUnidades(), o.getEstado(), o.getMetodoPago(), estadoPago, d == null ? null : d.estado(),
                d == null ? null : d.seguimiento());
    }
}
