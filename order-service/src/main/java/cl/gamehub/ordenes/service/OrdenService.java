package cl.gamehub.ordenes.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.ordenes.client.CuponDto;
import cl.gamehub.ordenes.client.DescuentoStockSolicitud;
import cl.gamehub.ordenes.client.DespachoClient;
import cl.gamehub.ordenes.client.DespachoDto;
import cl.gamehub.ordenes.client.InventarioClient;
import cl.gamehub.ordenes.client.NuevoPagoSolicitud;
import cl.gamehub.ordenes.client.PagoClient;
import cl.gamehub.ordenes.client.PagoDto;
import cl.gamehub.ordenes.client.ProductoClient;
import cl.gamehub.ordenes.client.ProductoInternoDto;
import cl.gamehub.ordenes.client.PromocionClient;
import cl.gamehub.ordenes.dto.CrearOrdenSolicitud;
import cl.gamehub.ordenes.dto.DatosDespachoDto;
import cl.gamehub.ordenes.dto.LineaOrdenDto;
import cl.gamehub.ordenes.dto.LineaSolicitud;
import cl.gamehub.ordenes.dto.OrdenDetalleDto;
import cl.gamehub.ordenes.dto.OrdenInternaDto;
import cl.gamehub.ordenes.dto.OrdenResumenDto;
import cl.gamehub.ordenes.model.DatosDespacho;
import cl.gamehub.ordenes.model.Orden;
import cl.gamehub.ordenes.repository.OrdenRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Checkout y seguimiento de las órdenes del cliente. */
@Service
public class OrdenService {

    private static final Logger log = LoggerFactory.getLogger(OrdenService.class);

    private final OrdenRepository ordenes;
    private final ProductoClient productos;
    private final InventarioClient inventario;
    private final PromocionClient promociones;
    private final PagoClient pagos;
    private final DespachoClient despachos;

    public OrdenService(OrdenRepository ordenes, ProductoClient productos, InventarioClient inventario,
                        PromocionClient promociones, PagoClient pagos, DespachoClient despachos) {
        this.ordenes = ordenes;
        this.productos = productos;
        this.inventario = inventario;
        this.promociones = promociones;
        this.pagos = pagos;
        this.despachos = despachos;
    }

    /**
     * Crea la orden en cuatro pasos: revisa productos y stock, vuelve a validar el cupón, registra el pago
     * simulado en payment-service y, solo si quedó aprobado, descuenta el stock. Si algo falla antes de
     * terminar, la transacción se revierte y la orden no queda guardada.
     */
    @Transactional
    public OrdenDetalleDto crear(UsuarioAutenticado cliente, CrearOrdenSolicitud solicitud) {
        List<LineaSolicitud> lineas = CarritoService.agrupar(solicitud.lineas());
        List<Long> ids = lineas.stream().map(LineaSolicitud::productoId).toList();
        Map<Long, ProductoInternoDto> catalogo = CarritoService.porId(productos.buscar(ids));
        Map<Long, Integer> stock = CarritoService.stockPorId(inventario.stock(ids));

        DatosDespachoDto d = solicitud.despacho();
        Orden orden = new Orden(cliente.id(), cliente.nombre(), new DatosDespacho(d.nombre().trim(), d.correo().trim(),
                d.telefono(), d.region(), d.comuna(), d.direccion().trim(), Formatos.vacioANulo(d.depto()),
                Formatos.vacioANulo(d.indicaciones())), solicitud.metodoPago(), LocalDateTime.now());
        int subtotal = 0;
        for (LineaSolicitud linea : lineas) {
            ProductoInternoDto p = catalogo.get(linea.productoId());
            if (p == null || !p.activo()) {
                throw ApiException.regla("Uno de los productos ya no está disponible. Revisa tu carrito.");
            }
            int disponible = stock.getOrDefault(p.id(), 0);
            if (linea.cantidad() > disponible) {
                throw ApiException.conflicto("No hay stock suficiente de " + p.nombre() + ": quedan " + disponible + " unidades.");
            }
            orden.agregarLinea(p.id(), p.sku(), p.nombre(), p.imagen(), p.precio(), linea.cantidad());
            subtotal += p.precio() * linea.cantidad();
        }

        CuponDto cupon = null;
        if (solicitud.cupon() != null && !solicitud.cupon().isBlank()) {
            // Si el cupón ya no sirve, promotion-service responde 400 o 404 con el motivo y la compra se detiene
            cupon = promociones.vigente(solicitud.cupon().trim());
        }
        orden.fijarTotales(subtotal, cupon == null ? 0 : cupon.descuento(subtotal), cupon == null ? null : cupon.codigo());
        orden = ordenes.save(orden);
        orden.asignarNumero();

        PagoDto pago = pagos.crear(new NuevoPagoSolicitud(orden.getId(), orden.getNumero(), cliente.id(),
                solicitud.metodoPago().name(), orden.getTotal()));
        orden.aplicarPago(pago.estado());
        if ("APROBADO".equals(pago.estado())) {
            confirmarEfectos(orden);
        }
        log.info("Orden {} de {}: total {} con {}, pago {}", orden.getNumero(), cliente.email(),
                Formatos.clp(orden.getTotal()), orden.getMetodoPago(), pago.estado());
        return armarDetalle(orden, pago, null);
    }

