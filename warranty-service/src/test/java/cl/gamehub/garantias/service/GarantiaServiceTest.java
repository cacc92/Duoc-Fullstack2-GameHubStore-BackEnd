package cl.gamehub.garantias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.comun.util.Formatos;
import cl.gamehub.garantias.client.OrdenClient;
import cl.gamehub.garantias.dto.CrearGarantiaSolicitud;
import cl.gamehub.garantias.dto.GarantiaDto;
import cl.gamehub.garantias.dto.LineaOrdenInternaDto;
import cl.gamehub.garantias.dto.OrdenInternaDto;
import cl.gamehub.garantias.dto.ResolverGarantiaSolicitud;
import cl.gamehub.garantias.model.EstadoGarantia;
import cl.gamehub.garantias.model.Garantia;
import cl.gamehub.garantias.model.MotivoGarantia;
import cl.gamehub.garantias.repository.GarantiaRepository;
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

/** Reglas de GarantiaService con dobles de prueba: el repositorio y order-service son mocks. */
@ExtendWith(MockitoExtension.class)
class GarantiaServiceTest {

    /** Martina (usuario 5), tal como llega en su token. */
    private static final UsuarioAutenticado MARTINA =
            new UsuarioAutenticado(5L, "martina@gamehub.test", "Martina Castro", Rol.CLIENTE);

    @Mock
    private GarantiaRepository garantias;

    @Mock
    private OrdenClient ordenes;

    @InjectMocks
    private GarantiaService servicio;

