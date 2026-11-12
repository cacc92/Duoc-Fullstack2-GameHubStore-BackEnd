package cl.gamehub.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.ordenes.client.CuponDto;
import cl.gamehub.ordenes.client.DisponibilidadDto;
import cl.gamehub.ordenes.client.InventarioClient;
import cl.gamehub.ordenes.client.ProductoClient;
import cl.gamehub.ordenes.client.ProductoInternoDto;
import cl.gamehub.ordenes.client.PromocionClient;
import cl.gamehub.ordenes.dto.LineaSolicitud;
import cl.gamehub.ordenes.dto.ValidacionCarritoDto;
import cl.gamehub.ordenes.dto.ValidarCarritoSolicitud;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Validación del carrito con tres servicios simulados: product-service, inventory-service y promotion-service. */
@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    private static final ProductoInternoDto TECLADO = new ProductoInternoDto(4L, "KB-NIM-TKL", "Teclado mecánico Nimbo TKL", "teclado", 59990, null, true);
    private static final ProductoInternoDto AUDIFONOS = new ProductoInternoDto(6L, "HS-AND-P71", "Audífonos Andesia Pulse 7.1", "audifonos", 49990, 54990, true);
    private static final CuponDto GAMER10 = new CuponDto("GAMER10", 10, 30000, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

    @Mock
    private ProductoClient productos;

    @Mock
    private InventarioClient inventario;

    @Mock
    private PromocionClient promociones;

    @InjectMocks
    private CarritoService servicio;

    @Test
    void agruparSumaLasLineasDelMismoProducto() {
        List<LineaSolicitud> agrupadas = CarritoService.agrupar(List.of(
                new LineaSolicitud(4L, 1), new LineaSolicitud(6L, 1), new LineaSolicitud(4L, 2)));

        assertThat(agrupadas).containsExactly(new LineaSolicitud(4L, 3), new LineaSolicitud(6L, 1));
    }

    @Test
    void unaLineaQueSuperaElStockDejaElCarritoInvalido() {
        // Given: quedan 3 audífonos y el teclado no tiene stock
        when(productos.buscar(anyList())).thenReturn(List.of(TECLADO, AUDIFONOS));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 0), new DisponibilidadDto(6L, 3)));

        // When
        ValidacionCarritoDto resultado = servicio.validar(new ValidarCarritoSolicitud(
                List.of(new LineaSolicitud(4L, 1), new LineaSolicitud(6L, 4)), null));

        // Then
        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.lineas()).extracting(l -> l.mensaje())
                .containsExactly("Este producto se quedó sin stock.", "Solo quedan 3 unidades.");
        assertThat(resultado.subtotal()).isEqualTo(59990 + 4 * 49990);
    }

    @Test
    void unProductoInactivoNoEstaDisponible() {
        when(productos.buscar(anyList())).thenReturn(List.of(new ProductoInternoDto(13L, "CTL-AUS-PAD25", "Control 2025", "control", 39990, null, false)));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(13L, 0)));

        ValidacionCarritoDto resultado = servicio.validar(new ValidarCarritoSolicitud(List.of(new LineaSolicitud(13L, 1)), null));

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.lineas().getFirst().mensaje()).isEqualTo("Este producto ya no está disponible.");
    }

    @Test
    void unCuponVigenteCalculaElDescuento() {
        when(productos.buscar(anyList())).thenReturn(List.of(TECLADO));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 24)));
        when(promociones.vigente("GAMER10")).thenReturn(GAMER10);

        ValidacionCarritoDto resultado = servicio.validar(new ValidarCarritoSolicitud(List.of(new LineaSolicitud(4L, 1)), " GAMER10 "));

        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.descuento()).isEqualTo(5999);
        assertThat(resultado.total()).isEqualTo(53991);
    }

    @Test
    void unCuponVencidoDejaSuMotivoSinInvalidarElCarrito() {
        when(productos.buscar(anyList())).thenReturn(List.of(TECLADO));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 24)));
        when(promociones.vigente("CYBER25")).thenThrow(ApiException.regla("El cupón CYBER25 venció el 01-11-2026."));

        ValidacionCarritoDto resultado = servicio.validar(new ValidarCarritoSolicitud(List.of(new LineaSolicitud(4L, 1)), "CYBER25"));

        assertThat(resultado.valido()).isTrue();
        assertThat(resultado.mensajeCupon()).contains("venció");
        assertThat(resultado.descuento()).isZero();
    }

    @Test
    void siPromotionServiceNoRespondeElErrorSube() {
        when(productos.buscar(anyList())).thenReturn(List.of(TECLADO));
        when(inventario.stock(anyList())).thenReturn(List.of(new DisponibilidadDto(4L, 24)));
        when(promociones.vigente("GAMER10")).thenThrow(new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No responde"));

        assertThatThrownBy(() -> servicio.validar(new ValidarCarritoSolicitud(List.of(new LineaSolicitud(4L, 1)), "GAMER10")))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void unCarritoVacioNoEsValido() {
        ValidacionCarritoDto resultado = servicio.validar(new ValidarCarritoSolicitud(List.of(), null));

        assertThat(resultado.valido()).isFalse();
        assertThat(resultado.lineas()).isEmpty();
    }

    @Test
    void elDescuentoNuncaSuperaElTopeNiElSubtotal() {
        assertThat(GAMER10.descuento(1_149_990)).isEqualTo(30000);
        assertThat(new CuponDto("TODO", 90, 1_000_000, null, null).descuento(1000)).isEqualTo(900);
        assertThat(GAMER10.descuento(0)).isZero();
    }
}
