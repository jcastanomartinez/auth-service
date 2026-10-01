package com.login.auth_service.services;

import com.login.auth_service.modelo.Usuario;
import com.login.auth_service.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, authenticationManager, jwtService);
    }

    @Test
    void loginCorrecto_autenticaYDevuelveToken() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@gmail.com");
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generarToken(usuario)).thenReturn("token-firmado");

        String token = authService.loginUsuario("ana@gmail.com", "clave123");

        assertThat(token).isEqualTo("token-firmado");

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("ana@gmail.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("clave123");
    }

    @Test
    void credencialesIncorrectas_propagaBadCredentialsException() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

        assertThatThrownBy(() -> authService.loginUsuario("ana@gmail.com", "malaclave"))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }

    @Test
    void usuarioAutenticadoPeroNoEncontradoEnBD_lanzaIllegalStateException() {
        // caso raro pero cubierto: pasa la autenticación (existe en el UserDetailsService)
        // pero luego no se encuentra al recargarlo desde el repositorio
        when(usuarioRepository.findByEmail("fantasma@gmail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginUsuario("fantasma@gmail.com", "clave123"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Usuario no encontrado");
    }
}