    @Test
    void crearRechazaLaOrdenDeOtroCliente() {
        // Given: la orden 10 es de Joaquín (usuario 6), no de Martina
        when(ordenes.buscar(10L)).thenReturn(orden(6L, "ENTREGADA", hace(12)));

        // When / Then
        assertThatThrownBy(() -> servicio.crear(MARTINA, solicitud(2L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo puedes pedir garantía de tus propias compras.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(garantias, never()).save(any());
    }

    @Test
    void crearRechazaUnProductoQueNoEstaEnLaOrden() {
        // Given: la orden 10 trae la tarjeta gráfica (2) y los audífonos (6)
        when(ordenes.buscar(10L)).thenReturn(orden(5L, "ENTREGADA", hace(12)));

        // When / Then: el producto 9 no está en la orden
        assertThatThrownBy(() -> servicio.crear(MARTINA, solicitud(9L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("El producto no pertenece a esta orden.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(garantias, never()).save(any());
    }

    @Test
    void crearEsperaQueLaOrdenEsteEntregada() {
        // Given: la orden todavía va en camino
        when(ordenes.buscar(10L)).thenReturn(orden(5L, "EN_TRANSITO", null));

        // When / Then
        assertThatThrownBy(() -> servicio.crear(MARTINA, solicitud(2L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La garantía se puede pedir desde que recibes el producto.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(garantias, never()).save(any());
    }

    @Test
    void crearExigeLaFechaDeEntrega() {
        // Given: la orden dice ENTREGADA pero no trae la fecha: no hay desde cuándo contar el plazo
        when(ordenes.buscar(10L)).thenReturn(orden(5L, "ENTREGADA", null));

        // When / Then
        assertThatThrownBy(() -> servicio.crear(MARTINA, solicitud(2L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La garantía se puede pedir desde que recibes el producto.");
        verify(garantias, never()).save(any());
    }

    @Test
    void crearRechazaUnPlazoVencido() {
        // Given: la orden se entregó hace 7 meses y la garantía dura 6
        LocalDateTime entrega = LocalDateTime.now().minusMonths(7);
        when(ordenes.buscar(10L)).thenReturn(orden(5L, "ENTREGADA", entrega));
        String vencio = Formatos.fecha(entrega.toLocalDate().plusMonths(Garantia.MESES_GARANTIA));

        // When / Then: el mensaje dice el día en que venció
        assertThatThrownBy(() -> servicio.crear(MARTINA, solicitud(2L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("El plazo de garantía venció el " + vencio + ".")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(garantias, never()).save(any());
    }

    @Test
    void crearRechazaUnaSegundaSolicitudAbierta() {
        // Given: los audífonos de la orden 10 ya tienen una solicitud sin cerrar
        when(ordenes.buscar(10L)).thenReturn(orden(5L, "ENTREGADA", hace(12)));
        when(garantias.existsByOrdenIdAndProductoIdAndEstadoIn(10L, 6L, EstadoGarantia.ABIERTOS)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> servicio.crear(MARTINA, solicitud(6L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya hay una solicitud abierta para este producto.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(garantias, never()).save(any());
    }

    @Test
    void crearDejaPasarElErrorDeOrderService() {
        // Given: order-service respondió 404 y FeignInternoConfig lo convirtió en ApiException
        when(ordenes.buscar(10L)).thenThrow(ApiException.noEncontrado("La orden no existe."));

        // When / Then: el código y el mensaje llegan intactos
        assertThatThrownBy(() -> servicio.crear(MARTINA, solicitud(2L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La orden no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void crearGuardaLaSolicitudConSuCodigoYSuPlazo() {
        // Given: la orden 10 de Martina se entregó hace 12 días y la tarjeta gráfica no tiene solicitudes abiertas
        LocalDateTime entrega = hace(12);
        when(ordenes.buscar(10L)).thenReturn(orden(5L, "ENTREGADA", entrega));
        when(garantias.existsByOrdenIdAndProductoIdAndEstadoIn(10L, 2L, EstadoGarantia.ABIERTOS)).thenReturn(false);
        when(garantias.save(any(Garantia.class))).thenAnswer(i -> {
            Garantia g = i.getArgument(0);
            ReflectionTestUtils.setField(g, "id", 4L); // el id que asignaría la base
            return g;
        });

        // When
        GarantiaDto creada = servicio.crear(MARTINA, new CrearGarantiaSolicitud(10L, 2L, MotivoGarantia.FALLA,
                "  La tarjeta se apaga sola a los pocos minutos de jugar.  "));

        // Then: queda a nombre del usuario del token, con código, plazo de 6 meses y los nombres que informó order-service
        ArgumentCaptor<Garantia> guardada = ArgumentCaptor.forClass(Garantia.class);
        verify(garantias).save(guardada.capture());
        assertThat(guardada.getValue().getUsuarioId()).isEqualTo(5L);
        assertThat(creada.codigo()).isEqualTo("GAR-0004");
        assertThat(creada.estado()).isEqualTo(EstadoGarantia.ABIERTA);
        assertThat(creada.plazo()).isEqualTo(entrega.toLocalDate().plusMonths(6));
        assertThat(creada.ordenNumero()).isEqualTo("GH-2026-000010");
        assertThat(creada.producto()).isEqualTo("Tarjeta gráfica Voltra X70 OC 12 GB");
        assertThat(creada.cliente()).isEqualTo("Martina Castro");
        assertThat(creada.descripcion()).isEqualTo("La tarjeta se apaga sola a los pocos minutos de jugar.");
        assertThat(creada.actualizadaEn()).isNull();
    }

    @Test
    void miasTraeLasSolicitudesDelUsuario() {
        // Given
        when(garantias.findByUsuarioIdOrderByCreadaEnDesc(5L))
                .thenReturn(List.of(garantia(1L, EstadoGarantia.ABIERTA, hace(2))));

        // When
        List<GarantiaDto> mias = servicio.mias(5L);

        // Then
        assertThat(mias).extracting(GarantiaDto::codigo).containsExactly("GAR-0001");
    }

    @Test
    void todasMuestraPrimeroLasQueEsperanAlOperador() {
        // Given: el repositorio las entrega de la más nueva a la más antigua
        when(garantias.findAllByOrderByCreadaEnDesc()).thenReturn(List.of(
                garantia(1L, EstadoGarantia.RESUELTA, hace(1)),
                garantia(2L, EstadoGarantia.ABIERTA, hace(2)),
                garantia(3L, EstadoGarantia.RECHAZADA, hace(3)),
                garantia(4L, EstadoGarantia.EN_REVISION, hace(4))));

        // When
        List<GarantiaDto> bandeja = servicio.todas();

        // Then: primero las abiertas, después las cerradas, y dentro de cada grupo se mantiene el orden por fecha
        assertThat(bandeja).extracting(GarantiaDto::codigo).containsExactly("GAR-0002", "GAR-0004", "GAR-0001", "GAR-0003");
    }

    @Test
    void resumenCuentaLasAbiertasYLasEnRevision() {
        // Given
        when(garantias.countByEstadoIn(EstadoGarantia.ABIERTOS)).thenReturn(2L);

        // When / Then
        assertThat(servicio.resumen().abiertas()).isEqualTo(2L);
    }

    @Test
    void resolverResponde404SiNoExiste() {
        // Given
        when(garantias.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.resolver(99L, new ResolverGarantiaSolicitud(EstadoGarantia.EN_REVISION, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La garantía no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void resolverNoCambiaUnaGarantiaCerrada() {
        // Given
        when(garantias.findById(2L)).thenReturn(Optional.of(garantia(2L, EstadoGarantia.RESUELTA, hace(30))));

        // When / Then
        assertThatThrownBy(() -> servicio.resolver(2L, new ResolverGarantiaSolicitud(EstadoGarantia.EN_REVISION, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Esta garantía ya está cerrada.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resolverNoDejaVolverAAbierta() {
        // Given
        when(garantias.findById(3L)).thenReturn(Optional.of(garantia(3L, EstadoGarantia.EN_REVISION, hace(4))));

        // When / Then
        assertThatThrownBy(() -> servicio.resolver(3L, new ResolverGarantiaSolicitud(EstadoGarantia.ABIERTA, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Elige «En revisión», «Resuelta» o «Rechazada».")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resolverNoCierraSinResolucion() {
        // Given
        Garantia abierta = garantia(1L, EstadoGarantia.ABIERTA, hace(2));
        when(garantias.findById(1L)).thenReturn(Optional.of(abierta));

        // When / Then: ni sin texto ni con solo espacios
        assertThatThrownBy(() -> servicio.resolver(1L, new ResolverGarantiaSolicitud(EstadoGarantia.RECHAZADA, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Una garantía no se cierra sin resolución. Escribe qué se hizo.");
        assertThatThrownBy(() -> servicio.resolver(1L, new ResolverGarantiaSolicitud(EstadoGarantia.RESUELTA, "   ")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Una garantía no se cierra sin resolución. Escribe qué se hizo.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(abierta.getEstado()).isEqualTo(EstadoGarantia.ABIERTA);
    }

    @Test
    void resolverEnRevisionSinTextoConservaLaResolucionAnterior() {
        // Given: el operador ya había anotado qué pidió al cliente
        Garantia enRevision = garantia(3L, EstadoGarantia.EN_REVISION, hace(4));
        when(garantias.findById(3L)).thenReturn(Optional.of(enRevision));

        // When
        GarantiaDto actualizada = servicio.resolver(3L, new ResolverGarantiaSolicitud(EstadoGarantia.EN_REVISION, null));

        // Then
        assertThat(actualizada.resolucion()).isEqualTo("Se pidió un video de la falla para evaluar el cambio.");
        assertThat(actualizada.actualizadaEn()).isAfter(hace(1));
    }

    @Test
    void resolverCierraConLaResolucion() {
        // Given
        Garantia abierta = garantia(1L, EstadoGarantia.ABIERTA, hace(2));
        when(garantias.findById(1L)).thenReturn(Optional.of(abierta));

        // When
        GarantiaDto resuelta = servicio.resolver(1L,
                new ResolverGarantiaSolicitud(EstadoGarantia.RESUELTA, "  Se cambiaron los audífonos por unos nuevos.  "));

        // Then: la entidad queda cerrada (JPA la actualiza al terminar la transacción)
        assertThat(resuelta.estado()).isEqualTo(EstadoGarantia.RESUELTA);
        assertThat(resuelta.resolucion()).isEqualTo("Se cambiaron los audífonos por unos nuevos.");
        assertThat(resuelta.actualizadaEn()).isNotNull();
        assertThat(abierta.cerrada()).isTrue();
    }

    // ---------- Ayudantes ----------

    private static LocalDateTime hace(int dias) {
        return LocalDateTime.now().minusDays(dias);
    }

    /** La orden 10 con la tarjeta gráfica (2) y los audífonos (6), como la respondería order-service. */
    private static OrdenInternaDto orden(Long usuarioId, String estado, LocalDateTime entregadaEn) {
        return new OrdenInternaDto(10L, "GH-2026-000010", usuarioId, DatosDeEjemplo.usuario(usuarioId).nombre(), estado,
                entregadaEn, List.of(new LineaOrdenInternaDto(2L, "Tarjeta gráfica Voltra X70 OC 12 GB"),
                        new LineaOrdenInternaDto(6L, "Audífonos Andesia Pulse 7.1")));
    }

    private static CrearGarantiaSolicitud solicitud(Long productoId) {
        return new CrearGarantiaSolicitud(10L, productoId, MotivoGarantia.FALLA,
                "Se corta el audio del lado izquierdo desde la semana pasada.");
    }

    /** Garantía ya guardada (con id y código) de Martina por los audífonos de la orden 10. */
    private static Garantia garantia(Long id, EstadoGarantia estado, LocalDateTime creadaEn) {
        Garantia g = new Garantia(10L, "GH-2026-000010", 5L, "Martina Castro", 6L, "Audífonos Andesia Pulse 7.1",
                MotivoGarantia.FALLA, "Se corta el audio del lado izquierdo desde la semana pasada.",
                LocalDate.now().plusMonths(5), creadaEn);
        ReflectionTestUtils.setField(g, "id", id);
        g.asignarCodigo();
        if (estado != EstadoGarantia.ABIERTA) {
            g.actualizar(estado, "Se pidió un video de la falla para evaluar el cambio.", creadaEn.plusDays(1));
        }
        return g;
    }
}
