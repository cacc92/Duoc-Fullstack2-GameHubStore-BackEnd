package cl.gamehub.ordenes.service;

import static cl.gamehub.ordenes.service.OrdenServiceTest.orden;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.ordenes.client.DespachoClient;
import cl.gamehub.ordenes.client.DespachoDto;
import cl.gamehub.ordenes.client.DisponibilidadDto;
import cl.gamehub.ordenes.client.InventarioClient;
import cl.gamehub.ordenes.client.PagoClient;
import cl.gamehub.ordenes.client.PagoDto;
import cl.gamehub.ordenes.dto.ActualizarDespachoSolicitud;
import cl.gamehub.ordenes.dto.GenerarDespachoSolicitud;
import cl.gamehub.ordenes.dto.OrdenOperadorDto;
import cl.gamehub.ordenes.model.EstadoDespacho;
import cl.gamehub.ordenes.model.EstadoOrden;
import cl.gamehub.ordenes.model.Orden;
import cl.gamehub.ordenes.repository.OrdenRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OperacionServiceTest {

    @Mock
    private OrdenRepository ordenes;

    @Mock
    private OrdenService ordenService;

    @Mock
    private InventarioClient inventario;

    @Mock
    private PagoClient pagos;

    @Mock
    private DespachoClient despachos;

    @InjectMocks
    private OperacionService servicio;

    @Test
    void noSeDespachaUnaOrdenNoPagada() {
        when(ordenService.buscar(6L)).thenReturn(orden(6L, 12L, EstadoOrden.PENDIENTE_PAGO));

        assertThatThrownBy(() -> servicio.generarDespacho(6L, new GenerarDespachoSolicitud(null, null)))
                .hasMessage("No se despacha una orden no pagada.");
        verify(despachos, never()).crear(any());
    }

    @Test
    void unaOrdenConDespachoNoRecibeOtro() {
        when(ordenService.buscar(8L)).thenReturn(orden(8L, 14L, EstadoOrden.EN_PREPARACION));

        assertThatThrownBy(() -> servicio.generarDespacho(8L, new GenerarDespachoSolicitud(null, null)))
                .hasMessage("Esta orden ya tiene un despacho.");
    }

    @Test
    void generarElDespachoDejaLaOrdenEnPreparacion() {
        Orden pagada = orden(5L, 3L, EstadoOrden.PAGADA);
        when(ordenService.buscar(5L)).thenReturn(pagada);
        when(despachos.crear(any())).thenReturn(new DespachoDto(5L, "GHX-70005", "PREPARANDO", LocalDate.now().plusDays(3), null, List.of()));

        OrdenOperadorDto fila = servicio.generarDespacho(5L, new GenerarDespachoSolicitud(null, null));

        assertThat(fila.estado()).isEqualTo(EstadoOrden.EN_PREPARACION);
        assertThat(fila.seguimiento()).isEqualTo("GHX-70005");
    }

    @Test
    void alEntregarLaOrdenGuardaLaFechaDeEntrega() {
        Orden enTransito = orden(2L, 3L, EstadoOrden.EN_TRANSITO);
        when(ordenService.buscar(2L)).thenReturn(enTransito);
        LocalDateTime ahora = LocalDateTime.now();
        when(despachos.actualizar(eq(2L), any())).thenReturn(new DespachoDto(2L, "GHX-70214", "ENTREGADO", LocalDate.now(), ahora, List.of()));

        OrdenOperadorDto fila = servicio.actualizarDespacho(2L, new ActualizarDespachoSolicitud(EstadoDespacho.ENTREGADO, null, null));

        assertThat(fila.estado()).isEqualTo(EstadoOrden.ENTREGADA);
        assertThat(enTransito.getEntregadaEn()).isEqualTo(ahora);
    }

    @Test
    void sinDespachoNoSePuedeAvanzar() {
        when(ordenService.buscar(5L)).thenReturn(orden(5L, 3L, EstadoOrden.PAGADA));

        assertThatThrownBy(() -> servicio.actualizarDespacho(5L, new ActualizarDespachoSolicitud(EstadoDespacho.DESPACHADO, null, null)))
                .hasMessage("Primero genera el despacho de esta orden.");
    }

    @Test
    void confirmarUnaTransferenciaDescuentaElStock() {
        Orden pendiente = orden(6L, 12L, EstadoOrden.PENDIENTE_PAGO);
        when(ordenService.buscar(6L)).thenReturn(pendiente);
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 24)));
        when(pagos.confirmar(6L)).thenReturn(new PagoDto(6L, "TRANSFERENCIA", "APROBADO", LocalDateTime.now(), "Transferencia confirmada por el operador."));

        OrdenOperadorDto fila = servicio.confirmarPago(6L);

        assertThat(fila.estado()).isEqualTo(EstadoOrden.PAGADA);
        assertThat(fila.estadoPago()).isEqualTo("APROBADO");
        verify(ordenService).confirmarEfectos(pendiente);
    }

    @Test
    void sinStockNoSeConfirmaElPago() {
        when(ordenService.buscar(6L)).thenReturn(orden(6L, 12L, EstadoOrden.PENDIENTE_PAGO));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 0)));

        assertThatThrownBy(() -> servicio.confirmarPago(6L))
                .hasMessage("No hay stock suficiente de Teclado mecánico Nimbo TKL para confirmar el pago.");
        verify(pagos, never()).confirmar(any());
    }

    @Test
    void siElDescuentoFallaLaOrdenQuedaAnulada() {
        Orden pendiente = orden(7L, 13L, EstadoOrden.PENDIENTE_PAGO);
        when(ordenService.buscar(7L)).thenReturn(pendiente);
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 1)));
        when(pagos.confirmar(7L)).thenReturn(new PagoDto(7L, "TRANSFERENCIA", "APROBADO", LocalDateTime.now(), "ok"));
        doThrow(ApiException.conflicto("No hay stock suficiente de Teclado mecánico Nimbo TKL.")).when(ordenService).confirmarEfectos(pendiente);

        assertThatThrownBy(() -> servicio.confirmarPago(7L)).hasMessageEndingWith("El pago quedó anulado.");
        assertThat(pendiente.getEstado()).isEqualTo(EstadoOrden.ANULADA);
    }

    @Test
    void soloSeConfirmanOAnulanPagosPendientes() {
        when(ordenService.buscar(5L)).thenReturn(orden(5L, 3L, EstadoOrden.PAGADA));

        assertThatThrownBy(() -> servicio.confirmarPago(5L)).hasMessage("Solo se pueden confirmar pagos pendientes.");
        assertThatThrownBy(() -> servicio.anularPago(5L)).hasMessage("Solo se pueden anular pagos pendientes.");
    }

    @Test
    void anularUnPagoPendienteAnulaLaOrden() {
        when(ordenService.buscar(7L)).thenReturn(orden(7L, 13L, EstadoOrden.PENDIENTE_PAGO));
        when(pagos.anular(7L)).thenReturn(new PagoDto(7L, "TRANSFERENCIA", "ANULADO", LocalDateTime.now(), "Anulado por el operador."));

        assertThat(servicio.anularPago(7L).estado()).isEqualTo(EstadoOrden.ANULADA);
    }

    @Test
    void elResumenCuentaPorEstado() {
        when(ordenes.countByEstadoIn(List.of(EstadoOrden.PENDIENTE_PAGO))).thenReturn(2L);
        when(ordenes.countByEstadoIn(List.of(EstadoOrden.PAGADA))).thenReturn(1L);
        when(ordenes.countByEstadoIn(List.of(EstadoOrden.EN_PREPARACION, EstadoOrden.DESPACHADA, EstadoOrden.EN_TRANSITO))).thenReturn(2L);

        assertThat(servicio.resumen()).isEqualTo(new cl.gamehub.ordenes.dto.ResumenOperadorDto(2, 1, 2));
    }

    @Test
    void laBandejaJuntaElEstadoDelPagoYDelDespacho() {
        List<Orden> enCurso = List.of(orden(2L, 3L, EstadoOrden.EN_TRANSITO));
        when(ordenes.findByEstadoInOrderByCreadaEnDesc(List.of(EstadoOrden.EN_PREPARACION, EstadoOrden.DESPACHADA, EstadoOrden.EN_TRANSITO)))
                .thenReturn(enCurso);
        when(ordenService.pagosDe(enCurso)).thenReturn(Map.of(2L, new PagoDto(2L, "CREDITO", "APROBADO", LocalDateTime.now(), "ok")));
        when(ordenService.despachosDe(enCurso)).thenReturn(Map.of(2L, new DespachoDto(2L, "GHX-70214", "EN_TRANSITO", LocalDate.now(), null, List.of())));

        List<OrdenOperadorDto> filas = servicio.listar("en_curso");

        assertThat(filas).singleElement().satisfies(f -> {
            assertThat(f.estadoPago()).isEqualTo("APROBADO");
            assertThat(f.estadoDespacho()).isEqualTo("EN_TRANSITO");
        });
    }

    @Test
    void sinFiltroLaBandejaMuestraTodas() {
        when(ordenes.findAllByOrderByCreadaEnDesc()).thenReturn(List.of());

        assertThat(servicio.listar(null)).isEmpty();
        assertThat(servicio.listar("CERRADAS")).isEmpty();
    }
}
