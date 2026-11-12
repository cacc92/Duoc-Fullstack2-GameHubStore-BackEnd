package cl.gamehub.comun.util;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatos que aparecen en los mensajes para el usuario: fechas dd-MM-yyyy y pesos chilenos. */
public final class Formatos {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Locale CHILE = Locale.forLanguageTag("es-CL");

    private Formatos() {
    }

    public static String fecha(LocalDate fecha) {
        return fecha.format(FECHA);
    }

    /** 1149990 → "$1.149.990". */
    public static String clp(long monto) {
        return "$" + String.format(CHILE, "%,d", monto);
    }

    /** "Tarjetas gráficas" → "tarjetas-graficas". */
    public static String slug(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    /** "Martina Castro" → "Martina C." para mostrar reseñas sin exponer el apellido completo. */
    public static String nombreCorto(String nombre) {
        String[] partes = nombre.trim().split("\\s+");
        return partes.length < 2 ? partes[0] : partes[0] + " " + partes[1].charAt(0) + ".";
    }

    public static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
