package com.login.auth_service.services;

import com.login.auth_service.dtos.RegisterRequest;
import com.login.auth_service.dtos.UsuarioResponse;
import com.login.auth_service.modelo.Perfil;
import com.login.auth_service.modelo.Rol;
import com.login.auth_service.modelo.Usuario;
import com.login.auth_service.repository.RolRepository;
import com.login.auth_service.repository.UsuarioRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test unitario puro: sin contexto de Spring. UsuarioRepository y RolRepository
 * están mockeados con Mockito; solo se prueba la lógica de UsuarioService.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RolRepository rolRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, rolRepository, passwordEncoder);
    }



    @Nested
    @DisplayName("registrarUsuario")
    class RegistrarUsuario {

        @Test
        @DisplayName("crea el usuario con el rol USER, el perfil enlazado y la contraseña codificada")
        void creaUsuarioCorrectamente() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("nuevo@gmail.com");
            request.setPassword("plano123");
            request.setNombre("Ana");
            request.setApellidos("García");
            request.setTelefono("600111222");
            request.setPais("España");
            request.setCiudad("Madrid");
            request.setDireccion("Calle Falsa 1");

            when(usuarioRepository.findByEmail("nuevo@gmail.com")).thenReturn(Optional.empty());
            Rol rolUser = new Rol();
            rolUser.setId(1L);
            rolUser.setNombre("USER");
            when(rolRepository.findByNombre("USER")).thenReturn(Optional.of(rolUser));
            when(passwordEncoder.encode("plano123")).thenReturn("HASH(plano123)");
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            Usuario resultado = usuarioService.registrarUsuario(request);

            assertThat(resultado.getEmail()).isEqualTo("nuevo@gmail.com");
            assertThat(resultado.getPassword()).isEqualTo("HASH(plano123)");
            assertThat(resultado.getRoles()).containsExactly(rolUser);
            assertThat(resultado.getPerfil()).isNotNull();
            assertThat(resultado.getPerfil().getNombre()).isEqualTo("Ana");
            assertThat(resultado.getPerfil().getUsuario()).isSameAs(resultado);
        }

        @Test
        @DisplayName("rechaza el registro si el email ya existe")
        void rechazaEmailDuplicado() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("existente@gmail.com");
            when(usuarioRepository.findByEmail("existente@gmail.com"))
                    .thenReturn(Optional.of(new Usuario()));

            assertThatThrownBy(() -> usuarioService.registrarUsuario(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El email ya está registrado");

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("lanza IllegalStateException si el rol USER no existe en base de datos")
        void lanzaExcepcionSiFaltaElRolUser() {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("nuevo@gmail.com");
            when(usuarioRepository.findByEmail("nuevo@gmail.com")).thenReturn(Optional.empty());
            when(rolRepository.findByNombre("USER")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.registrarUsuario(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Rol USER no encontrado");
        }
    }

    @Nested
    @DisplayName("getByEmail")
    class GetByEmail {

        @Test
        @DisplayName("devuelve los datos del perfil cuando existe")
        void devuelvePerfilCompleto() {
            Usuario usuario = new Usuario();
            usuario.setEmail("ana@gmail.com");
            Perfil perfil = new Perfil();
            perfil.setNombre("Ana");
            perfil.setApellidos("García");
            perfil.setPais("España");
            perfil.setTelefono("600111222");
            perfil.setDireccion("Calle Falsa 1");
            perfil.setCiudad("Madrid");
            usuario.setPerfil(perfil);

            when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));

            UsuarioResponse respuesta = usuarioService.getByEmail("ana@gmail.com");

            assertThat(respuesta.email()).isEqualTo("ana@gmail.com");
            assertThat(respuesta.nombre()).isEqualTo("Ana");
            assertThat(respuesta.ciudad()).isEqualTo("Madrid");
        }

        @Test
        @DisplayName("devuelve campos vacíos si el usuario no tiene perfil")
        void devuelveCamposVaciosSinPerfil() {
            Usuario usuario = new Usuario();
            usuario.setEmail("sinperfil@gmail.com");
            usuario.setPerfil(null);
            when(usuarioRepository.findByEmail("sinperfil@gmail.com")).thenReturn(Optional.of(usuario));

            UsuarioResponse respuesta = usuarioService.getByEmail("sinperfil@gmail.com");

            assertThat(respuesta.nombre()).isEmpty();
            assertThat(respuesta.email()).isEqualTo("sinperfil@gmail.com");
        }

        @Test
        @DisplayName("lanza IllegalArgumentException si el usuario no existe")
        void lanzaExcepcionSiNoExiste() {
            when(usuarioRepository.findByEmail("fantasma@gmail.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usuarioService.getByEmail("fantasma@gmail.com"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
