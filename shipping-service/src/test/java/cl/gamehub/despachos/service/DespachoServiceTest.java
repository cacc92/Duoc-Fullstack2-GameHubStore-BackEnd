package cl.gamehub.despachos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.despachos.dto.ActualizarDespachoSolicitud;
import cl.gamehub.despachos.dto.DespachoDto;
import cl.gamehub.despachos.dto.EventoDto;
import cl.gamehub.despachos.dto.NuevoDespachoSolicitud;
import cl.gamehub.despachos.model.Despacho;
import cl.gamehub.despachos.model.EstadoDespacho;
import cl.gamehub.despachos.repository.DespachoRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

/** Reglas de DespachoService con un doble del repositorio: sin base de datos ni Spring. */
@ExtendWith(MockitoExtension.class)
class DespachoServiceTest {

    private static final LocalDate HOY = LocalDate.now();

    @Mock
    private DespachoRepository despachos;

    @InjectMocks
    private DespachoService servicio;

    // ---------- Generar ----------

    @Test
    void generarUsaElSeguimientoYLaFechaEstimadaPorDefecto() {
        // Given: la orden 5 todavía no tiene despacho
        cuandoSeGuarda("GHX-70005");
        ArgumentCaptor<Despacho> guardado = ArgumentCaptor.forClass(Despacho.class);

        // When: order-service no manda seguimiento ni fecha estimada
        DespachoDto despacho = servicio.generar(new NuevoDespachoSolicitud(5L, "GH-2026-000005", null, null,
                " Camila Rojas ", "Av. Los Aromos 1450, depto. 32", "Ñuñoa", "  "));

        // Then: GHX- y 70000 + 5, llegada en 3 días y un solo evento PREPARANDO
        assertThat(despacho.ordenId()).isEqualTo(5L);
        assertThat(despacho.ordenNumero()).isEqualTo("GH-2026-000005");
        assertThat(despacho.seguimiento()).isEqualTo("GHX-70005");
        assertThat(despacho.fechaEstimada()).isEqualTo(HOY.plusDays(3));
        assertThat(despacho.estado()).isEqualTo(EstadoDespacho.PREPARANDO);
        assertThat(despacho.entregadoEn()).isNull();
        assertThat(despacho.eventos()).extracting(EventoDto::estado).containsExactly(EstadoDespacho.PREPARANDO);

        // Y la copia de los datos de entrega queda limpia: sin espacios de más y los vacíos como nulos
        verify(despachos).save(guardado.capture());
        Despacho nuevo = guardado.getValue();
        assertThat(nuevo.getDestinatario()).isEqualTo("Camila Rojas");
        assertThat(nuevo.getDireccion()).isEqualTo("Av. Los Aromos 1450, depto. 32");
        assertThat(nuevo.getComuna()).isEqualTo("Ñuñoa");
        assertThat(nuevo.getRegion()).isNull();
        assertThat(nuevo.getCreadoEn()).isEqualTo(despacho.eventos().getFirst().fecha());
    }

    @Test
    void generarGuardaElSeguimientoEscritoEnMayusculas() {
        cuandoSeGuarda("GHX-ABC123");

        DespachoDto despacho = servicio.generar(new NuevoDespachoSolicitud(5L, "GH-2026-000005", " ghx-abc123 ",
                HOY.plusDays(7), null, null, null, null));

        assertThat(despacho.seguimiento()).isEqualTo("GHX-ABC123");
        assertThat(despacho.fechaEstimada()).isEqualTo(HOY.plusDays(7));
    }

