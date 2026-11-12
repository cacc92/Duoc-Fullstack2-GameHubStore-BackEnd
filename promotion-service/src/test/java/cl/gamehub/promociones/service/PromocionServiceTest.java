package cl.gamehub.promociones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.promociones.dto.CuponDto;
import cl.gamehub.promociones.dto.PromocionDto;
import cl.gamehub.promociones.dto.PromocionSolicitud;
import cl.gamehub.promociones.model.Promocion;
import cl.gamehub.promociones.repository.PromocionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/** Reglas de PromocionService con un doble del repositorio: sin base de datos ni Spring. */
@ExtendWith(MockitoExtension.class)
class PromocionServiceTest {

    private static final LocalDate HOY = LocalDate.now();

    @Mock
    private PromocionRepository promociones;

    @InjectMocks
    private PromocionService servicio;

    // ---------- Validación del cupón ----------

    @Test
    void validarDevuelveElCuponSinImportarMayusculasNiEspacios() {
        // Given: GAMER10 rige desde hace 9 días y le quedan usos
        when(promociones.findByCodigoIgnoreCase("GAMER10")).thenReturn(Optional.of(gamer10()));

        // When: el cliente lo escribe en minúsculas y con espacios
        CuponDto cupon = servicio.validar("  gamer10 ");

        // Then
        assertThat(cupon.codigo()).isEqualTo("GAMER10");
        assertThat(cupon.porcentaje()).isEqualTo(10);
        assertThat(cupon.tope()).isEqualTo(30_000);
        assertThat(cupon.termino()).isEqualTo(HOY.plusDays(20));
    }

