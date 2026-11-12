package cl.gamehub.ordenes.client;

import java.util.List;

/** referencia queda como motivo del movimiento en inventory-service: "Venta GH-2026-000019". */
public record DescuentoStockSolicitud(String referencia, List<LineaStock> lineas) {

    public record LineaStock(Long productoId, int cantidad) {
    }
}
