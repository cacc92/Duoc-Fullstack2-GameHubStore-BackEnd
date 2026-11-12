package cl.gamehub.ordenes.client;

import java.time.LocalDate;

public record CuponDto(String codigo, int porcentaje, int tope, LocalDate inicio, LocalDate termino) {

    /** Porcentaje del subtotal, sin pasar del tope ni del subtotal: el total nunca queda negativo. */
    public int descuento(int subtotal) {
        if (subtotal <= 0) {
            return 0;
        }
        int descuento = (int) Math.round(subtotal * (double) porcentaje / 100);
        return Math.min(Math.min(descuento, tope), subtotal);
    }
}