    @Test
    void validarSinCodigoPideEscribirlo() {
        assertThatThrownBy(() -> servicio.validar(null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Escribe un código de cupón.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> servicio.validar("   ")).hasMessage("Escribe un código de cupón.");
        verify(promociones, never()).findByCodigoIgnoreCase(any());
    }

    @Test
    void unCuponQueNoExisteResponde404() {
        when(promociones.findByCodigoIgnoreCase("NOEXISTE")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.validar("noexiste"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El cupón «NOEXISTE» no existe. Revisa cómo lo escribiste.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unCuponDesactivadoNoEstaDisponible() {
        Promocion desactivado = gamer10();
        desactivado.setActiva(false);
        when(promociones.findByCodigoIgnoreCase("GAMER10")).thenReturn(Optional.of(desactivado));

        assertThatThrownBy(() -> servicio.validar("GAMER10"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El cupón GAMER10 no está disponible.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unCuponVencidoDiceCuandoVencio() {
        // Given: CYBER25 terminó hace 10 días
        Promocion cyber = promocion("CYBER25", HOY.minusDays(40), HOY.minusDays(10), 100, 0);
        when(promociones.findByCodigoIgnoreCase("CYBER25")).thenReturn(Optional.of(cyber));

        // When / Then: el mensaje trae la fecha en formato dd-MM-yyyy
        assertThatThrownBy(() -> servicio.validar("CYBER25"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El cupón CYBER25 venció el " + Formatos.fecha(HOY.minusDays(10)) + ".")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unCuponProgramadoDiceCuandoComienzaARegir() {
        Promocion setup = promocion("SETUP15", HOY.plusDays(5), HOY.plusDays(35), 100, 0);
        when(promociones.findByCodigoIgnoreCase("SETUP15")).thenReturn(Optional.of(setup));

        assertThatThrownBy(() -> servicio.validar("SETUP15"))
                .hasMessage("El cupón SETUP15 comienza a regir el " + Formatos.fecha(HOY.plusDays(5)) + ".");
    }

    @Test
    void unCuponAgotadoNoSePuedeUsar() {
        Promocion agotado = promocion("GAMER10", HOY.minusDays(9), HOY.plusDays(20), 200, 200);
        when(promociones.findByCodigoIgnoreCase("GAMER10")).thenReturn(Optional.of(agotado));

        assertThatThrownBy(() -> servicio.validar("GAMER10"))
                .hasMessage("El cupón GAMER10 ya alcanzó su tope de usos.");
    }

    @Test
    void elDiaDeInicioYElDeTerminoElCuponSirve() {
        // Given: un cupón que rige solo hoy (inicio y término están incluidos en la vigencia)
        when(promociones.findByCodigoIgnoreCase("SOLOHOY")).thenReturn(Optional.of(promocion("SOLOHOY", HOY, HOY, 1, 0)));

        // When / Then
        assertThat(servicio.validar("SOLOHOY").codigo()).isEqualTo("SOLOHOY");
    }

    // ---------- Usos ----------

    @Test
    void registrarUsoSumaUnUsoAlCuponVigente() {
        // Given: GAMER10 con 34 usos
        Promocion gamer = gamer10();
        when(promociones.findByCodigoIgnoreCase("GAMER10")).thenReturn(Optional.of(gamer));

        // When
        CuponDto cupon = servicio.registrarUso("gamer10");

        // Then: la entidad está administrada por JPA, así que el cambio se guarda al terminar la transacción
        assertThat(gamer.getUsos()).isEqualTo(35);
        assertThat(cupon.codigo()).isEqualTo("GAMER10");
    }

    @Test
    void registrarUsoNoCuentaSiElCuponYaSeAgoto() {
        Promocion agotado = promocion("GAMER10", HOY.minusDays(9), HOY.plusDays(20), 200, 200);
        when(promociones.findByCodigoIgnoreCase("GAMER10")).thenReturn(Optional.of(agotado));

        assertThatThrownBy(() -> servicio.registrarUso("GAMER10"))
                .hasMessage("El cupón GAMER10 ya alcanzó su tope de usos.");
        assertThat(agotado.getUsos()).isEqualTo(200);
    }

    @Test
    void calcularDescuentoRespetaElPorcentajeElTopeYElSubtotal() {
        Promocion gamer = gamer10(); // 10 % con tope de $30.000

        assertThat(gamer.calcularDescuento(100_000)).isEqualTo(10_000);
        assertThat(gamer.calcularDescuento(764_975)).isEqualTo(30_000); // el 10 % serían $76.498: manda el tope
        assertThat(gamer.calcularDescuento(15)).isEqualTo(2);           // $1,5 se redondea a $2
        assertThat(gamer.calcularDescuento(0)).isZero();
    }

    // ---------- Administración ----------

    @Test
    void listarCalculaElEstadoDeCadaCupon() {
        // Given: un cupón en cada estado posible
        Promocion inactiva = promocion("PAUSA10", HOY.minusDays(1), HOY.plusDays(1), 10, 0);
        inactiva.setActiva(false);
        when(promociones.findAllByOrderByInicioDesc()).thenReturn(List.of(
                promocion("SETUP15", HOY.plusDays(5), HOY.plusDays(35), 100, 0),
                gamer10(),
                promocion("AGOTA5", HOY.minusDays(20), HOY.plusDays(10), 50, 50),
                promocion("CYBER25", HOY.minusDays(40), HOY.minusDays(10), 100, 100),
                inactiva));

        // When
        List<PromocionDto> lista = servicio.listar();

        // Then
        assertThat(lista).extracting(PromocionDto::estado)
                .containsExactly("PROGRAMADA", "VIGENTE", "AGOTADA", "VENCIDA", "INACTIVA");
        assertThat(lista.get(1).id()).isEqualTo(1L);
        assertThat(lista.get(1).usos()).isEqualTo(34);
    }

    @Test
    void crearGuardaElCodigoEnMayusculas() {
        when(promociones.existsByCodigoIgnoreCase("VERANO20")).thenReturn(false);
        when(promociones.save(any(Promocion.class))).thenAnswer(i -> i.getArgument(0));

        PromocionDto creada = servicio.crear(solicitud(" verano20 ", HOY, HOY.plusDays(30), 150));

        assertThat(creada.codigo()).isEqualTo("VERANO20");
        assertThat(creada.usos()).isZero();
        assertThat(creada.activa()).isTrue();
        assertThat(creada.estado()).isEqualTo("VIGENTE");
    }

    @Test
    void crearRechazaUnCodigoRepetido() {
        when(promociones.existsByCodigoIgnoreCase("GAMER10")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(solicitud("gamer10", HOY, HOY.plusDays(30), 150)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe un cupón con el código GAMER10.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(promociones, never()).save(any());
    }

    @Test
    void crearRechazaUnTerminoAnteriorAlInicio() {
        assertThatThrownBy(() -> servicio.crear(solicitud("VERANO20", HOY.plusDays(10), HOY, 150)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La fecha de término no puede ser anterior a la de inicio.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(promociones, never()).save(any());
    }

    @Test
    void actualizarCambiaLosDatosYConservaLosUsos() {
        // Given: GAMER10 (id 1) con 34 usos
        when(promociones.findById(1L)).thenReturn(Optional.of(gamer10()));
        when(promociones.existsByCodigoIgnoreCaseAndIdNot("GAMER12", 1L)).thenReturn(false);

        // When
        PromocionDto editada = servicio.actualizar(1L, new PromocionSolicitud("gamer12", 12, 35_000,
                HOY.minusDays(9), HOY.plusDays(40), 300));

        // Then
        assertThat(editada.codigo()).isEqualTo("GAMER12");
        assertThat(editada.porcentaje()).isEqualTo(12);
        assertThat(editada.tope()).isEqualTo(35_000);
        assertThat(editada.inicio()).isEqualTo(HOY.minusDays(9));
        assertThat(editada.termino()).isEqualTo(HOY.plusDays(40));
        assertThat(editada.usosMaximos()).isEqualTo(300);
        assertThat(editada.usos()).isEqualTo(34);
    }

    @Test
    void actualizarRechazaElCodigoDeOtroCupon() {
        when(promociones.findById(1L)).thenReturn(Optional.of(gamer10()));
        when(promociones.existsByCodigoIgnoreCaseAndIdNot("SETUP15", 1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(1L, solicitud("SETUP15", HOY, HOY.plusDays(30), 200)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe otro cupón con el código SETUP15.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void actualizarNoDejaElTopeDeUsosBajoLosUsosQueYaTiene() {
        when(promociones.findById(1L)).thenReturn(Optional.of(gamer10()));
        when(promociones.existsByCodigoIgnoreCaseAndIdNot("GAMER10", 1L)).thenReturn(false);

        assertThatThrownBy(() -> servicio.actualizar(1L, solicitud("GAMER10", HOY, HOY.plusDays(30), 20)))
                .isInstanceOf(ApiException.class)
                .hasMessage("El tope de usos no puede ser menor que los 34 usos que ya tiene.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void actualizarTambienRevisaLasFechas() {
        when(promociones.findById(1L)).thenReturn(Optional.of(gamer10()));

        assertThatThrownBy(() -> servicio.actualizar(1L, solicitud("GAMER10", HOY, HOY.minusDays(1), 200)))
                .hasMessage("La fecha de término no puede ser anterior a la de inicio.");
    }

    @Test
    void unaPromocionQueNoExisteResponde404() {
        when(promociones.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiarActivo(99L, false))
                .isInstanceOf(ApiException.class)
                .hasMessage("La promoción no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> servicio.actualizar(99L, solicitud("GAMER10", HOY, HOY, 1)))
                .hasMessage("La promoción no existe.");
    }

    @Test
    void cambiarActivoDesactivaYReactivaElCupon() {
        when(promociones.findById(1L)).thenReturn(Optional.of(gamer10()));

        assertThat(servicio.cambiarActivo(1L, false).estado()).isEqualTo("INACTIVA");
        assertThat(servicio.cambiarActivo(1L, true).estado()).isEqualTo("VIGENTE");
    }

    /** GAMER10 como en los datos de ejemplo: 10 % con tope de $30.000, 34 de 200 usos. */
    private static Promocion gamer10() {
        Promocion p = promocion("GAMER10", HOY.minusDays(9), HOY.plusDays(20), 200, 34);
        ReflectionTestUtils.setField(p, "id", 1L);
        return p;
    }

    private static Promocion promocion(String codigo, LocalDate inicio, LocalDate termino, int usosMaximos, int usos) {
        Promocion p = new Promocion(codigo, 10, 30_000, inicio, termino, usosMaximos);
        p.setUsos(usos);
        return p;
    }

    private static PromocionSolicitud solicitud(String codigo, LocalDate inicio, LocalDate termino, int usosMaximos) {
        return new PromocionSolicitud(codigo, 20, 40_000, inicio, termino, usosMaximos);
    }
}
