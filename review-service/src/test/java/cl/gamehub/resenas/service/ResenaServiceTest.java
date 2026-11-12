package cl.gamehub.resenas.service;

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
import cl.gamehub.resenas.client.OrdenClient;
import cl.gamehub.resenas.dto.CrearResenaSolicitud;
import cl.gamehub.resenas.dto.LineaOrdenInternaDto;
import cl.gamehub.resenas.dto.MiResenaDto;
import cl.gamehub.resenas.dto.OrdenInternaDto;
import cl.gamehub.resenas.dto.ResenaAdminDto;
import cl.gamehub.resenas.dto.ResenaDto;
import cl.gamehub.resenas.dto.ResumenResenasDto;
import cl.gamehub.resenas.model.EstadoResena;
import cl.gamehub.resenas.model.Resena;
import cl.gamehub.resenas.repository.PromedioProducto;
import cl.gamehub.resenas.repository.ResenaRepository;
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

/** Reglas de ResenaService con dobles de prueba: el repositorio y order-service son mocks. */
@ExtendWith(MockitoExtension.class)
class ResenaServiceTest {

    /** Camila (usuario 3), tal como llega en su token. */
    private static final UsuarioAutenticado CAMILA =
            new UsuarioAutenticado(3L, "camila@gamehub.test", "Camila Rojas", Rol.CLIENTE);

    @Mock
    private ResenaRepository resenas;

    @Mock
    private OrdenClient ordenes;

    @InjectMocks
    private ResenaService servicio;

    @Test
    void publicasSoloPideLasAprobadas() {
        // Given: el producto 3 tiene una reseña aprobada
        when(resenas.findByProductoIdAndEstadoOrderByFechaDesc(3L, EstadoResena.APROBADA))
                .thenReturn(List.of(resena(1L, 3L, EstadoResena.APROBADA, "Instalación simple y temperaturas bajas.")));

        // When
        List<ResenaDto> lista = servicio.publicas(3L);

        // Then: sale con el autor abreviado que se guardó al crearla
        assertThat(lista).singleElement().satisfies(r -> {
            assertThat(r.autor()).isEqualTo("Camila R.");
            assertThat(r.estado()).isEqualTo(EstadoResena.APROBADA);
        });
    }

    @Test
    void resumenRedondeaElPromedioAUnDecimal() {
        // Given: la base calcula 14 / 3 = 4,666… para el producto 2
        when(resenas.promedios(EstadoResena.APROBADA))
                .thenReturn(List.of(new PromedioProducto(2L, 14 / 3.0, 3L), new PromedioProducto(3L, 5.0, 1L)));

        // When: una lista vacía equivale a no mandar el parámetro (todos los productos)
        List<ResumenResenasDto> resumen = servicio.resumen(List.of());

        // Then
        assertThat(resumen).containsExactly(new ResumenResenasDto(2L, 4.7, 3L), new ResumenResenasDto(3L, 5.0, 1L));
    }

    @Test
    void resumenConProductosConsultaSoloEsos() {
        // Given: de los productos 1 y 2, solo el 1 tiene reseñas aprobadas
        when(resenas.promedios(EstadoResena.APROBADA, List.of(1L, 2L)))
                .thenReturn(List.of(new PromedioProducto(1L, 4.25, 4L)));

        // When
        List<ResumenResenasDto> resumen = servicio.resumen(List.of(1L, 2L));

        // Then: 4,25 se redondea a 4,3 y el producto sin reseñas no aparece
        assertThat(resumen).containsExactly(new ResumenResenasDto(1L, 4.3, 4L));
        verify(resenas, never()).promedios(EstadoResena.APROBADA);
    }

