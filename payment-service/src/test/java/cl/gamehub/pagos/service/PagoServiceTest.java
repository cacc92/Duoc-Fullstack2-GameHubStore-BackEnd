package cl.gamehub.pagos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.pagos.dto.NuevoPagoSolicitud;
import cl.gamehub.pagos.dto.PagoDto;
import cl.gamehub.pagos.model.EstadoPago;
import cl.gamehub.pagos.model.MetodoPago;
import cl.gamehub.pagos.model.Pago;
import cl.gamehub.pagos.repository.PagoRepository;
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

/** Reglas de PagoService con un doble del repositorio: sin base de datos ni Spring. */
@ExtendWith(MockitoExtension.class)
class PagoServiceTest {

    @Mock
    private PagoRepository pagos;

    @InjectMocks
    private PagoService servicio;

    // ---------- Pago simulado ----------

    @Test
    void debitoQuedaAprobado() {
        // Given: la orden 19 todavía no tiene pago
        cuandoSeGuarda();

        // When
        PagoDto pago = servicio.registrar(solicitud(MetodoPago.DEBITO, 59_990));

        // Then
        assertThat(pago.estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pago.detalle()).isEqualTo("Pago aprobado (simulado).");
        assertThat(pago.ordenId()).isEqualTo(19L);
        assertThat(pago.ordenNumero()).isEqualTo("GH-2026-000019");
        assertThat(pago.metodo()).isEqualTo(MetodoPago.DEBITO);
        assertThat(pago.monto()).isEqualTo(59_990);
        assertThat(pago.fecha()).isNotNull();
    }