    @Test
    void unaOrdenNoTieneDosDespachos() {
        when(despachos.existsByOrdenId(1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.generar(nuevo(1L, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Esta orden ya tiene un despacho.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(despachos, never()).save(any());
    }

    @Test
    void dosDespachosNoCompartenSeguimiento() {
        when(despachos.existsByOrdenId(5L)).thenReturn(false);
        when(despachos.existsBySeguimiento("GHX-61873")).thenReturn(true);

        assertThatThrownBy(() -> servicio.generar(nuevo(5L, "ghx-61873")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe un despacho con el seguimiento GHX-61873.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(despachos, never()).save(any());
    }

    // ---------- Avanzar ----------

    @Test
    void sinDespachoPideGenerarloPrimero() {
        when(despachos.findByOrdenId(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.actualizar(5L, avance(EstadoDespacho.DESPACHADO)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Primero genera el despacho de esta orden.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void elDespachoSoloAvanza() {
        // Given: el despacho de la orden 2 va en tránsito
        Despacho enTransito = despacho(2L, "GHX-70214", EstadoDespacho.EN_TRANSITO);
        when(despachos.findByOrdenId(2L)).thenReturn(Optional.of(enTransito));

        // When / Then: ni el mismo estado ni uno anterior, y no se agregan eventos
        assertThatThrownBy(() -> servicio.actualizar(2L, avance(EstadoDespacho.EN_TRANSITO)))
                .isInstanceOf(ApiException.class)
                .hasMessage("El despacho solo avanza: elige un estado posterior al actual.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThatThrownBy(() -> servicio.actualizar(2L, avance(EstadoDespacho.DESPACHADO)))
                .hasMessage("El despacho solo avanza: elige un estado posterior al actual.");
        assertThat(enTransito.getEventos()).hasSize(3);
    }

    @Test
    void entregarFijaLaFechaDeEntregaYAgregaElEvento() {
        when(despachos.findByOrdenId(2L)).thenReturn(Optional.of(despacho(2L, "GHX-70214", EstadoDespacho.EN_TRANSITO)));

        DespachoDto entregado = servicio.actualizar(2L, avance(EstadoDespacho.ENTREGADO));

        assertThat(entregado.estado()).isEqualTo(EstadoDespacho.ENTREGADO);
        assertThat(entregado.entregadoEn()).isNotNull().isEqualTo(entregado.eventos().getLast().fecha());
        assertThat(entregado.eventos()).extracting(EventoDto::estado).containsExactly(EstadoDespacho.PREPARANDO,
                EstadoDespacho.DESPACHADO, EstadoDespacho.EN_TRANSITO, EstadoDespacho.ENTREGADO);
    }

    @Test
    void sePuedeSaltarUnEstadoMientrasSeaPosterior() {
        when(despachos.findByOrdenId(8L)).thenReturn(Optional.of(despacho(8L, "GHX-70341", EstadoDespacho.PREPARANDO)));

        DespachoDto enTransito = servicio.actualizar(8L, avance(EstadoDespacho.EN_TRANSITO));

        assertThat(enTransito.estado()).isEqualTo(EstadoDespacho.EN_TRANSITO);
        assertThat(enTransito.entregadoEn()).isNull();
        assertThat(enTransito.eventos()).hasSize(2);
    }

    @Test
    void avanzarPuedeCambiarElSeguimientoYLaFechaEstimada() {
        when(despachos.findByOrdenId(8L)).thenReturn(Optional.of(despacho(8L, "GHX-70341", EstadoDespacho.PREPARANDO)));
        when(despachos.existsBySeguimientoAndIdNot("CHX-123456", 8L)).thenReturn(false);

        DespachoDto despachado = servicio.actualizar(8L,
                new ActualizarDespachoSolicitud(EstadoDespacho.DESPACHADO, " chx-123456 ", HOY.plusDays(2)));

        assertThat(despachado.seguimiento()).isEqualTo("CHX-123456");
        assertThat(despachado.fechaEstimada()).isEqualTo(HOY.plusDays(2));
        assertThat(despachado.estado()).isEqualTo(EstadoDespacho.DESPACHADO);
    }

    @Test
    void avanzarSinSeguimientoNiFechaLosConserva() {
        Despacho preparando = despacho(8L, "GHX-70341", EstadoDespacho.PREPARANDO);
        LocalDate estimada = preparando.getFechaEstimada();
        when(despachos.findByOrdenId(8L)).thenReturn(Optional.of(preparando));

        DespachoDto despachado = servicio.actualizar(8L, new ActualizarDespachoSolicitud(EstadoDespacho.DESPACHADO, "  ", null));

        assertThat(despachado.seguimiento()).isEqualTo("GHX-70341");
        assertThat(despachado.fechaEstimada()).isEqualTo(estimada);
        verify(despachos, never()).existsBySeguimientoAndIdNot(any(), any());
    }

    @Test
    void avanzarNoUsaElSeguimientoDeOtroDespacho() {
        when(despachos.findByOrdenId(8L)).thenReturn(Optional.of(despacho(8L, "GHX-70341", EstadoDespacho.PREPARANDO)));
        when(despachos.existsBySeguimientoAndIdNot("GHX-61873", 8L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.actualizar(8L,
                new ActualizarDespachoSolicitud(EstadoDespacho.DESPACHADO, "GHX-61873", null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe otro despacho con el seguimiento GHX-61873.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---------- Consultas ----------

    @Test
    void elSeguimientoNoDistingueMayusculasNiEspacios() {
        when(despachos.findBySeguimiento("GHX-61873")).thenReturn(Optional.of(despacho(1L, "GHX-61873", EstadoDespacho.ENTREGADO)));

        DespachoDto despacho = servicio.seguimiento(" ghx-61873 ");

        assertThat(despacho.ordenId()).isEqualTo(1L);
        assertThat(despacho.estado()).isEqualTo(EstadoDespacho.ENTREGADO);
        assertThat(despacho.eventos()).hasSize(4);
    }

    @Test
    void unSeguimientoQueNoExisteResponde404() {
        when(despachos.findBySeguimiento("GHX-00000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.seguimiento("ghx-00000"))
                .isInstanceOf(ApiException.class)
                .hasMessage("No encontramos un despacho con el seguimiento GHX-00000.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void buscarPorOrdenesSinIdsNoConsultaLaBase() {
        assertThat(servicio.buscarPorOrdenes(null)).isEmpty();
        assertThat(servicio.buscarPorOrdenes(List.of())).isEmpty();
        verifyNoInteractions(despachos);
    }

    @Test
    void buscarPorOrdenesOmiteLasOrdenesSinDespacho() {
        when(despachos.findByOrdenIdInOrderByOrdenIdAsc(List.of(1L, 2L, 3L))).thenReturn(List.of(
                despacho(1L, "GHX-61873", EstadoDespacho.ENTREGADO), despacho(2L, "GHX-70214", EstadoDespacho.EN_TRANSITO)));

        assertThat(servicio.buscarPorOrdenes(List.of(1L, 2L, 3L))).extracting(DespachoDto::seguimiento)
                .containsExactly("GHX-61873", "GHX-70214");
    }

    @Test
    void listarFiltraPorEstadoSoloSiSeIndica() {
        Despacho enTransito = despacho(2L, "GHX-70214", EstadoDespacho.EN_TRANSITO);
        when(despachos.findAllByOrderByCreadoEnDesc()).thenReturn(List.of(enTransito,
                despacho(1L, "GHX-61873", EstadoDespacho.ENTREGADO)));
        when(despachos.findByEstadoOrderByCreadoEnDesc(EstadoDespacho.EN_TRANSITO)).thenReturn(List.of(enTransito));

        assertThat(servicio.listar(null)).hasSize(2);
        assertThat(servicio.listar(EstadoDespacho.EN_TRANSITO)).extracting(DespachoDto::ordenId).containsExactly(2L);
    }

    private void cuandoSeGuarda(String seguimiento) {
        when(despachos.existsByOrdenId(5L)).thenReturn(false);
        when(despachos.existsBySeguimiento(seguimiento)).thenReturn(false);
        when(despachos.save(any(Despacho.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static NuevoDespachoSolicitud nuevo(long ordenId, String seguimiento) {
        return new NuevoDespachoSolicitud(ordenId, "GH-2026-%06d".formatted(ordenId), seguimiento, null, null, null, null, null);
    }

    private static ActualizarDespachoSolicitud avance(EstadoDespacho estado) {
        return new ActualizarDespachoSolicitud(estado, null, null);
    }

    /** Un despacho generado hace dos días que avanzó hasta el estado indicado, con un evento por paso. */
    private static Despacho despacho(long ordenId, String seguimiento, EstadoDespacho hasta) {
        LocalDateTime inicio = LocalDateTime.now().minusDays(2);
        Despacho d = new Despacho(ordenId, "GH-2026-%06d".formatted(ordenId), seguimiento, HOY.plusDays(2),
                "Camila Rojas", "Av. Los Aromos 1450, depto. 32", "Ñuñoa", "Región Metropolitana de Santiago", inicio);
        for (EstadoDespacho estado : EstadoDespacho.values()) {
            if (estado.ordinal() > 0 && estado.ordinal() <= hasta.ordinal()) {
                d.avanzar(estado, inicio.plusHours(6L * estado.ordinal()));
            }
        }
        ReflectionTestUtils.setField(d, "id", ordenId);
        return d;
    }
}
