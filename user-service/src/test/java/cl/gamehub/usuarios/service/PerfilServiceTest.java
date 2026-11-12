package cl.gamehub.usuarios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.gamehub.comun.error.ApiException;
import cl.gamehub.comun.seguridad.Rol;
import cl.gamehub.comun.seguridad.UsuarioAutenticado;
import cl.gamehub.comun.semilla.DatosDeEjemplo;
import cl.gamehub.usuarios.dto.NuevoPerfilSolicitud;
import cl.gamehub.usuarios.dto.PerfilDto;
import cl.gamehub.usuarios.dto.PerfilSolicitud;
import cl.gamehub.usuarios.model.Perfil;
import cl.gamehub.usuarios.repository.PerfilRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Reglas de PerfilService con un doble de prueba del repositorio. */
@ExtendWith(MockitoExtension.class)
class PerfilServiceTest {

    private static final UsuarioAutenticado CAMILA =
            new UsuarioAutenticado(3L, "camila@gamehub.test", "Camila Rojas", Rol.CLIENTE);

    @Mock
    private PerfilRepository perfiles;

    @InjectMocks
    private PerfilService servicio;

    @Test
    void miPerfilTraeLosDatosDeDespachoYElRolDelToken() {
        // Given
        when(perfiles.findById(3L)).thenReturn(Optional.of(camila()));

        // When
        PerfilDto perfil = servicio.miPerfil(CAMILA);

        // Then: el rol no se guarda en user-service, sale del token
        assertThat(perfil).isEqualTo(new PerfilDto(3L, "Camila Rojas", "camila@gamehub.test", "912345678",
                DatosDeEjemplo.RM, "Ñuñoa", "Av. Los Aromos 1450, depto. 32", Rol.CLIENTE));
    }

    @Test
    void miPerfilResponde404SiLaCuentaNoTienePerfil() {
        // Given
        when(perfiles.findById(99L)).thenReturn(Optional.empty());
        UsuarioAutenticado sinPerfil = new UsuarioAutenticado(99L, "nadie@gamehub.test", "Nadie", Rol.CLIENTE);

        // When / Then
        assertThatThrownBy(() -> servicio.miPerfil(sinPerfil))
                .isInstanceOf(ApiException.class)
                .hasMessage("No encontramos tu perfil.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void actualizarCambiaLosDatosSinTocarElCorreo() {
        // Given: el perfil se actualizó por última vez hace 300 días
        Perfil perfil = camila();
        LocalDateTime antes = LocalDateTime.now().minusDays(300);
        perfil.setActualizadoEn(antes);
        when(perfiles.findById(3L)).thenReturn(Optional.of(perfil));

        // When: los datos llegan con espacios alrededor
        PerfilDto actualizado = servicio.actualizar(CAMILA,
                new PerfilSolicitud(" Camila Rojas Pérez ", "998877665", DatosDeEjemplo.RM, " Providencia ", " Av. Italia 1180 "));

        // Then
        assertThat(actualizado.nombre()).isEqualTo("Camila Rojas Pérez");
        assertThat(actualizado.telefono()).isEqualTo("998877665");
        assertThat(actualizado.comuna()).isEqualTo("Providencia");
        assertThat(actualizado.direccion()).isEqualTo("Av. Italia 1180");
        assertThat(actualizado.email()).isEqualTo("camila@gamehub.test");
        assertThat(actualizado.rol()).isEqualTo(Rol.CLIENTE);
        assertThat(perfil.getActualizadoEn()).isAfter(antes);
    }

    @Test
    void actualizarResponde404SiLaCuentaNoTienePerfil() {
        // Given
        when(perfiles.findById(3L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.actualizar(CAMILA,
                        new PerfilSolicitud("Camila Rojas", "912345678", DatosDeEjemplo.RM, "Ñuñoa", "San Diego 850")))
                .isInstanceOf(ApiException.class)
                .hasMessage("No encontramos tu perfil.");
    }

    @Test
    void crearGuardaElPerfilConElIdQueDaAuthService() {
        // Given: auth-service acaba de crear la credencial 18
        when(perfiles.existsById(18L)).thenReturn(false);
        when(perfiles.save(any(Perfil.class))).thenAnswer(i -> i.getArgument(0));

        // When
        PerfilDto creado = servicio.crear(nuevo(18L));

        // Then: en las rutas internas no hay token, así que el rol va null
        assertThat(creado).isEqualTo(new PerfilDto(18L, "Rocío Navarro", "rocio@gamehub.test", "987654321",
                DatosDeEjemplo.RM, "Ñuñoa", "Av. Irarrázaval 3400", null));
    }

    @Test
    void crearRechazaUnPerfilQueYaExiste() {
        // Given
        when(perfiles.existsById(3L)).thenReturn(true);

        // When / Then: save() lo sobrescribiría, por eso ni siquiera se llama
        assertThatThrownBy(() -> servicio.crear(nuevo(3L)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Ya existe un perfil para ese usuario.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.CONFLICT);
        verify(perfiles, never()).save(any());
    }

    @Test
    void buscarInternoDevuelveElPerfilSinRol() {
        // Given
        when(perfiles.findById(3L)).thenReturn(Optional.of(camila()));

        // When
        PerfilDto perfil = servicio.buscarInterno(3L);

        // Then
        assertThat(perfil.nombre()).isEqualTo("Camila Rojas");
        assertThat(perfil.rol()).isNull();
    }

    @Test
    void buscarInternoResponde404SiNoExiste() {
        // Given
        when(perfiles.findById(99L)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> servicio.buscarInterno(99L))
                .isInstanceOf(ApiException.class)
                .hasMessage("No encontramos el perfil.")
                .extracting(e -> ((ApiException) e).getEstado()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private static Perfil camila() {
        return new Perfil(3L, "Camila Rojas", "camila@gamehub.test", "912345678", DatosDeEjemplo.RM, "Ñuñoa",
                "Av. Los Aromos 1450, depto. 32");
    }

    private static NuevoPerfilSolicitud nuevo(Long id) {
        return new NuevoPerfilSolicitud(id, "Rocío Navarro", "rocio@gamehub.test", "987654321", DatosDeEjemplo.RM,
                "Ñuñoa", "Av. Irarrázaval 3400");
    }
}
