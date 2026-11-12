package cl.gamehub.inventario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.inventario.dto.DescuentoStockSolicitud;
import cl.gamehub.inventario.dto.DisponibilidadDto;
import cl.gamehub.inventario.dto.LineaDescuento;
import cl.gamehub.inventario.dto.MovimientoDto;
import cl.gamehub.inventario.dto.MovimientoSolicitud;
import cl.gamehub.inventario.dto.ProductoInventarioSolicitud;
import cl.gamehub.inventario.dto.StockDto;
import cl.gamehub.inventario.dto.StockNuevoSolicitud;
import cl.gamehub.inventario.model.Movimiento;
import cl.gamehub.inventario.model.Stock;
import cl.gamehub.inventario.model.TipoMovimiento;
import cl.gamehub.inventario.repository.MovimientoRepository;
import cl.gamehub.inventario.repository.StockRepository;
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

/** Reglas de InventarioService con dobles de prueba: los dos repositorios son mocks. */
@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 10, 17, 30);

    @Mock
    private StockRepository stocks;

    @Mock
    private MovimientoRepository movimientos;

    @InjectMocks
    private InventarioService servicio;

    // ---------- Catálogo ----------

    @Test
    void sinIdsLaDisponibilidadTraeTodosLosProductos() {
        // Given
        when(stocks.findAllByOrderByProductoIdAsc()).thenReturn(List.of(stock(1L, 7), stock(5L, 0)));

        // When
        List<DisponibilidadDto> lista = servicio.disponibilidad(null);

        // Then
        assertThat(lista).containsExactly(new DisponibilidadDto(1L, 7), new DisponibilidadDto(5L, 0));
    }

    @Test
    void laDisponibilidadDeVariosProductosConsultaSoloEsos() {
        // Given
        when(stocks.findByProductoIdInOrderByProductoIdAsc(List.of(5L, 1L))).thenReturn(List.of(stock(1L, 7), stock(5L, 0)));

        // When
        List<DisponibilidadDto> lista = servicio.disponibilidad(List.of(5L, 1L));

        // Then
        assertThat(lista).extracting(DisponibilidadDto::productoId).containsExactly(1L, 5L);
    }

    @Test
    void unaListaVaciaDeIdsNoConsultaLaBase() {
        assertThat(servicio.disponibilidad(List.of())).isEmpty();
        verifyNoInteractions(stocks);
    }

    // ---------- Panel del operador ----------

    @Test
    void elPanelMuestraElStockConLosDatosDelProducto() {
        // Given
        Stock descontinuado = new Stock(13L, "CTL-AUS-PAD25", "Control Austral Pad (edición 2025)", false, 0);
        when(stocks.findAllByOrderByProductoIdAsc()).thenReturn(List.of(descontinuado));

        // When / Then
        assertThat(servicio.stock())
                .containsExactly(new StockDto(13L, "CTL-AUS-PAD25", "Control Austral Pad (edición 2025)", 0, false));
    }

    @Test
    void losMovimientosLleganConElNombreDelProductoYElStockQueQuedo() {
        // Given
        Movimiento m = new Movimiento(stock(10L, 30), TipoMovimiento.INGRESO, 20, "Recepción de proveedor", "Luis Soto", FECHA);
        ReflectionTestUtils.setField(m, "id", 1L);
        when(movimientos.findTop30ByOrderByFechaDescIdDesc()).thenReturn(List.of(m));

        // When / Then
        assertThat(servicio.movimientos()).containsExactly(new MovimientoDto(1L, FECHA, 10L, "Producto 10",
                TipoMovimiento.INGRESO, 20, "Recepción de proveedor", "Luis Soto", 30));
    }

    @Test
    void registrarResponde404SiElProductoNoExiste() {
        // Given
        when(stocks.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.registrar(solicitud(99L, TipoMovimiento.INGRESO, 5), "Luis Soto"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El producto no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unIngresoOUnaSalidaDeCeroUnidadesSeRechaza() {
        // Given
        when(stocks.findById(4L)).thenReturn(Optional.of(stock(4L, 24)));

        // When / Then
        for (TipoMovimiento tipo : List.of(TipoMovimiento.INGRESO, TipoMovimiento.SALIDA)) {
            assertThatThrownBy(() -> servicio.registrar(solicitud(4L, tipo, 0), "Luis Soto"))
                    .isInstanceOf(ApiException.class)
                    .hasMessage("La cantidad debe ser mayor que cero.")
                    .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        verify(movimientos, never()).save(any());
    }

    @Test
    void unaSalidaNoPuedeDejarElStockNegativo() {
        // Given: quedan 3 audífonos
        Stock audifonos = stock(6L, 3);
        when(stocks.findById(6L)).thenReturn(Optional.of(audifonos));

        // When / Then
        assertThatThrownBy(() -> servicio.registrar(solicitud(6L, TipoMovimiento.SALIDA, 5), "Ana Torres"))
                .isInstanceOf(ApiException.class)
                .hasMessage("La salida deja el stock en -2. El stock no puede quedar negativo.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(audifonos.getCantidad()).isEqualTo(3);
        verify(movimientos, never()).save(any());
    }

    @Test
    void unAjusteIgualAlStockActualNoTieneNadaQueAjustar() {
        // Given
        when(stocks.findById(4L)).thenReturn(Optional.of(stock(4L, 24)));

        // When / Then
        assertThatThrownBy(() -> servicio.registrar(solicitud(4L, TipoMovimiento.AJUSTE, 24), "Luis Soto"))
                .isInstanceOf(ApiException.class)
                .hasMessage("El conteo coincide con el stock actual: no hay nada que ajustar.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unIngresoSumaYQuedaRegistradoConElUsuario() {
        // Given
        Stock teclado = stock(4L, 24);
        when(stocks.findById(4L)).thenReturn(Optional.of(teclado));
        when(movimientos.save(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        // When
        MovimientoDto dto = servicio.registrar(
                new MovimientoSolicitud(4L, TipoMovimiento.INGRESO, 6, " Recepción de proveedor "), "Luis Soto");

        // Then
        assertThat(teclado.getCantidad()).isEqualTo(30);
        assertThat(dto.tipo()).isEqualTo(TipoMovimiento.INGRESO);
        assertThat(dto.cantidad()).isEqualTo(6);
        assertThat(dto.stockResultante()).isEqualTo(30);
        assertThat(dto.motivo()).isEqualTo("Recepción de proveedor");
        assertThat(dto.usuario()).isEqualTo("Luis Soto");
        assertThat(dto.fecha()).isNotNull();
    }

    @Test
    void unaSalidaRestaYSeGuardaConCantidadNegativa() {
        // Given
        Stock teclado = stock(4L, 24);
        when(stocks.findById(4L)).thenReturn(Optional.of(teclado));
        when(movimientos.save(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        // When
        MovimientoDto dto = servicio.registrar(solicitud(4L, TipoMovimiento.SALIDA, 4), "Luis Soto");

        // Then
        assertThat(teclado.getCantidad()).isEqualTo(20);
        assertThat(dto.cantidad()).isEqualTo(-4);
        assertThat(dto.stockResultante()).isEqualTo(20);
    }

    @Test
    void unAjusteFijaElStockAlConteoYRegistraLaDiferencia() {
        // Given: el sistema dice 24, pero en bodega se contaron 21
        Stock teclado = stock(4L, 24);
        when(stocks.findById(4L)).thenReturn(Optional.of(teclado));
        when(movimientos.save(any(Movimiento.class))).thenAnswer(i -> i.getArgument(0));

        // When
        MovimientoDto dto = servicio.registrar(solicitud(4L, TipoMovimiento.AJUSTE, 21), "Luis Soto");

        // Then
        assertThat(teclado.getCantidad()).isEqualTo(21);
        assertThat(dto.cantidad()).isEqualTo(-3);
        assertThat(dto.stockResultante()).isEqualTo(21);
    }

    // ---------- Para otros microservicios ----------

    @Test
    void crearRechazaUnProductoQueYaTieneStock() {
        // Given
        when(stocks.existsById(14L)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> servicio.crear(new StockNuevoSolicitud(14L, "KB-NIM-MINI", "Teclado Nimbo Mini", true, 5)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe stock para el producto 14.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(stocks, never()).save(any());
    }

    @Test
    void crearConStockInicialRegistraUnIngresoDelSistema() {
        // Given
        when(stocks.existsById(14L)).thenReturn(false);
        when(stocks.save(any(Stock.class))).thenAnswer(i -> i.getArgument(0));

        // When
        StockDto dto = servicio.crear(new StockNuevoSolicitud(14L, " KB-NIM-MINI ", "Teclado Nimbo Mini", true, 5));

        // Then
        assertThat(dto).isEqualTo(new StockDto(14L, "KB-NIM-MINI", "Teclado Nimbo Mini", 5, true));
        ArgumentCaptor<Movimiento> guardado = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos).save(guardado.capture());
        Movimiento m = guardado.getValue();
        assertThat(m.getProductoId()).isEqualTo(14L);
        assertThat(m.getProducto()).isEqualTo("Teclado Nimbo Mini");
        assertThat(m.getTipo()).isEqualTo(TipoMovimiento.INGRESO);
        assertThat(m.getCantidad()).isEqualTo(5);
        assertThat(m.getMotivo()).isEqualTo("Stock inicial del producto");
        assertThat(m.getUsuario()).isEqualTo("Sistema");
        assertThat(m.getStockResultante()).isEqualTo(5);
    }

    @Test
    void crearSinStockInicialNoRegistraMovimientos() {
        // Given
        when(stocks.existsById(14L)).thenReturn(false);
        when(stocks.save(any(Stock.class))).thenAnswer(i -> i.getArgument(0));

        // When
        StockDto dto = servicio.crear(new StockNuevoSolicitud(14L, "KB-NIM-MINI", "Teclado Nimbo Mini", false, null));

        // Then
        assertThat(dto.stock()).isZero();
        assertThat(dto.activo()).isFalse();
        verifyNoInteractions(movimientos);
    }

    @Test
    void actualizarProductoCambiaLaCopiaDeSusDatosSinTocarElStock() {
        // Given
        when(stocks.findById(4L)).thenReturn(Optional.of(stock(4L, 24)));

        // When
        StockDto dto = servicio.actualizarProducto(4L, new ProductoInventarioSolicitud("KB-NIM-TKL2", " Teclado v2 ", false));

        // Then
        assertThat(dto).isEqualTo(new StockDto(4L, "KB-NIM-TKL2", "Teclado v2", 24, false));
    }

    @Test
    void actualizarProductoResponde404SiNoTieneStock() {
        // Given
        when(stocks.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.actualizarProducto(99L, new ProductoInventarioSolicitud("X-1", "X", true)))
                .isInstanceOf(ApiException.class)
                .hasMessage("El producto no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void descontarEsTodoONada() {
        // Given: el teclado alcanza, pero de la consola quedan 2 y se piden 3
        Stock teclado = stock(4L, 24);
        Stock consola = consola(2);
        when(stocks.findById(4L)).thenReturn(Optional.of(teclado));
        when(stocks.findById(8L)).thenReturn(Optional.of(consola));

        // When / Then: no se descuenta ninguna línea
        assertThatThrownBy(() -> servicio.descontar(descuento(linea(4L, 1), linea(8L, 3))))
                .isInstanceOf(ApiException.class)
                .hasMessage("No hay stock suficiente de Consola portátil Austral Go 512 GB.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(teclado.getCantidad()).isEqualTo(24);
        assertThat(consola.getCantidad()).isEqualTo(2);
        verifyNoInteractions(movimientos);
    }

    @Test
    void elMismoProductoEnDosLineasSeSumaAntesDeComparar() {
        // Given: quedan 2 consolas y llegan dos líneas de 1 y 2
        Stock consola = consola(2);
        when(stocks.findById(8L)).thenReturn(Optional.of(consola));

        // When / Then
        assertThatThrownBy(() -> servicio.descontar(descuento(linea(8L, 1), linea(8L, 2))))
                .hasMessage("No hay stock suficiente de Consola portátil Austral Go 512 GB.");
        assertThat(consola.getCantidad()).isEqualTo(2);
    }

    @Test
    void descontarRestaYRegistraUnaSalidaPorLinea() {
        // Given
        Stock teclado = stock(4L, 24);
        Stock consola = consola(2);
        when(stocks.findById(8L)).thenReturn(Optional.of(consola));
        when(stocks.findById(4L)).thenReturn(Optional.of(teclado));

        // When
        List<DisponibilidadDto> quedan = servicio.descontar(descuento(linea(8L, 2), linea(4L, 1)));

        // Then: devuelve lo que quedó y cada línea es una SALIDA con la referencia como motivo
        assertThat(quedan).containsExactly(new DisponibilidadDto(8L, 0), new DisponibilidadDto(4L, 23));
        ArgumentCaptor<Movimiento> guardados = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientos, times(2)).save(guardados.capture());
        assertThat(guardados.getAllValues())
                .extracting(Movimiento::getTipo, Movimiento::getCantidad, Movimiento::getMotivo, Movimiento::getUsuario,
                        Movimiento::getStockResultante)
                .containsExactly(
                        tuple(TipoMovimiento.SALIDA, -2, "Venta GH-2026-000019", "Sistema", 0),
                        tuple(TipoMovimiento.SALIDA, -1, "Venta GH-2026-000019", "Sistema", 23));
    }

    @Test
    void descontarUnProductoSinStockRegistradoResponde404() {
        // Given
        when(stocks.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.descontar(descuento(linea(99L, 1))))
                .isInstanceOf(ApiException.class)
                .hasMessage("El producto no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- Datos de prueba ----------

    private static Stock stock(Long productoId, int cantidad) {
        return new Stock(productoId, "SKU-" + productoId, "Producto " + productoId, true, cantidad);
    }

    private static Stock consola(int cantidad) {
        return new Stock(8L, "CON-AUS-GO", "Consola portátil Austral Go 512 GB", true, cantidad);
    }

    private static MovimientoSolicitud solicitud(Long productoId, TipoMovimiento tipo, int cantidad) {
        return new MovimientoSolicitud(productoId, tipo, cantidad, "Conteo de inventario");
    }

    private static LineaDescuento linea(Long productoId, int cantidad) {
        return new LineaDescuento(productoId, cantidad);
    }

    private static DescuentoStockSolicitud descuento(LineaDescuento... lineas) {
        return new DescuentoStockSolicitud(" Venta GH-2026-000019 ", List.of(lineas));
    }
}
