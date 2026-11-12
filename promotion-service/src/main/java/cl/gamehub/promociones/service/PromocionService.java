package cl.gamehub.promociones.service;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.promociones.dto.CuponDto;
import cl.gamehub.promociones.dto.PromocionDto;
import cl.gamehub.promociones.dto.PromocionSolicitud;
import cl.gamehub.promociones.model.Promocion;
import cl.gamehub.promociones.repository.PromocionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromocionService {

    private static final Logger log = LoggerFactory.getLogger(PromocionService.class);

    private final PromocionRepository promociones;

    public PromocionService(PromocionRepository promociones) {
        this.promociones = promociones;
    }

    /** El carrito (y order-service al comprar) preguntan si el cupón sirve hoy. */
    @Transactional(readOnly = true)
    public CuponDto validar(String codigo) {
        return cupon(vigente(codigo, LocalDate.now()));
    }

    /**
     * order-service avisa que una compra usó el cupón. Se vuelve a validar porque entre el carrito y el pago
     * el cupón pudo vencer o agotarse; si sigue vigente, se cuenta el uso.
     */
    @Transactional
    public CuponDto registrarUso(String codigo) {
        Promocion promocion = vigente(codigo, LocalDate.now());
        promocion.registrarUso();
        log.info("Uso registrado del cupón {}: {} de {}", promocion.getCodigo(), promocion.getUsos(),
                promocion.getUsosMaximos());
        return cupon(promocion);
    }

    /**
     * Busca el cupón y revisa que sirva hoy. Cada caso tiene su mensaje: no existe (404),
     * desactivado, vencido, todavía no rige o sin usos disponibles (400).
     */
    private Promocion vigente(String codigo, LocalDate hoy) {
        String limpio = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            throw ApiException.regla("Escribe un código de cupón.");
        }
        Promocion p = promociones.findByCodigoIgnoreCase(limpio).orElseThrow(() ->
                ApiException.noEncontrado("El cupón «" + limpio + "» no existe. Revisa cómo lo escribiste."));
        if (!p.isActiva()) {
            throw ApiException.regla("El cupón " + limpio + " no está disponible.");
        }
        if (hoy.isAfter(p.getTermino())) {
            throw ApiException.regla("El cupón " + limpio + " venció el " + Formatos.fecha(p.getTermino()) + ".");
        }
        if (hoy.isBefore(p.getInicio())) {
            throw ApiException.regla("El cupón " + limpio + " comienza a regir el " + Formatos.fecha(p.getInicio()) + ".");
        }
        if (p.getUsos() >= p.getUsosMaximos()) {
            throw ApiException.regla("El cupón " + limpio + " ya alcanzó su tope de usos.");
        }
        return p;
    }

    // ---------- Administración ----------

    /** Del inicio más reciente al más antiguo, con el estado de cada cupón calculado para hoy. */
    @Transactional(readOnly = true)
    public List<PromocionDto> listar() {
        LocalDate hoy = LocalDate.now();
        return promociones.findAllByOrderByInicioDesc().stream().map(p -> admin(p, hoy)).toList();
    }

    @Transactional
    public PromocionDto crear(PromocionSolicitud s) {
        String codigo = s.codigo().trim().toUpperCase(Locale.ROOT);
        validarFechas(s);
        if (promociones.existsByCodigoIgnoreCase(codigo)) {
            throw ApiException.conflicto("Ya existe un cupón con el código " + codigo + ".");
        }
        Promocion p = promociones.save(new Promocion(codigo, s.porcentaje(), s.tope(), s.inicio(), s.termino(),
                s.usosMaximos()));
        log.info("Cupón creado: {} ({} % con tope de {}), del {} al {}", codigo, p.getPorcentaje(),
                Formatos.clp(p.getTope()), Formatos.fecha(p.getInicio()), Formatos.fecha(p.getTermino()));
        return admin(p, LocalDate.now());
    }

    @Transactional
    public PromocionDto actualizar(Long id, PromocionSolicitud s) {
        Promocion p = buscar(id);
        String codigo = s.codigo().trim().toUpperCase(Locale.ROOT);
        validarFechas(s);
        if (promociones.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw ApiException.conflicto("Ya existe otro cupón con el código " + codigo + ".");
        }
        if (s.usosMaximos() < p.getUsos()) {
            throw ApiException.regla("El tope de usos no puede ser menor que los " + p.getUsos() + " usos que ya tiene.");
        }
        p.setCodigo(codigo);
        p.setPorcentaje(s.porcentaje());
        p.setTope(s.tope());
        p.setInicio(s.inicio());
        p.setTermino(s.termino());
        p.setUsosMaximos(s.usosMaximos());
        log.info("Cupón {} actualizado: {} %, tope {}, {} usos máximos", codigo, p.getPorcentaje(),
                Formatos.clp(p.getTope()), p.getUsosMaximos());
        return admin(p, LocalDate.now());
    }

    @Transactional
    public PromocionDto cambiarActivo(Long id, boolean activa) {
        Promocion p = buscar(id);
        p.setActiva(activa);
        log.info("Cupón {} {}", p.getCodigo(), activa ? "activado" : "desactivado");
        return admin(p, LocalDate.now());
    }

    private void validarFechas(PromocionSolicitud s) {
        if (s.termino().isBefore(s.inicio())) {
            throw ApiException.regla("La fecha de término no puede ser anterior a la de inicio.");
        }
    }

    private Promocion buscar(Long id) {
        return promociones.findById(id).orElseThrow(() -> ApiException.noEncontrado("La promoción no existe."));
    }

    private static CuponDto cupon(Promocion p) {
        return new CuponDto(p.getCodigo(), p.getPorcentaje(), p.getTope(), p.getInicio(), p.getTermino());
    }

    /** El estado sigue el mismo orden de revisión que vigente(): primero si está activa, después las fechas y los usos. */
    private static PromocionDto admin(Promocion p, LocalDate hoy) {
        String estado;
        if (!p.isActiva()) {
            estado = "INACTIVA";
        } else if (hoy.isAfter(p.getTermino())) {
            estado = "VENCIDA";
        } else if (hoy.isBefore(p.getInicio())) {
            estado = "PROGRAMADA";
        } else if (p.getUsos() >= p.getUsosMaximos()) {
            estado = "AGOTADA";
        } else {
            estado = "VIGENTE";
        }
        return new PromocionDto(p.getId(), p.getCodigo(), p.getPorcentaje(), p.getTope(), p.getInicio(), p.getTermino(),
                p.getUsosMaximos(), p.getUsos(), p.isActiva(), estado);
    }
}
