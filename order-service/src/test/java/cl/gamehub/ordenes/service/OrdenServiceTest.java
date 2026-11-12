package cl.gamehub.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.ordenes.client.CuponDto;
import cl.gamehub.ordenes.client.DescuentoStockSolicitud;
import cl.gamehub.ordenes.client.DespachoClient;
import cl.gamehub.ordenes.client.DespachoDto;
import cl.gamehub.ordenes.client.DisponibilidadDto;
import cl.gamehub.ordenes.client.InventarioClient;
import cl.gamehub.ordenes.client.NuevoPagoSolicitud;
import cl.gamehub.ordenes.client.PagoClient;
import cl.gamehub.ordenes.client.PagoDto;
import cl.gamehub.ordenes.client.ProductoClient;
import cl.gamehub.ordenes.client.ProductoInternoDto;
import cl.gamehub.ordenes.client.PromocionClient;
import cl.gamehub.ordenes.dto.CrearOrdenSolicitud;
import cl.gamehub.ordenes.dto.DatosDespachoDto;
import cl.gamehub.ordenes.dto.LineaSolicitud;
import cl.gamehub.ordenes.dto.OrdenDetalleDto;
import cl.gamehub.ordenes.dto.OrdenInternaDto;
import cl.gamehub.ordenes.model.DatosDespacho;
import cl.gamehub.ordenes.model.EstadoOrden;
import cl.gamehub.ordenes.model.MetodoPago;
import cl.gamehub.ordenes.model.Orden;
import cl.gamehub.ordenes.repository.OrdenRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

/** El checkout coordina cinco servicios; aquí todos son dobles de prueba y solo se prueba la coordinación. */
@ExtendWith(MockitoExtension.class)
class OrdenServiceTest {

    private static final UsuarioAutenticado CAMILA = new UsuarioAutenticado(3L, "camila@gamehub.test", "Camila Rojas", Rol.CLIENTE);
    private static final ProductoInternoDto TECLADO = new ProductoInternoDto(4L, "KB-NIM-TKL", "Teclado mecánico Nimbo TKL", "teclado", 59990, null, true);
    private static final DatosDespachoDto DESPACHO = new DatosDespachoDto("Camila Rojas", "camila@gamehub.test", "912345678",
            "Región Metropolitana de Santiago", "Ñuñoa", "Av. Los Aromos 1450", "", null);

    @Mock
    private OrdenRepository ordenes;

    @Mock
    private ProductoClient productos;

    @Mock
    private InventarioClient inventario;

    @Mock
    private PromocionClient promociones;

    @Mock
    private PagoClient pagos;

    @Mock
    private DespachoClient despachos;

    @InjectMocks
    private OrdenService servicio;

