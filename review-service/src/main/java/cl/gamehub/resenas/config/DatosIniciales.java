package cl.gamehub.resenas.config;

import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.resenas.model.EstadoResena;
import cl.gamehub.resenas.model.Resena;
import cl.gamehub.resenas.repository.ResenaRepository;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Carga las 16 reseñas de ejemplo si la base está vacía (sus id quedan del 1 al 16). La orden, el cliente
 * y el producto de cada una salen de DatosDeEjemplo, así calzan con lo que tiene order-service: la reseña 1
 * es de Camila sobre el procesador que recibió en la orden 1.
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final boolean habilitado;
    private final ResenaRepository resenas;

    public DatosIniciales(@Value("${app.datos-iniciales:true}") boolean habilitado, ResenaRepository resenas) {
        this.habilitado = habilitado;
        this.resenas = resenas;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!habilitado || resenas.count() > 0) {
            return;
        }
        LocalDateTime base = DatosDeEjemplo.base();

        // Camila: el procesador de su orden entregada y los dos productos de su compra antigua
        resena(base, 1, 3, 5, "Instalación simple y temperaturas bajas con un disipador de torre. Vale cada peso.",
                base.minusDays(17), EstadoResena.APROBADA);
        resena(base, 4, 11, 4, "Se escucha claro en llamadas y transmisiones. El soporte podría ser más firme.",
                base.minusDays(200), EstadoResena.APROBADA);
        resena(base, 4, 12, 5, "Enorme y no se mueve del escritorio. Los bordes cosidos aguantan bien.",
                base.minusDays(198), EstadoResena.APROBADA);

        // Compras entregadas de otros clientes, con reseñas aprobadas
        resena(base, 10, 2, 5, "Llegó en dos días y bien embalada. En 1440p mantiene más de 100 FPS en lo que juego y es silenciosa con carga.",
                base.minusDays(5), EstadoResena.APROBADA);
        resena(base, 11, 2, 4, "Rinde muy bien, pero es más larga de lo que pensaba: revisen el espacio del gabinete antes de comprar.",
                base.minusDays(13), EstadoResena.APROBADA);
        resena(base, 11, 4, 4, "Buen recorrido de tecla y el software es liviano. Le falta un reposamuñecas.",
                base.minusDays(12), EstadoResena.APROBADA);
        resena(base, 12, 2, 5, "Buen precio con la oferta. La instalación fue directa y el software de control es simple.",
                base.minusDays(29), EstadoResena.APROBADA);
        resena(base, 12, 1, 5, "Llevo un mes jugando y no se calienta tanto como temía. La pantalla de 165 Hz se nota.",
                base.minusDays(8), EstadoResena.APROBADA);
        resena(base, 13, 8, 5, "Corre todo mi catálogo y la pantalla de 120 Hz se ve muy fluida.",
                base.minusDays(45), EstadoResena.APROBADA);
        resena(base, 13, 10, 5, "Bajó los tiempos de carga a la mitad. Lo instalé en cinco minutos.",
                base.minusDays(44), EstadoResena.APROBADA);
        resena(base, 13, 6, 4, "Cómodos para sesiones largas; el micrófono se escucha claro.",
                base.minusDays(43), EstadoResena.APROBADA);
        // Sofía reseñó el monitor después de que se lo cambiaron por garantía (warranty-service)
        resena(base, 14, 7, 5, "Después del cambio por garantía, perfecto. Muy buenos colores para el precio.",
                base.minusDays(20), EstadoResena.APROBADA);
        resena(base, 14, 9, 4, "Cómodo y la batería dura toda la semana.",
                base.minusDays(35), EstadoResena.APROBADA);

        // Pendientes de moderación: la de Paula trae un teléfono y el panel muestra la alerta
        resena(base, 16, 4, 5, "Los switches se sienten firmes y el software deja cambiar las luces por tecla.",
                base.minusDays(1), EstadoResena.PENDIENTE);
        resena(base, 17, 7, 2, "Llegó con un píxel muerto. Llámenme al 9 8765 4321 para coordinar el cambio.",
                base.minusDays(2), EstadoResena.PENDIENTE);
        resena(base, 18, 10, 4, "Rápido y frío. Le faltó traer el disipador que muestran las fotos.",
                base.minusDays(3), EstadoResena.PENDIENTE);

        log.info("Datos de ejemplo cargados: {} reseñas.", resenas.count());
    }

    /** El cliente sale de la orden de ejemplo y el nombre, del producto de ejemplo. */
    private void resena(LocalDateTime base, long ordenId, long productoId, int puntuacion, String comentario,
                        LocalDateTime fecha, EstadoResena estado) {
        DatosDeEjemplo.UsuarioEjemplo cliente = DatosDeEjemplo.orden(base, ordenId).cliente();
        Resena resena = new Resena(ordenId, cliente.id(), Formatos.nombreCorto(cliente.nombre()), productoId,
                DatosDeEjemplo.producto(productoId).nombre(), puntuacion, comentario, fecha);
        resena.moderar(estado);
        resenas.save(resena);
    }
}
