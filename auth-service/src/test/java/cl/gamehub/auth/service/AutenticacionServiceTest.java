package cl.gamehub.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import cl.gamehub.auth.client.NuevoPerfilSolicitud;
import cl.gamehub.auth.client.UserClient;
import cl.gamehub.auth.dto.LoginSolicitud;
import cl.gamehub.auth.dto.RegistroSolicitud;
import cl.gamehub.auth.dto.SesionRespuesta;
import cl.gamehub.auth.dto.UsuarioAdminDto;
import cl.gamehub.auth.dto.UsuarioDto;
import cl.gamehub.auth.model.Credencial;
import cl.gamehub.auth.repository.CredencialRepository;
import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.seguridad.JwtServicio;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Reglas de AutenticacionService con dobles de prueba: el repositorio, BCrypt, el JWT y user-service son
 * mocks, así cada prueba controla exactamente qué responde cada uno.
 */
@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    private static final String DESACTIVADA = "Tu cuenta está desactivada. Escríbenos desde Ayuda para reactivarla.";

    @Mock
    private CredencialRepository credenciales;

    @Mock
    private PasswordEncoder codificador;

    @Mock
    private JwtServicio jwtServicio;

    @Mock
    private UserClient perfiles;

    @InjectMocks
    private AutenticacionService servicio;

    // ---------- Login ----------

    @Test
    void loginEntregaElTokenYLosDatosDeLaCuenta() {
        // Given: la cuenta de Camila existe, está activa y la contraseña calza con su hash
        when(credenciales.findByEmailIgnoreCase("camila@gamehub.test"))
                .thenReturn(Optional.of(credencial(3L, "Camila Rojas", "camila@gamehub.test", Rol.CLIENTE, true)));
        when(codificador.matches("clave123", "hash")).thenReturn(true);
        when(jwtServicio.generar(3L, "camila@gamehub.test", "Camila Rojas", Rol.CLIENTE)).thenReturn("token-camila");

        // When: el correo llega con espacios alrededor
        SesionRespuesta sesion = servicio.iniciarSesion(new LoginSolicitud(" camila@gamehub.test ", "clave123"));

        // Then
        assertThat(sesion.token()).isEqualTo("token-camila");
        assertThat(sesion.usuario()).isEqualTo(new UsuarioDto(3L, "Camila Rojas", "camila@gamehub.test", Rol.CLIENTE));
    }

    @Test
    void unCorreoSinCuentaResponde401() {
        // Given
        when(credenciales.findByEmailIgnoreCase("nadie@gamehub.test")).thenReturn(Optional.empty());

        // When / Then: el mensaje no dice si falló el correo o la contraseña
        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginSolicitud("nadie@gamehub.test", "clave123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Correo o contraseña incorrectos.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtServicio);
    }

    @Test
    void unaClaveIncorrectaResponde401() {
        // Given: la cuenta existe pero BCrypt dice que la contraseña no calza
        when(credenciales.findByEmailIgnoreCase("camila@gamehub.test"))
                .thenReturn(Optional.of(credencial(3L, "Camila Rojas", "camila@gamehub.test", Rol.CLIENTE, true)));
        when(codificador.matches("otra-clave1", "hash")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginSolicitud("camila@gamehub.test", "otra-clave1")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Correo o contraseña incorrectos.");
        verifyNoInteractions(jwtServicio);
    }

    @Test
    void unaCuentaDesactivadaResponde403AunqueLaClaveCalce() {
        // Given: Pedro está desactivado
        when(credenciales.findByEmailIgnoreCase("pedro@gamehub.test"))
                .thenReturn(Optional.of(credencial(4L, "Pedro Díaz", "pedro@gamehub.test", Rol.CLIENTE, false)));
        when(codificador.matches("clave123", "hash")).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> servicio.iniciarSesion(new LoginSolicitud("pedro@gamehub.test", "clave123")))
                .isInstanceOf(ApiException.class)
                .hasMessage(DESACTIVADA)
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(jwtServicio);
    }

    // ---------- Registro ----------

    @Test
    void registrarGuardaUnClienteYLePideElPerfilAUserService() {
        // Given: el correo está libre y la base le asigna el id 18 a la credencial
        when(credenciales.existsByEmailIgnoreCase("rocio@gamehub.test")).thenReturn(false);
        when(codificador.encode("clave1234")).thenReturn("hash-nuevo");
        when(credenciales.save(any(Credencial.class))).thenAnswer(i -> {
            Credencial guardada = i.getArgument(0);
            ReflectionTestUtils.setField(guardada, "id", 18L);
            return guardada;
        });
        when(jwtServicio.generar(18L, "rocio@gamehub.test", "Rocío Navarro", Rol.CLIENTE)).thenReturn("token-nuevo");

        // When: el correo viene con mayúsculas y el nombre con espacios
        SesionRespuesta sesion = servicio.registrar(registro(" Rocio@GameHub.test "));

        // Then: la cuenta queda como CLIENTE, con el correo en minúsculas y solo el hash de la contraseña
        ArgumentCaptor<Credencial> captor = ArgumentCaptor.forClass(Credencial.class);
        verify(credenciales).save(captor.capture());
        Credencial guardada = captor.getValue();
        assertThat(guardada.getRol()).isEqualTo(Rol.CLIENTE);
        assertThat(guardada.getEmail()).isEqualTo("rocio@gamehub.test");
        assertThat(guardada.getClaveHash()).isEqualTo("hash-nuevo");
        assertThat(guardada.isActivo()).isTrue();
        // y user-service recibe el perfil con el mismo id que la credencial
        verify(perfiles).crearPerfil(new NuevoPerfilSolicitud(18L, "Rocío Navarro", "rocio@gamehub.test",
                "987654321", DatosDeEjemplo.RM, "Ñuñoa", "Av. Irarrázaval 3400"));
        assertThat(sesion.token()).isEqualTo("token-nuevo");
        assertThat(sesion.usuario()).isEqualTo(new UsuarioDto(18L, "Rocío Navarro", "rocio@gamehub.test", Rol.CLIENTE));
    }

    @Test
    void registrarRechazaUnCorreoQueYaTieneCuenta() {
        // Given
        when(credenciales.existsByEmailIgnoreCase("camila@gamehub.test")).thenReturn(true);

        // When / Then: no se guarda nada ni se llama a user-service
        assertThatThrownBy(() -> servicio.registrar(registro("Camila@gamehub.test")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe una cuenta con ese correo. Inicia sesión o usa otro.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(credenciales, never()).save(any());
        verifyNoInteractions(perfiles);
    }

    @Test
    void siUserServiceFallaElErrorSaleDelRegistro() {
        // Given: user-service responde con error (Feign lo convierte en ApiException)
        when(credenciales.existsByEmailIgnoreCase("rocio@gamehub.test")).thenReturn(false);
        when(codificador.encode("clave1234")).thenReturn("hash-nuevo");
        when(credenciales.save(any(Credencial.class))).thenAnswer(i -> i.getArgument(0));
        ApiException caido = ApiException.noDisponible("Uno de los servicios de la tienda no responde.");
        doThrow(caido).when(perfiles).crearPerfil(any());

        // When / Then: la excepción sale del método, así @Transactional revierte la credencial
        // (el rollback real se prueba en AutenticacionControllerTest)
        assertThatThrownBy(() -> servicio.registrar(registro("rocio@gamehub.test"))).isSameAs(caido);
        verifyNoInteractions(jwtServicio);
    }

    // ---------- Renovar ----------

    @Test
    void renovarEntregaUnTokenNuevoConLosDatosGuardados() {
        // Given
        when(credenciales.findById(3L))
                .thenReturn(Optional.of(credencial(3L, "Camila Rojas", "camila@gamehub.test", Rol.CLIENTE, true)));
        when(jwtServicio.generar(3L, "camila@gamehub.test", "Camila Rojas", Rol.CLIENTE)).thenReturn("token-renovado");

        // When
        SesionRespuesta sesion = servicio.renovar(3L);

        // Then
        assertThat(sesion.token()).isEqualTo("token-renovado");
        assertThat(sesion.usuario().id()).isEqualTo(3L);
    }

    @Test
    void renovarRechazaUnaCuentaDesactivada() {
        // Given: Pedro tenía un token, pero el administrador lo desactivó
        when(credenciales.findById(4L))
                .thenReturn(Optional.of(credencial(4L, "Pedro Díaz", "pedro@gamehub.test", Rol.CLIENTE, false)));

        // When / Then
        assertThatThrownBy(() -> servicio.renovar(4L))
                .isInstanceOf(ApiException.class)
                .hasMessage(DESACTIVADA)
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void renovarResponde404SiElUsuarioNoExiste() {
        // Given
        when(credenciales.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.renovar(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("El usuario no existe.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---------- Administración ----------

    @Test
    void listarDevuelveLasCuentasEnElOrdenDelRepositorio() {
        // Given
        when(credenciales.findAllByOrderByRolAscNombreAsc()).thenReturn(List.of(
                credencial(1L, "Ana Torres", "ana@gamehub.test", Rol.ADMIN, true),
                credencial(4L, "Pedro Díaz", "pedro@gamehub.test", Rol.CLIENTE, false)));

        // When
        List<UsuarioAdminDto> lista = servicio.listar();

        // Then
        assertThat(lista).extracting(UsuarioAdminDto::nombre).containsExactly("Ana Torres", "Pedro Díaz");
        assertThat(lista).extracting(UsuarioAdminDto::activo).containsExactly(true, false);
        assertThat(lista.getFirst().creadoEn()).isNotNull();
    }

    @Test
    void elAdministradorDesactivaOtraCuenta() {
        // Given
        Credencial camila = credencial(3L, "Camila Rojas", "camila@gamehub.test", Rol.CLIENTE, true);
        when(credenciales.findById(3L)).thenReturn(Optional.of(camila));

        // When: Ana (id 1) desactiva a Camila
        UsuarioAdminDto resultado = servicio.cambiarActivo(3L, false, 1L);

        // Then
        assertThat(resultado.activo()).isFalse();
        assertThat(camila.isActivo()).isFalse();
    }

    @Test
    void elAdministradorNoPuedeDesactivarSuPropiaCuenta() {
        // When / Then: ni siquiera se busca la cuenta
        assertThatThrownBy(() -> servicio.cambiarActivo(1L, false, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("No puedes desactivar tu propia cuenta.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(credenciales, never()).findById(any());
    }

    @Test
    void elAdministradorSiPuedeReactivarSuPropiaCuenta() {
        // Given
        when(credenciales.findById(1L))
                .thenReturn(Optional.of(credencial(1L, "Ana Torres", "ana@gamehub.test", Rol.ADMIN, true)));

        // When
        UsuarioAdminDto resultado = servicio.cambiarActivo(1L, true, 1L);

        // Then
        assertThat(resultado.activo()).isTrue();
    }

    @Test
    void cambiarActivoResponde404SiElUsuarioNoExiste() {
        // Given
        when(credenciales.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.cambiarActivo(99L, true, 1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("El usuario no existe.");
    }

    private static Credencial credencial(Long id, String nombre, String email, Rol rol, boolean activo) {
        Credencial c = new Credencial(nombre, email, "hash", rol);
        c.setActivo(activo);
        ReflectionTestUtils.setField(c, "id", id);
        return c;
    }

    private static RegistroSolicitud registro(String email) {
        return new RegistroSolicitud(" Rocío Navarro ", email, "clave1234", "987654321", DatosDeEjemplo.RM, "Ñuñoa",
                "Av. Irarrázaval 3400");
    }
}