    /**
     * Efectos de un pago aprobado: descontar el stock y contar el uso del cupón. Si inventory-service ya no
     * tiene stock (otra compra se adelantó), se revierte el pago en payment-service: es la compensación de
     * una transacción repartida entre servicios.
     */
    void confirmarEfectos(Orden orden) {
        List<DescuentoStockSolicitud.LineaStock> lineas = orden.getLineas().stream()
                .map(l -> new DescuentoStockSolicitud.LineaStock(l.getProductoId(), l.getCantidad()))
                .toList();
        try {
            inventario.descontar(new DescuentoStockSolicitud("Venta " + orden.getNumero(), lineas));
        } catch (ApiException e) {
            log.warn("No se pudo descontar el stock de {} ({}). Se revierte el pago.", orden.getNumero(), e.getMessage());
            pagos.revertir(orden.getId());
            throw e;
        }
        if (orden.getCupon() != null) {
            try {
                promociones.registrarUso(orden.getCupon());
            } catch (RuntimeException e) {
                // El uso del cupón es un contador: si falla, la compra sigue siendo válida
                log.warn("No se pudo registrar el uso del cupón {} en {}: {}", orden.getCupon(), orden.getNumero(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<OrdenResumenDto> mias(Long usuarioId) {
        return ordenes.findByUsuarioIdOrderByCreadaEnDesc(usuarioId).stream()
                .map(o -> new OrdenResumenDto(o.getId(), o.getNumero(), o.getCreadaEn(), o.getEstado(), o.getTotal(),
                        o.getUnidades()))
                .toList();
    }

    /** El cliente solo ve sus órdenes; el operador y el administrador ven todas. */
    @Transactional(readOnly = true)
    public OrdenDetalleDto detalle(Long id, UsuarioAutenticado yo) {
        Orden orden = buscar(id);
        if (yo.rol() == Rol.CLIENTE && !orden.getUsuarioId().equals(yo.id())) {
            throw ApiException.prohibido("Esta orden no te pertenece.");
        }
        return detalle(orden);
    }

    /** Lo propio de la orden, más el pago (payment-service) y el despacho si ya existe (shipping-service). */
    OrdenDetalleDto detalle(Orden orden) {
        PagoDto pago = pagosDe(List.of(orden)).get(orden.getId());
        DespachoDto despacho = despachosDe(List.of(orden)).get(orden.getId());
        return armarDetalle(orden, pago, despacho);
    }

    @Transactional(readOnly = true)
    public OrdenInternaDto interno(Long id) {
        Orden o = buscar(id);
        return new OrdenInternaDto(o.getId(), o.getNumero(), o.getUsuarioId(), o.getCliente(), o.getEstado(),
                o.getEntregadaEn(), o.getLineas().stream()
                        .map(l -> new OrdenInternaDto.LineaInternaDto(l.getProductoId(), l.getNombre()))
                        .toList());
    }

    public Orden buscar(Long id) {
        return ordenes.findById(id).orElseThrow(() -> ApiException.noEncontrado("La orden no existe."));
    }

    /** Pagos de varias órdenes en una sola llamada a payment-service. */
    Map<Long, PagoDto> pagosDe(List<Orden> lista) {
        if (lista.isEmpty()) {
            return Map.of();
        }
        return pagos.buscar(lista.stream().map(Orden::getId).toList()).stream()
                .collect(Collectors.toMap(PagoDto::ordenId, Function.identity(), (a, b) -> a));
    }

    /** Despachos en una sola llamada a shipping-service, solo de las órdenes que ya tienen uno. */
    Map<Long, DespachoDto> despachosDe(List<Orden> lista) {
        List<Long> conDespacho = lista.stream().filter(o -> o.getEstado().tieneDespacho()).map(Orden::getId).toList();
        if (conDespacho.isEmpty()) {
            return Map.of();
        }
        return despachos.buscar(conDespacho).stream()
                .collect(Collectors.toMap(DespachoDto::ordenId, Function.identity(), (a, b) -> a));
    }

    private static OrdenDetalleDto armarDetalle(Orden o, PagoDto pago, DespachoDto despacho) {
        DatosDespacho d = o.getDatosDespacho();
        DatosDespachoDto datos = new DatosDespachoDto(d.getDestinatario(), d.getCorreo(), d.getTelefono(), d.getRegion(),
                d.getComuna(), d.getDireccion(), d.getDepto(), d.getIndicaciones());
        List<LineaOrdenDto> lineas = o.getLineas().stream()
                .map(l -> new LineaOrdenDto(l.getProductoId(), l.getNombre(), l.getImagen(), l.getPrecioUnitario(),
                        l.getCantidad(), l.getSubtotal()))
                .toList();
        return new OrdenDetalleDto(o.getId(), o.getNumero(), o.getCreadaEn(), o.getEstado(), o.getCliente(),
                o.getSubtotal(), o.getDescuento(), o.getTotal(), o.getCupon(), datos, lineas, pago, despacho,
                o.getEntregadaEn());
    }
}
