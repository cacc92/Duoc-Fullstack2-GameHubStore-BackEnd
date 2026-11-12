package cl.gamehub.productos.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.semilla.DatosDeEjemplo.ProductoEjemplo;
import cl.gamehub.productos.model.Especificacion;
import cl.gamehub.productos.model.Producto;
import cl.gamehub.productos.repository.ProductoRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga los 13 productos de ejemplo si la base está vacía, en el orden de DatosDeEjemplo.PRODUCTOS: así
 * sus id quedan del 1 al 13, los mismos que usan inventory-service, order-service y review-service.
 * El stock no se carga aquí (inventory-service tiene sus propios datos) y no se llama a otros servicios.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final ProductoRepository productos;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, ProductoRepository productos) {
        this.habilitado = habilitado;
        this.productos = productos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || productos.count() > 0) {
            return;
        }
        LocalDate hoy = DatosDeEjemplo.base().toLocalDate();
        for (ProductoEjemplo e : DatosDeEjemplo.PRODUCTOS) {
            Ficha ficha = ficha(e.sku());
            Producto p = new Producto(e.sku(), e.nombre(), ficha.marca(), e.categoriaId(), e.precio());
            p.setPrecioNormal(e.precioNormal());
            if (e.precioNormal() != null) {
                p.setOfertaHasta(hoy.plusDays(20));
            }
            p.setActivo(e.activo());
            p.setImagen(e.imagen());
            p.setResumen(ficha.resumen());
            p.setDescripcion(ficha.descripcion());
            String[] specs = ficha.especificaciones();
            for (int i = 0; i + 1 < specs.length; i += 2) {
                p.getEspecificaciones().add(new Especificacion(specs[i], specs[i + 1]));
            }
            productos.save(p);
        }
        log.info("Datos de ejemplo cargados: {} productos.", productos.count());
    }

    /** Lo que DatosDeEjemplo no trae porque ningún otro servicio lo necesita. especificaciones alterna clave y valor. */
    private record Ficha(String marca, String resumen, String descripcion, String... especificaciones) {
    }

    /** Marca, resumen, descripción y ficha técnica de cada SKU, copiados del monolito. */
    private static Ficha ficha(String sku) {
        return switch (sku) {
            case "NB-KAI-B15" -> new Ficha("Kairo",
                    "15,6\" 165 Hz · 16 GB · 1 TB SSD",
                    "Notebook de 15,6 pulgadas con pantalla de 165 Hz, 16 GB de RAM y SSD de 1 TB. Pensado para jugar en 1080p con detalle alto y llevarlo a clases.",
                    "Pantalla", "15,6\" IPS · 1920 × 1080 · 165 Hz",
                    "Procesador", "Tectra R7 · 8 núcleos",
                    "Tarjeta gráfica", "Voltra X60 · 8 GB",
                    "Memoria", "16 GB DDR5",
                    "Almacenamiento", "SSD NVMe 1 TB",
                    "Peso", "2,1 kg",
                    "Garantía", "6 meses");
            case "GPU-VOL-X70" -> new Ficha("Voltra",
                    "12 GB GDDR6X · PCIe 4.0",
                    "Tarjeta gráfica de 12 GB con overclock de fábrica y doble ventilador. Rinde en 1440p con detalle alto y se mantiene silenciosa con carga.",
                    "Memoria", "12 GB GDDR6X · 192 bits",
                    "Frecuencia boost", "2.610 MHz (OC)",
                    "Interfaz", "PCIe 4.0 x16",
                    "Salidas de video", "3 × DisplayPort 1.4a · 1 × HDMI 2.1",
                    "Consumo típico", "220 W",
                    "Fuente recomendada", "650 W",
                    "Conector de energía", "1 × 16 pines",
                    "Dimensiones", "285 × 112 × 50 mm",
                    "Garantía", "6 meses");
            case "CPU-TEC-R7" -> new Ficha("Tectra",
                    "8 núcleos · 16 hilos · 5,1 GHz",
                    "Procesador de 8 núcleos y 16 hilos para jugar y transmitir al mismo tiempo. No incluye disipador.",
                    "Núcleos / hilos", "8 / 16",
                    "Frecuencia máxima", "5,1 GHz",
                    "Caché", "32 MB L3",
                    "Socket", "TX5",
                    "Consumo", "105 W",
                    "Incluye disipador", "No");
            case "KB-NIM-TKL" -> new Ficha("Nimbo",
                    "Switches rojos · RGB · USB-C",
                    "Teclado mecánico sin bloque numérico, con switches lineales y luz RGB por tecla. Cable USB-C desmontable.",
                    "Formato", "TKL (87 teclas)",
                    "Switches", "Mecánicos rojos lineales",
                    "Iluminación", "RGB por tecla",
                    "Conexión", "USB-C desmontable",
                    "Distribución", "Español latinoamericano");
            case "MS-NIM-AERO" -> new Ficha("Nimbo",
                    "26.000 DPI · 63 g",
                    "Mouse liviano de 63 g con sensor de 26.000 DPI y conexión inalámbrica de baja latencia.",
                    "Sensor", "Óptico de 26.000 DPI",
                    "Peso", "63 g",
                    "Conexión", "Inalámbrica 2,4 GHz y Bluetooth",
                    "Batería", "Hasta 70 h");
            case "HS-AND-P71" -> new Ficha("Andesia",
                    "Sonido 7.1 · micrófono desmontable",
                    "Audífonos con sonido envolvente 7.1 virtual y micrófono desmontable con cancelación de ruido.",
                    "Sonido", "Envolvente 7.1 virtual",
                    "Micrófono", "Desmontable, con cancelación de ruido",
                    "Conexión", "USB y 3,5 mm",
                    "Peso", "290 g");
            case "MON-KAI-27Q" -> new Ficha("Kairo",
                    "2560 × 1440 · 1 ms · IPS",
                    "Monitor de 27 pulgadas QHD con panel IPS de 170 Hz y 1 ms de respuesta.",
                    "Tamaño", "27\"",
                    "Resolución", "2560 × 1440",
                    "Frecuencia", "170 Hz",
                    "Respuesta", "1 ms",
                    "Panel", "IPS",
                    "Entradas", "2 × HDMI 2.0 · 1 × DisplayPort 1.4");
            case "CON-AUS-GO" -> new Ficha("Austral",
                    "Pantalla de 7\" · 512 GB",
                    "Consola portátil con pantalla de 7 pulgadas a 120 Hz y 512 GB de almacenamiento.",
                    "Pantalla", "7\" · 1280 × 800 · 120 Hz",
                    "Almacenamiento", "512 GB",
                    "Batería", "Hasta 6 h",
                    "Peso", "640 g");
            case "CTL-AUS-PAD" -> new Ficha("Austral",
                    "Bluetooth · 40 h de batería",
                    "Control inalámbrico compatible con PC, consola y móviles, con 40 horas de batería.",
                    "Conexión", "Bluetooth 5.2 y USB-C",
                    "Batería", "Hasta 40 h",
                    "Compatibilidad", "PC, Austral Go y móviles");
            case "SSD-TEC-FLX1" -> new Ficha("Tectra",
                    "PCIe 4.0 · 7.000 MB/s",
                    "Unidad SSD M.2 de 1 TB que lee hasta 7.000 MB/s: los juegos cargan en segundos.",
                    "Capacidad", "1 TB",
                    "Interfaz", "PCIe 4.0 x4 NVMe",
                    "Lectura", "7.000 MB/s",
                    "Escritura", "6.000 MB/s",
                    "Formato", "M.2 2280");
            case "MIC-AND-CAST" -> new Ficha("Andesia",
                    "Cardioide · USB-C",
                    "Micrófono cardioide para streaming y clases en línea, con soporte de escritorio.",
                    "Patrón", "Cardioide",
                    "Conexión", "USB-C",
                    "Frecuencia", "20 Hz a 20 kHz",
                    "Incluye", "Soporte de escritorio");
            case "PAD-NIM-XL" -> new Ficha("Nimbo",
                    "90 × 40 cm · base antideslizante",
                    "Mousepad extendido que cubre teclado y mouse, con bordes cosidos y base de goma.",
                    "Medidas", "90 × 40 cm",
                    "Grosor", "4 mm",
                    "Base", "Goma antideslizante");
            case "CTL-AUS-PAD25" -> new Ficha("Austral",
                    "Bluetooth · 30 h de batería",
                    "Edición anterior del control Austral Pad.",
                    "Conexión", "Bluetooth 5.0");
            default -> throw new IllegalStateException("Falta la ficha del SKU " + sku);
        };
    }
}