    @Test
    void crearRechazaLaOrdenDeOtroCliente() {
        // Given: la orden 10 es de Martina (usuario 5)
        when(ordenes.buscar(10L)).thenReturn(orden(10L, 5L, "ENTREGADA"));

        // When / Then
        assertThatThrownBy(() -> servicio.crear(CAMILA, solicitud(10L, 3L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo puedes reseñar tus propias compras.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(resenas, never()).save(any());
    }

    @Test
    void crearRechazaUnProductoQueNoEstaEnLaOrden() {
        // Given: la orden 1 de Camila trae los productos 3 y 6
        when(ordenes.buscar(1L)).thenReturn(orden(1L, 3L, "ENTREGADA"));

        // When / Then: el producto 9 no está en la orden
        assertThatThrownBy(() -> servicio.crear(CAMILA, solicitud(1L, 9L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo puedes reseñar productos que compraste en esta orden.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(resenas, never()).save(any());
    }

    @Test
    void crearEsperaQueLaOrdenEsteEntregada() {
        // Given: la orden todavía va en camino
        when(ordenes.buscar(2L)).thenReturn(orden(2L, 3L, "EN_TRANSITO"));

        // When / Then
        assertThatThrownBy(() -> servicio.crear(CAMILA, solicitud(2L, 3L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Podrás reseñar este producto cuando lo recibas.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(resenas, never()).save(any());
    }

    @Test
    void crearRechazaUnaSegundaResenaDelMismoProducto() {
        // Given: Camila ya reseñó el producto 3 de la orden 1
        when(ordenes.buscar(1L)).thenReturn(orden(1L, 3L, "ENTREGADA"));
        when(resenas.existsByOrdenIdAndProductoId(1L, 3L)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> servicio.crear(CAMILA, solicitud(1L, 3L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya reseñaste este producto en esta orden.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(resenas, never()).save(any());
    }

    @Test
    void crearDejaPasarElErrorDeOrderService() {
        // Given: order-service respondió 404 y FeignInternoConfig lo convirtió en ApiException
        when(ordenes.buscar(99L)).thenThrow(ApiException.noEncontrado("La orden no existe."));

        // When / Then: el código y el mensaje llegan intactos
        assertThatThrownBy(() -> servicio.crear(CAMILA, solicitud(99L, 3L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La orden no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void crearGuardaUnaResenaPendienteConElAutorAbreviado() {
        // Given: la orden 1 de Camila está entregada y los audífonos (producto 6) no tienen reseña
        when(ordenes.buscar(1L)).thenReturn(orden(1L, 3L, "ENTREGADA"));
        when(resenas.existsByOrdenIdAndProductoId(1L, 6L)).thenReturn(false);
        when(resenas.save(any(Resena.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ResenaDto creada = servicio.crear(CAMILA, new CrearResenaSolicitud(1L, 6L, 4, "  Cómodos y con buen sonido.  "));

        // Then: queda pendiente, con el nombre abreviado y el nombre del producto que informó order-service
        ArgumentCaptor<Resena> guardada = ArgumentCaptor.forClass(Resena.class);
        verify(resenas).save(guardada.capture());
        assertThat(guardada.getValue().getUsuarioId()).isEqualTo(3L);
        assertThat(guardada.getValue().getProducto()).isEqualTo("Audífonos Andesia Pulse 7.1");
        assertThat(creada.autor()).isEqualTo("Camila R.");
        assertThat(creada.puntuacion()).isEqualTo(4);
        assertThat(creada.comentario()).isEqualTo("Cómodos y con buen sonido.");
        assertThat(creada.estado()).isEqualTo(EstadoResena.PENDIENTE);
    }

    @Test
    void miasTraeLasResenasDelUsuarioConSuOrdenYProducto() {
        // Given
        when(resenas.findByUsuarioIdOrderByFechaDesc(3L))
                .thenReturn(List.of(resena(1L, 3L, EstadoResena.PENDIENTE, "Instalación simple y temperaturas bajas.")));

        // When
        List<MiResenaDto> mias = servicio.mias(3L);

        // Then: Mis órdenes necesita la orden y el producto para marcar qué ya reseñó
        assertThat(mias).singleElement().satisfies(r -> {
            assertThat(r.ordenId()).isEqualTo(1L);
            assertThat(r.productoId()).isEqualTo(3L);
            assertThat(r.producto()).isEqualTo("Procesador Tectra R7 de 8 núcleos");
            assertThat(r.estado()).isEqualTo(EstadoResena.PENDIENTE);
        });
    }

    @Test
    void listarSinEstadoTraeTodasYAvisaLosDatosDeContacto() {
        // Given: una reseña con teléfono, otra con correo y otra sin datos de contacto
        when(resenas.findAllByOrderByFechaDesc()).thenReturn(List.of(
                resena(15L, 7L, EstadoResena.PENDIENTE, "Llegó con un píxel muerto. Llámenme al 9 8765 4321 para coordinar el cambio."),
                resena(16L, 10L, EstadoResena.PENDIENTE, "Escríbanme a paula.rivas@correo.cl y les mando fotos."),
                resena(1L, 3L, EstadoResena.APROBADA, "Instalación simple y temperaturas bajas.")));

        // When
        List<ResenaAdminDto> lista = servicio.listar(null);

        // Then
        assertThat(lista).extracting(ResenaAdminDto::alerta)
                .containsExactly("Contiene datos de contacto", "Contiene datos de contacto", null);
        assertThat(lista.getFirst().producto()).isEqualTo("Monitor Kairo 27\" QHD 170 Hz");
    }

    @Test
    void listarConEstadoFiltraEnLaBase() {
        // Given
        when(resenas.findByEstadoOrderByFechaDesc(EstadoResena.OCULTA)).thenReturn(List.of());

        // When
        List<ResenaAdminDto> lista = servicio.listar(EstadoResena.OCULTA);

        // Then
        assertThat(lista).isEmpty();
        verify(resenas, never()).findAllByOrderByFechaDesc();
    }

    @Test
    void moderarNoAceptaDejarlaPendiente() {
        // When / Then: ni siquiera se busca la reseña
        assertThatThrownBy(() -> servicio.moderar(15L, EstadoResena.PENDIENTE))
                .isInstanceOf(ApiException.class)
                .hasMessage("Elige aprobar u ocultar la reseña.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(resenas, never()).findById(any());
    }

    @Test
    void moderarResponde404SiLaResenaNoExiste() {
        // Given
        when(resenas.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.moderar(99L, EstadoResena.APROBADA))
                .isInstanceOf(ApiException.class)
                .hasMessage("La reseña no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void moderarCambiaElEstado() {
        // Given: la reseña de Paula espera moderación
        Resena pendiente = resena(15L, 7L, EstadoResena.PENDIENTE, "Llegó con un píxel muerto. Llámenme al 9 8765 4321.");
        when(resenas.findById(15L)).thenReturn(Optional.of(pendiente));

        // When
        ResenaAdminDto moderada = servicio.moderar(15L, EstadoResena.OCULTA);

        // Then: la entidad cambia (JPA la actualiza al terminar la transacción) y la respuesta lo refleja
        assertThat(pendiente.getEstado()).isEqualTo(EstadoResena.OCULTA);
        assertThat(moderada.estado()).isEqualTo(EstadoResena.OCULTA);
        assertThat(moderada.alerta()).isEqualTo("Contiene datos de contacto");
    }

    // ---------- Ayudantes ----------

    /** Una orden con el procesador (3) y los audífonos (6), como la respondería order-service. */
    private static OrdenInternaDto orden(Long id, Long usuarioId, String estado) {
        LocalDateTime entregadaEn = "ENTREGADA".equals(estado) ? LocalDateTime.now().minusDays(20) : null;
        return new OrdenInternaDto(id, String.format("GH-2026-%06d", id), usuarioId,
                DatosDeEjemplo.usuario(usuarioId).nombre(), estado, entregadaEn,
                List.of(new LineaOrdenInternaDto(3L, "Procesador Tectra R7 de 8 núcleos"),
                        new LineaOrdenInternaDto(6L, "Audífonos Andesia Pulse 7.1")));
    }

    private static CrearResenaSolicitud solicitud(Long ordenId, Long productoId) {
        return new CrearResenaSolicitud(ordenId, productoId, 5, "Muy buen producto, lo recomiendo.");
    }

    /** Reseña ya guardada (con id) de Camila en su orden 1. */
    private static Resena resena(Long id, Long productoId, EstadoResena estado, String comentario) {
        Resena r = new Resena(1L, 3L, "Camila R.", productoId, DatosDeEjemplo.producto(productoId).nombre(), 5,
                comentario, LocalDateTime.now().minusDays(id));
        r.moderar(estado);
        ReflectionTestUtils.setField(r, "id", id);
        return r;
    }
}