    private void catalogoConStock(int stock) {
        when(productos.buscar(anyList())).thenReturn(List.of(TECLADO));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, stock)));
    }

    private void guardarConId(long id) {
        when(ordenes.save(any(Orden.class))).thenAnswer(i -> {
            Orden o = i.getArgument(0);
            ReflectionTestUtils.setField(o, "id", id);
            return o;
        });
    }

    private static CrearOrdenSolicitud solicitud(MetodoPago metodo, String cupon) {
        return new CrearOrdenSolicitud(List.of(new LineaSolicitud(4L, 1)), cupon, metodo, DESPACHO);
    }

    @Test
    void unaCompraConDebitoYCuponQuedaPagadaYDescuentaElStock() {
        // Given
        catalogoConStock(24);
        when(promociones.vigente("GAMER10")).thenReturn(new CuponDto("GAMER10", 10, 30000, LocalDate.now(), LocalDate.now()));
        guardarConId(19);
        when(pagos.crear(any())).thenReturn(new PagoDto(19L, "DEBITO", "APROBADO", LocalDateTime.now(), "Pago aprobado (simulado)."));

        // When
        OrdenDetalleDto orden = servicio.crear(CAMILA, solicitud(MetodoPago.DEBITO, "GAMER10"));

        // Then: el descuento es el 10 %, el pago se cobra por el total y el stock se descuenta con el número de orden
        assertThat(orden.estado()).isEqualTo(EstadoOrden.PAGADA);
        assertThat(orden.descuento()).isEqualTo(5999);
        assertThat(orden.numero()).endsWith("-000019");
        ArgumentCaptor<NuevoPagoSolicitud> pago = ArgumentCaptor.forClass(NuevoPagoSolicitud.class);
        verify(pagos).crear(pago.capture());
        assertThat(pago.getValue().monto()).isEqualTo(53991);
        ArgumentCaptor<DescuentoStockSolicitud> descuento = ArgumentCaptor.forClass(DescuentoStockSolicitud.class);
        verify(inventario).descontar(descuento.capture());
        assertThat(descuento.getValue().referencia()).startsWith("Venta GH-");
        verify(promociones).registrarUso("GAMER10");
    }

    @Test
    void unPagoRechazadoNoDescuentaStock() {
        catalogoConStock(24);
        guardarConId(20);
        when(pagos.crear(any())).thenReturn(new PagoDto(20L, "CREDITO", "RECHAZADO", LocalDateTime.now(), "Rechazado"));

        OrdenDetalleDto orden = servicio.crear(CAMILA, solicitud(MetodoPago.CREDITO, null));

        assertThat(orden.estado()).isEqualTo(EstadoOrden.RECHAZADA);
        verify(inventario, never()).descontar(any());
    }

    @Test
    void unaTransferenciaQuedaPendienteDePago() {
        catalogoConStock(24);
        guardarConId(21);
        when(pagos.crear(any())).thenReturn(new PagoDto(21L, "TRANSFERENCIA", "PENDIENTE", LocalDateTime.now(), "Esperando"));

        assertThat(servicio.crear(CAMILA, solicitud(MetodoPago.TRANSFERENCIA, null)).estado()).isEqualTo(EstadoOrden.PENDIENTE_PAGO);
        verify(inventario, never()).descontar(any());
    }

    @Test
    void sinStockSuficienteNoSeCobra() {
        catalogoConStock(0);

        assertThatThrownBy(() -> servicio.crear(CAMILA, solicitud(MetodoPago.DEBITO, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("No hay stock suficiente de Teclado mecánico Nimbo TKL: quedan 0 unidades.");
        verify(pagos, never()).crear(any());
    }

    @Test
    void unProductoInactivoDetieneLaCompra() {
        when(productos.buscar(anyList())).thenReturn(List.of(new ProductoInternoDto(4L, "KB", "Teclado", "teclado", 1, null, false)));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 5)));

        assertThatThrownBy(() -> servicio.crear(CAMILA, solicitud(MetodoPago.DEBITO, null)))
                .hasMessage("Uno de los productos ya no está disponible. Revisa tu carrito.");
    }

    @Test
    void siElStockSeAcaboDespuesDeAprobarSeRevierteElPago() {
        // Given: otra compra se llevó el último teclado entre la revisión y el descuento
        catalogoConStock(1);
        guardarConId(22);
        when(pagos.crear(any())).thenReturn(new PagoDto(22L, "DEBITO", "APROBADO", LocalDateTime.now(), "Pago aprobado (simulado)."));
        doThrow(ApiException.conflicto("No hay stock suficiente de Teclado mecánico Nimbo TKL.")).when(inventario).descontar(any());

        // When / Then: la compra falla con 409 y el pago se compensa en payment-service
        assertThatThrownBy(() -> servicio.crear(CAMILA, solicitud(MetodoPago.DEBITO, null)))
                .hasMessage("No hay stock suficiente de Teclado mecánico Nimbo TKL.");
        verify(pagos).revertir(22L);
    }

    @Test
    void siNoSePuedeContarElUsoDelCuponLaCompraSigue() {
        catalogoConStock(24);
        when(promociones.vigente("GAMER10")).thenReturn(new CuponDto("GAMER10", 10, 30000, LocalDate.now(), LocalDate.now()));
        when(promociones.registrarUso("GAMER10")).thenThrow(ApiException.regla("El cupón GAMER10 ya alcanzó su tope de usos."));
        guardarConId(23);
        when(pagos.crear(any())).thenReturn(new PagoDto(23L, "DEBITO", "APROBADO", LocalDateTime.now(), "ok"));

        assertThat(servicio.crear(CAMILA, solicitud(MetodoPago.DEBITO, "GAMER10")).estado()).isEqualTo(EstadoOrden.PAGADA);
    }

    @Test
    void unClienteNoVeLaOrdenDeOtro() {
        when(ordenes.findById(9L)).thenReturn(Optional.of(orden(9L, 15L, EstadoOrden.RECHAZADA)));

        assertThatThrownBy(() -> servicio.detalle(9L, CAMILA)).hasMessage("Esta orden no te pertenece.");
    }

    @Test
    void elDetalleJuntaElPagoYElDespacho() {
        when(ordenes.findById(2L)).thenReturn(Optional.of(orden(2L, 3L, EstadoOrden.EN_TRANSITO)));
        when(pagos.buscar(List.of(2L))).thenReturn(List.of(new PagoDto(2L, "CREDITO", "APROBADO", LocalDateTime.now(), "ok")));
        when(despachos.buscar(List.of(2L))).thenReturn(List.of(new DespachoDto(2L, "GHX-70214", "EN_TRANSITO", LocalDate.now(), null, List.of())));

        OrdenDetalleDto detalle = servicio.detalle(2L, CAMILA);

        assertThat(detalle.pago().estado()).isEqualTo("APROBADO");
        assertThat(detalle.despacho().seguimiento()).isEqualTo("GHX-70214");
        assertThat(detalle.lineas()).hasSize(1);
    }

    @Test
    void sinDespachoNoSeConsultaShippingService() {
        when(ordenes.findById(6L)).thenReturn(Optional.of(orden(6L, 12L, EstadoOrden.PENDIENTE_PAGO)));
        when(pagos.buscar(List.of(6L))).thenReturn(List.of(new PagoDto(6L, "TRANSFERENCIA", "PENDIENTE", LocalDateTime.now(), "Esperando")));

        OrdenDetalleDto detalle = servicio.detalle(6L, new UsuarioAutenticado(2L, "luis@gamehub.test", "Luis Soto", Rol.OPERADOR));

        assertThat(detalle.despacho()).isNull();
        verify(despachos, never()).buscar(anyList());
    }

    @Test
    void laRutaInternaEntregaDuenoEstadoYProductos() {
        Orden entregada = orden(1L, 3L, EstadoOrden.ENTREGADA);
        when(ordenes.findById(1L)).thenReturn(Optional.of(entregada));

        OrdenInternaDto interna = servicio.interno(1L);

        assertThat(interna.usuarioId()).isEqualTo(3L);
        assertThat(interna.lineas()).extracting(OrdenInternaDto.LineaInternaDto::productoId).containsExactly(4L);
    }

    @Test
    void unaOrdenInexistenteResponde404() {
        when(ordenes.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.buscar(99L)).hasMessage("La orden no existe.");
    }

    @Test
    void misOrdenesSonLasDelCliente() {
        when(ordenes.findByUsuarioIdOrderByCreadaEnDesc(3L)).thenReturn(List.of(orden(1L, 3L, EstadoOrden.ENTREGADA)));

        assertThat(servicio.mias(3L)).singleElement().satisfies(o -> assertThat(o.unidades()).isEqualTo(1));
    }

    static Orden orden(long id, long usuarioId, EstadoOrden estado) {
        Orden o = new Orden(usuarioId, "Cliente", new DatosDespacho("Cliente", "c@gamehub.test", "912345678", "RM", "Ñuñoa",
                "Calle 123", null, null), MetodoPago.DEBITO, LocalDateTime.now().minusDays(1));
        o.agregarLinea(4L, "KB-NIM-TKL", "Teclado mecánico Nimbo TKL", "teclado", 59990, 1);
        o.fijarTotales(59990, 0, null);
        ReflectionTestUtils.setField(o, "id", id);
        o.asignarNumero();
        o.restaurar(estado, estado == EstadoOrden.ENTREGADA ? LocalDateTime.now().minusDays(1) : null);
        return o;
    }
}
