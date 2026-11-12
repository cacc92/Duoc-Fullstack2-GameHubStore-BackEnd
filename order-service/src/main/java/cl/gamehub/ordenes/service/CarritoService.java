package cl.gamehub.ordenes.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.ordenes.client.CuponDto;
import cl.gamehub.ordenes.client.DisponibilidadDto;
import cl.gamehub.ordenes.client.InventarioClient;
import cl.gamehub.ordenes.client.ProductoClient;
import cl.gamehub.ordenes.client.ProductoInternoDto;
import cl.gamehub.ordenes.client.PromocionClient;
import cl.gamehub.ordenes.dto.LineaSolicitud;
import cl.gamehub.ordenes.dto.LineaValidadaDto;
import cl.gamehub.ordenes.dto.ValidacionCarritoDto;
import cl.gamehub.ordenes.dto.ValidarCarritoSolicitud;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Revisa el carrito antes del checkout, como pide el caso: el stock contra inventory-service, el precio
 * contra product-service y el cupón contra promotion-service. Son tres servicios en una sola respuesta.
 */
@Service
public class CarritoService {

    private final ProductoClient productos;
    private final InventarioClient inventario;
    private final PromocionClient promociones;

    public CarritoService(ProductoClient productos, InventarioClient inventario, PromocionClient promociones) {
        this.productos = productos;
        this.inventario = inventario;
        this.promociones = promociones;
    }

    public ValidacionCarritoDto validar(ValidarCarritoSolicitud solicitud) {
        List<LineaSolicitud> lineas = agrupar(solicitud.lineas());
        if (lineas.isEmpty()) {
            return new ValidacionCarritoDto(List.of(), null, null, 0, 0, 0, false);
        }
        List<Long> ids = lineas.stream().map(LineaSolicitud::productoId).toList();
        Map<Long, ProductoInternoDto> catalogo = porId(productos.buscar(ids));
        Map<Long, Integer> stock = stockPorId(inventario.stock(ids));

        List<LineaValidadaDto> resultado = new ArrayList<>();
        int subtotal = 0;
        boolean valido = true;
        for (LineaSolicitud linea : lineas) {
            ProductoInternoDto p = catalogo.get(linea.productoId());
            if (p == null || !p.activo()) {
                resultado.add(new LineaValidadaDto(linea.productoId(), p == null ? "Producto" : p.nombre(), 0, null, 0,
                        linea.cantidad(), false, "Este producto ya no está disponible."));
                valido = false;
                continue;
            }
            int disponible = stock.getOrDefault(p.id(), 0);
            boolean alcanza = linea.cantidad() <= disponible;
            String mensaje = null;
            if (!alcanza) {
                valido = false;
                mensaje = disponible == 0 ? "Este producto se quedó sin stock." : "Solo quedan " + disponible + " unidades.";
            }
            subtotal += p.precio() * linea.cantidad();
            resultado.add(new LineaValidadaDto(p.id(), p.nombre(), p.precio(), p.precioNormal(), disponible,
                    linea.cantidad(), alcanza, mensaje));
        }

        CuponDto cupon = null;
        String mensajeCupon = null;
        int descuento = 0;
        if (solicitud.cupon() != null && !solicitud.cupon().isBlank()) {
            try {
                cupon = promociones.vigente(solicitud.cupon().trim());
                descuento = cupon.descuento(subtotal);
            } catch (ApiException e) {
                // Cupón vencido, agotado o inexistente: el carrito sigue siendo válido, solo sin descuento
                if (!e.getEstado().is4xxClientError()) {
                    throw e;
                }
                mensajeCupon = e.getMessage();
            }
        }
        return new ValidacionCarritoDto(resultado, cupon, mensajeCupon, subtotal, descuento,
                Math.max(subtotal - descuento, 0), valido);
    }

    /** Si el mismo producto viene en dos líneas, se suman sus cantidades. */
    public static List<LineaSolicitud> agrupar(List<LineaSolicitud> lineas) {
        Map<Long, Integer> porProducto = new LinkedHashMap<>();
        for (LineaSolicitud l : lineas) {
            porProducto.merge(l.productoId(), l.cantidad(), Integer::sum);
        }
        return porProducto.entrySet().stream().map(e -> new LineaSolicitud(e.getKey(), e.getValue())).toList();
    }

    static Map<Long, ProductoInternoDto> porId(List<ProductoInternoDto> lista) {
        return lista.stream().collect(Collectors.toMap(ProductoInternoDto::id, Function.identity(), (a, b) -> a));
    }

    static Map<Long, Integer> stockPorId(List<DisponibilidadDto> lista) {
        return lista.stream().collect(Collectors.toMap(DisponibilidadDto::productoId, DisponibilidadDto::stock, (a, b) -> a));
    }
}
