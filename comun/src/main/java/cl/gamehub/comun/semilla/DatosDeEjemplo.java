package cl.gamehub.comun.semilla;

import cl.gamehub.comun.util.Formatos;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Datos de ejemplo compartidos. Cada microservicio tiene su propia base y la llena al arrancar con su
 * clase DatosIniciales, pero los datos se cruzan: la orden 1 es de Camila (usuario 3) y lleva el
 * producto 3. Para que los 11 servicios calcen, todos toman de aquí los identificadores, las fechas y
 * los montos. Esto es solo para la demostración: en producción cada servicio crea sus propios datos.
 */
public final class DatosDeEjemplo {

    public static final String RM = "Región Metropolitana de Santiago";

    /** GAMER10, el único cupón usado en las órdenes de ejemplo: 10 % con tope de $30.000. */
    public static final int PORCENTAJE_GAMER10 = 10;
    public static final int TOPE_GAMER10 = 30_000;

    private DatosDeEjemplo() {
    }

    /**
     * Hora en punto del arranque. Todas las fechas de ejemplo se calculan desde aquí, así las ofertas y
     * los cupones siguen vigentes, y servicios que arrancan con minutos de diferencia llegan a las mismas fechas.
     */
    public static LocalDateTime base() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
    }

    // ---------- Usuarios (auth-service guarda la credencial; user-service, el perfil) ----------

    public record UsuarioEjemplo(long id, String nombre, String email, String rol, boolean activo, String telefono,
                                 String region, String comuna, String direccion, int diasDesdeRegistro) {
    }

    public static final List<UsuarioEjemplo> USUARIOS = List.of(
            new UsuarioEjemplo(1, "Ana Torres", "ana@gamehub.test", "ADMIN", true, "911111111", RM, "Santiago", "Av. Libertador Bernardo O'Higgins 1234", 400),
            new UsuarioEjemplo(2, "Luis Soto", "luis@gamehub.test", "OPERADOR", true, "922222222", RM, "Santiago", "San Diego 850", 380),
            new UsuarioEjemplo(3, "Camila Rojas", "camila@gamehub.test", "CLIENTE", true, "912345678", RM, "Ñuñoa", "Av. Los Aromos 1450, depto. 32", 300),
            new UsuarioEjemplo(4, "Pedro Díaz", "pedro@gamehub.test", "CLIENTE", false, "933333333", "Región de Valparaíso", "Viña del Mar", "Calle Quillota 875", 250),
            new UsuarioEjemplo(5, "Martina Castro", "martina@gamehub.test", "CLIENTE", true, "944444441", RM, "Providencia", "Av. Providencia 2150, depto. 504", 200),
            new UsuarioEjemplo(6, "Joaquín Reyes", "joaquin@gamehub.test", "CLIENTE", true, "944444442", "Región de Valparaíso", "Valparaíso", "Calle Esmeralda 1020", 190),
            new UsuarioEjemplo(7, "Fernanda Pino", "fernanda@gamehub.test", "CLIENTE", true, "944444443", "Región del Biobío", "Concepción", "Calle Barros Arana 640", 180),
            new UsuarioEjemplo(8, "Ignacio Mella", "ignacio@gamehub.test", "CLIENTE", true, "944444444", RM, "Maipú", "Av. Pajaritos 3300", 120),
            new UsuarioEjemplo(9, "Paula Rivas", "paula@gamehub.test", "CLIENTE", true, "944444445", RM, "La Florida", "Av. Vicuña Mackenna 7110", 110),
            new UsuarioEjemplo(10, "Andrés Vidal", "andres@gamehub.test", "CLIENTE", true, "944444446", "Región de Coquimbo", "La Serena", "Av. Francisco de Aguirre 455", 100),
            new UsuarioEjemplo(11, "Rodrigo Muñoz", "rodrigo@gamehub.test", "CLIENTE", true, "944444447", RM, "Las Condes", "Av. Apoquindo 4500, of. 12", 90),
            new UsuarioEjemplo(12, "Matías Fuentes", "matias@gamehub.test", "CLIENTE", true, "944444448", RM, "Puente Alto", "Av. Concha y Toro 1500", 60),
            new UsuarioEjemplo(13, "Valentina Soto", "valentina@gamehub.test", "CLIENTE", true, "944444449", "Región de La Araucanía", "Temuco", "Av. Alemania 870", 50),
            new UsuarioEjemplo(14, "Javiera Lagos", "javiera@gamehub.test", "CLIENTE", true, "955555551", RM, "Providencia", "Av. Italia 1180", 40),
            new UsuarioEjemplo(15, "Tomás Herrera", "tomas@gamehub.test", "CLIENTE", true, "955555552", "Región de Antofagasta", "Antofagasta", "Av. Grecia 1520", 30),
            new UsuarioEjemplo(16, "Diego Pérez", "diego@gamehub.test", "CLIENTE", true, "955555553", RM, "Santiago", "Calle Agustinas 1070", 70),
            new UsuarioEjemplo(17, "Sofía Vera", "sofia@gamehub.test", "CLIENTE", true, "955555554", "Región de Valparaíso", "Quilpué", "Av. Los Carrera 1900", 80));

    // ---------- Catálogo (category-service, product-service e inventory-service) ----------

    public record CategoriaEjemplo(long id, String nombre) {
    }

    public static final List<CategoriaEjemplo> CATEGORIAS = List.of(
            new CategoriaEjemplo(1, "Notebooks"),
            new CategoriaEjemplo(2, "Tarjetas gráficas"),
            new CategoriaEjemplo(3, "Procesadores"),
            new CategoriaEjemplo(4, "Periféricos"),
            new CategoriaEjemplo(5, "Consolas"),
            new CategoriaEjemplo(6, "Monitores"),
            new CategoriaEjemplo(7, "Accesorios"));

    /** precio es el que se cobra; si hay precioNormal, el producto está en oferta hasta hoy + 20 días. */
    public record ProductoEjemplo(long id, String sku, String nombre, String imagen, long categoriaId, int precio,
                                  Integer precioNormal, boolean activo, int stock) {
    }

    public static final List<ProductoEjemplo> PRODUCTOS = List.of(
            new ProductoEjemplo(1, "NB-KAI-B15", "Notebook gamer Kairo Blade 15", "notebook", 1, 1_149_990, 1_299_990, true, 7),
            new ProductoEjemplo(2, "GPU-VOL-X70", "Tarjeta gráfica Voltra X70 OC 12 GB", "gpu", 2, 649_990, 729_990, true, 12),
            new ProductoEjemplo(3, "CPU-TEC-R7", "Procesador Tectra R7 de 8 núcleos", "cpu", 3, 289_990, null, true, 15),
            new ProductoEjemplo(4, "KB-NIM-TKL", "Teclado mecánico Nimbo TKL", "teclado", 4, 59_990, null, true, 24),
            new ProductoEjemplo(5, "MS-NIM-AERO", "Mouse inalámbrico Nimbo Aero", "mouse", 4, 39_990, null, true, 0),
            new ProductoEjemplo(6, "HS-AND-P71", "Audífonos Andesia Pulse 7.1", "audifonos", 4, 49_990, 54_990, true, 3),
            new ProductoEjemplo(7, "MON-KAI-27Q", "Monitor Kairo 27\" QHD 170 Hz", "monitor", 6, 279_990, null, true, 9),
            new ProductoEjemplo(8, "CON-AUS-GO", "Consola portátil Austral Go 512 GB", "consola", 5, 499_990, null, true, 2),
            new ProductoEjemplo(9, "CTL-AUS-PAD", "Control inalámbrico Austral Pad", "control", 5, 44_990, null, true, 18),
            new ProductoEjemplo(10, "SSD-TEC-FLX1", "SSD NVMe Tectra Flux 1 TB", "ssd", 7, 69_990, 79_990, true, 30),
            new ProductoEjemplo(11, "MIC-AND-CAST", "Micrófono USB Andesia Cast", "microfono", 4, 54_990, null, true, 6),
            new ProductoEjemplo(12, "PAD-NIM-XL", "Mousepad XL Nimbo Grid", "mousepad", 7, 14_990, null, true, 40),
            new ProductoEjemplo(13, "CTL-AUS-PAD25", "Control Austral Pad (edición 2025)", "control", 5, 39_990, null, false, 0));

    // ---------- Órdenes (order-service, payment-service, shipping-service, review-service y warranty-service) ----------

    public record LineaEjemplo(long productoId, int cantidad) {
    }

    public record EventoEjemplo(String estado, LocalDateTime fecha) {
    }

    /**
     * Una orden de ejemplo. despachoHasta dice hasta qué estado avanzó el despacho (PREPARANDO, DESPACHADO,
     * EN_TRANSITO o ENTREGADO) y es null si la orden no tiene despacho.
     */
    public record OrdenEjemplo(long id, long usuarioId, LocalDateTime creadaEn, String metodoPago, String estadoPago,
                               String cupon, List<LineaEjemplo> lineas, String seguimiento, String despachoHasta) {

        /** GH-2026-000001: el mismo formato que usa order-service con las órdenes nuevas. */
        public String numero() {
            return "GH-" + creadaEn.getYear() + "-" + String.format("%06d", id);
        }

        public UsuarioEjemplo cliente() {
            return usuario(usuarioId);
        }

        public int subtotal() {
            return lineas.stream().mapToInt(l -> producto(l.productoId()).precio() * l.cantidad()).sum();
        }

        public int descuento() {
            if (cupon == null) {
                return 0;
            }
            int descuento = (int) Math.round(subtotal() * PORCENTAJE_GAMER10 / 100.0);
            return Math.min(Math.min(descuento, TOPE_GAMER10), subtotal());
        }

        public int total() {
            return Math.max(subtotal() - descuento(), 0);
        }

        /** Una transferencia aprobada se confirmó tres horas después de comprar. */
        public LocalDateTime fechaPago() {
            return "TRANSFERENCIA".equals(metodoPago) && "APROBADO".equals(estadoPago) ? creadaEn.plusHours(3) : creadaEn;
        }

        public String detallePago() {
            return switch (estadoPago) {
                case "APROBADO" -> "TRANSFERENCIA".equals(metodoPago)
                        ? "Transferencia confirmada por el operador." : "Pago aprobado (simulado).";
                case "RECHAZADO" -> "Rechazado: el total supera el cupo simulado de " + Formatos.clp(1_000_000) + ".";
                case "ANULADO" -> "Anulado por el operador.";
                default -> "Esperando que el operador confirme la transferencia.";
            };
        }

        public LocalDate fechaEstimada() {
            return creadaEn.toLocalDate().plusDays(4);
        }

        /** Eventos del despacho: se prepara 4 h después del pago y cada paso toma 20 h (6 h el tránsito). */
        public List<EventoEjemplo> eventos() {
            List<EventoEjemplo> eventos = new ArrayList<>();
            if (despachoHasta == null) {
                return eventos;
            }
            List<String> pasos = List.of("PREPARANDO", "DESPACHADO", "EN_TRANSITO", "ENTREGADO");
            LocalDateTime t = fechaPago().plusHours(4);
            eventos.add(new EventoEjemplo("PREPARANDO", t));
            for (String paso : pasos.subList(1, pasos.indexOf(despachoHasta) + 1)) {
                t = t.plusHours(paso.equals("EN_TRANSITO") ? 6 : 20);
                eventos.add(new EventoEjemplo(paso, t));
            }
            return eventos;
        }

        public LocalDateTime entregadaEn() {
            return "ENTREGADO".equals(despachoHasta) ? eventos().getLast().fecha() : null;
        }

        /** El estado visible de la orden sale del pago y del despacho, igual que en order-service. */
        public String estadoOrden() {
            return switch (estadoPago) {
                case "PENDIENTE" -> "PENDIENTE_PAGO";
                case "RECHAZADO" -> "RECHAZADA";
                case "ANULADO" -> "ANULADA";
                default -> despachoHasta == null ? "PAGADA" : switch (despachoHasta) {
                    case "PREPARANDO" -> "EN_PREPARACION";
                    case "DESPACHADO" -> "DESPACHADA";
                    case "EN_TRANSITO" -> "EN_TRANSITO";
                    default -> "ENTREGADA";
                };
            };
        }
    }

    /** Las 18 órdenes de ejemplo: cada situación que muestran Mis órdenes y la bandeja del operador. */
    public static List<OrdenEjemplo> ordenes(LocalDateTime base) {
        return List.of(
                // Camila: entregada, en tránsito, rechazada, antigua y una compra reciente con cupón
                orden(1, 3, dia(base, 22, 14, 5), "TRANSFERENCIA", "APROBADO", null, "GHX-61873", "ENTREGADO", 3, 1, 6, 1),
                orden(2, 3, dia(base, 4, 18, 15), "CREDITO", "APROBADO", null, "GHX-70214", "EN_TRANSITO", 7, 1, 12, 1),
                orden(3, 3, dia(base, 39, 20, 31), "CREDITO", "RECHAZADO", null, null, null, 1, 1),
                orden(4, 3, dia(base, 212, 11, 2), "DEBITO", "APROBADO", null, "GHX-20457", "ENTREGADO", 11, 1, 12, 1),
                orden(5, 3, base.minusHours(2), "DEBITO", "APROBADO", "GAMER10", null, null, 2, 1, 4, 1, 6, 2),
                // Bandeja del operador: pagos pendientes, despacho en preparación y un pago rechazado
                orden(6, 12, base.minusHours(3), "TRANSFERENCIA", "PENDIENTE", null, null, null, 4, 1),
                orden(7, 13, dia(base, 6, 19, 2), "TRANSFERENCIA", "PENDIENTE", null, null, null, 9, 1),
                orden(8, 14, dia(base, 1, 15, 10), "DEBITO", "APROBADO", null, "GHX-70341", "PREPARANDO", 1, 1),
                orden(9, 15, dia(base, 5, 12, 48), "CREDITO", "RECHAZADO", null, null, null, 2, 2),
                // Compras entregadas de otros clientes, con reseñas y garantías
                orden(10, 5, dia(base, 15, 10, 30), "DEBITO", "APROBADO", null, "GHX-66102", "ENTREGADO", 2, 1, 6, 1),
                orden(11, 6, dia(base, 23, 9, 40), "CREDITO", "APROBADO", null, "GHX-63390", "ENTREGADO", 2, 1, 4, 1),
                orden(12, 7, dia(base, 38, 16, 20), "DEBITO", "APROBADO", null, "GHX-58817", "ENTREGADO", 2, 1, 1, 1),
                orden(13, 11, dia(base, 52, 13, 0), "DEBITO", "APROBADO", null, "GHX-55203", "ENTREGADO", 8, 1, 10, 1, 6, 1),
                orden(14, 17, dia(base, 42, 17, 45), "CREDITO", "APROBADO", null, "GHX-57744", "ENTREGADO", 7, 1, 9, 1),
                orden(15, 16, dia(base, 27, 12, 15), "DEBITO", "APROBADO", null, "GHX-62950", "ENTREGADO", 4, 1),
                orden(16, 8, dia(base, 8, 11, 0), "DEBITO", "APROBADO", null, "GHX-68811", "ENTREGADO", 4, 1),
                orden(17, 9, dia(base, 10, 19, 30), "DEBITO", "APROBADO", null, "GHX-67430", "ENTREGADO", 7, 1),
                orden(18, 10, dia(base, 11, 10, 5), "TRANSFERENCIA", "APROBADO", null, "GHX-67102", "ENTREGADO", 10, 1));
    }

    public static OrdenEjemplo orden(LocalDateTime base, long id) {
        return ordenes(base).stream().filter(o -> o.id() == id).findFirst().orElseThrow();
    }

    public static UsuarioEjemplo usuario(long id) {
        return USUARIOS.stream().filter(u -> u.id() == id).findFirst().orElseThrow();
    }

    public static ProductoEjemplo producto(long id) {
        return PRODUCTOS.stream().filter(p -> p.id() == id).findFirst().orElseThrow();
    }

    private static LocalDateTime dia(LocalDateTime base, int diasAtras, int hora, int minuto) {
        return base.minusDays(diasAtras).withHour(hora).withMinute(minuto);
    }

    /** lineas alterna id de producto y cantidad: 3, 1, 6, 1 → producto 3 × 1 y producto 6 × 1. */
    private static OrdenEjemplo orden(long id, long usuarioId, LocalDateTime creadaEn, String metodo, String estadoPago,
                                      String cupon, String seguimiento, String despachoHasta, int... lineas) {
        List<LineaEjemplo> lista = new ArrayList<>();
        for (int i = 0; i + 1 < lineas.length; i += 2) {
            lista.add(new LineaEjemplo(lineas[i], lineas[i + 1]));
        }
        return new OrdenEjemplo(id, usuarioId, creadaEn, metodo, estadoPago, cupon, List.copyOf(lista), seguimiento,
                despachoHasta);
    }
}