    @Test
    void creditoHastaElCupoQuedaAprobado() {
        cuandoSeGuarda();

        // Justo en el cupo todavía se aprueba
        PagoDto pago = servicio.registrar(solicitud(MetodoPago.CREDITO, PagoService.CUPO_CREDITO));

        assertThat(pago.estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(pago.detalle()).isEqualTo("Pago aprobado (simulado).");
    }

    @Test
    void creditoSobreElCupoSeRechaza() {
        cuandoSeGuarda();

        PagoDto pago = servicio.registrar(solicitud(MetodoPago.CREDITO, 1_149_990));

        assertThat(pago.estado()).isEqualTo(EstadoPago.RECHAZADO);
        assertThat(pago.detalle()).isEqualTo("Rechazado: el total supera el cupo simulado de $1.000.000.");
    }

    @Test
    void transferenciaQuedaPendienteDelOperador() {
        cuandoSeGuarda();

        PagoDto pago = servicio.registrar(solicitud(MetodoPago.TRANSFERENCIA, 59_990));

        assertThat(pago.estado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pago.detalle()).isEqualTo("Esperando que el operador confirme la transferencia.");
    }

    @Test
    void elPagoGuardaAlClienteYLaFechaDeCreacion() {
        // Given
        cuandoSeGuarda();
        ArgumentCaptor<Pago> guardado = ArgumentCaptor.forClass(Pago.class);

        // When
        servicio.registrar(new NuevoPagoSolicitud(19L, " GH-2026-000019 ", 3L, MetodoPago.DEBITO, 59_990));

        // Then: el cliente queda para «Mis pagos» y la fecha del último cambio parte igual a la de creación
        verify(pagos).save(guardado.capture());
        Pago pago = guardado.getValue();
        assertThat(pago.getUsuarioId()).isEqualTo(3L);
        assertThat(pago.getOrdenNumero()).isEqualTo("GH-2026-000019");
        assertThat(pago.getCreadoEn()).isNotNull().isEqualTo(pago.getFecha());
    }

    @Test
    void unaOrdenNoPuedeTenerDosPagos() {
        when(pagos.existsByOrdenId(1L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(new NuevoPagoSolicitud(1L, "GH-2026-000001", 3L, MetodoPago.DEBITO, 1_000)))
                .isInstanceOf(ApiException.class)
                .hasMessage("La orden ya tiene un pago registrado.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(pagos, never()).save(any());
    }

    // ---------- Confirmar, anular y revertir ----------

    @Test
    void confirmarApruebaUnaTransferenciaPendiente() {
        // Given: la orden 6 se pagó con transferencia hace 3 horas
        Pago pendiente = pago(6L, MetodoPago.TRANSFERENCIA, EstadoPago.PENDIENTE);
        when(pagos.findByOrdenId(6L)).thenReturn(Optional.of(pendiente));

        // When
        PagoDto confirmado = servicio.confirmar(6L);

        // Then: la fecha pasa a ser la de la confirmación
        assertThat(confirmado.estado()).isEqualTo(EstadoPago.APROBADO);
        assertThat(confirmado.detalle()).isEqualTo("Transferencia confirmada por el operador.");
        assertThat(confirmado.fecha()).isAfter(pendiente.getCreadoEn());
    }

    @Test
    void soloSeConfirmanPagosPendientes() {
        when(pagos.findByOrdenId(1L)).thenReturn(Optional.of(pago(1L, MetodoPago.TRANSFERENCIA, EstadoPago.APROBADO)));

        assertThatThrownBy(() -> servicio.confirmar(1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo se pueden confirmar pagos pendientes.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void anularDejaAnuladoUnPagoPendiente() {
        when(pagos.findByOrdenId(7L)).thenReturn(Optional.of(pago(7L, MetodoPago.TRANSFERENCIA, EstadoPago.PENDIENTE)));

        PagoDto anulado = servicio.anular(7L);

        assertThat(anulado.estado()).isEqualTo(EstadoPago.ANULADO);
        assertThat(anulado.detalle()).isEqualTo("Anulado por el operador.");
    }

    @Test
    void soloSeAnulanPagosPendientes() {
        when(pagos.findByOrdenId(3L)).thenReturn(Optional.of(pago(3L, MetodoPago.CREDITO, EstadoPago.RECHAZADO)));

        assertThatThrownBy(() -> servicio.anular(3L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Solo se pueden anular pagos pendientes.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void revertirAnulaAunqueElPagoEsteAprobado() {
        // Given: order-service aprobó el pago, pero después no hubo stock para completar la compra
        when(pagos.findByOrdenId(5L)).thenReturn(Optional.of(pago(5L, MetodoPago.DEBITO, EstadoPago.APROBADO)));

        // When
        PagoDto revertido = servicio.revertir(5L);

        // Then
        assertThat(revertido.estado()).isEqualTo(EstadoPago.ANULADO);
        assertThat(revertido.detalle()).isEqualTo("Anulado: no había stock para completar la compra.");
    }

    @Test
    void unaOrdenSinPagoResponde404() {
        when(pagos.findByOrdenId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.confirmar(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("El pago de la orden no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThatThrownBy(() -> servicio.anular(99L)).hasMessage("El pago de la orden no existe.");
        assertThatThrownBy(() -> servicio.revertir(99L)).hasMessage("El pago de la orden no existe.");
    }

    // ---------- Consultas ----------

    @Test
    void buscarPorOrdenesSinIdsNoConsultaLaBase() {
        assertThat(servicio.buscarPorOrdenes(null)).isEmpty();
        assertThat(servicio.buscarPorOrdenes(List.of())).isEmpty();
        verifyNoInteractions(pagos);
    }

    @Test
    void buscarPorOrdenesOmiteLasOrdenesSinPago() {
        when(pagos.findByOrdenIdInOrderByOrdenIdAsc(List.of(1L, 2L, 99L))).thenReturn(List.of(
                pago(1L, MetodoPago.TRANSFERENCIA, EstadoPago.APROBADO), pago(2L, MetodoPago.CREDITO, EstadoPago.APROBADO)));

        List<PagoDto> lista = servicio.buscarPorOrdenes(List.of(1L, 2L, 99L));

        assertThat(lista).extracting(PagoDto::ordenId).containsExactly(1L, 2L);
        assertThat(lista.getFirst().id()).isEqualTo(1L);
    }

    @Test
    void miosDevuelveLosPagosDelUsuario() {
        when(pagos.findByUsuarioIdOrderByFechaDesc(3L)).thenReturn(List.of(pago(5L, MetodoPago.DEBITO, EstadoPago.APROBADO)));

        assertThat(servicio.mios(3L)).extracting(PagoDto::ordenId).containsExactly(5L);
    }

    @Test
    void listarFiltraPorEstadoSoloSiSeIndica() {
        Pago pendiente = pago(6L, MetodoPago.TRANSFERENCIA, EstadoPago.PENDIENTE);
        when(pagos.findAllByOrderByFechaDesc()).thenReturn(List.of(pendiente, pago(5L, MetodoPago.DEBITO, EstadoPago.APROBADO)));
        when(pagos.findByEstadoOrderByFechaDesc(EstadoPago.PENDIENTE)).thenReturn(List.of(pendiente));

        assertThat(servicio.listar(null)).hasSize(2);
        assertThat(servicio.listar(EstadoPago.PENDIENTE)).extracting(PagoDto::estado).containsExactly(EstadoPago.PENDIENTE);
    }

    private void cuandoSeGuarda() {
        when(pagos.existsByOrdenId(19L)).thenReturn(false);
        when(pagos.save(any(Pago.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static NuevoPagoSolicitud solicitud(MetodoPago metodo, int monto) {
        return new NuevoPagoSolicitud(19L, "GH-2026-000019", 3L, metodo, monto);
    }

    /** Un pago creado hace 3 horas; su id es el mismo de la orden, como en los datos de ejemplo. */
    private static Pago pago(long ordenId, MetodoPago metodo, EstadoPago estado) {
        Pago p = new Pago(ordenId, "GH-2026-%06d".formatted(ordenId), 3L, metodo, 59_990, estado, "Detalle de prueba",
                LocalDateTime.now().minusHours(3));
        ReflectionTestUtils.setField(p, "id", ordenId);
        return p;
    }
}
