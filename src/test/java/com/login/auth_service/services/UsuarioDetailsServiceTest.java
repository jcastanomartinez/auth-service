package com.login.auth_service.services;

import com.login.auth_service.modelo.Rol;
import com.login.auth_service.modelo.Usuario;
import com.login.auth_service.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Test
    void loadUserByUsername_devuelveUserDetailsConRolesPrefijadosROLE() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@gmail.com");
        usuario.setPassword("hash");
        Rol rolUser = new Rol();
        rolUser.setNombre("USER");
        usuario.setRoles(List.of(rolUser));
        when(usuarioRepository.findByEmail("ana@gmail.com")).thenReturn(Optional.of(usuario));

        UsuarioDetailsService service = new UsuarioDetailsService(usuarioRepository);
        UserDetails userDetails = service.loadUserByUsername("ana@gmail.com");

        assertThat(userDetails.getUsername()).isEqualTo("ana@gmail.com");
        assertThat(userDetails.getPassword()).isEqualTo("hash");
        assertThat(userDetails.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_usuarioInexistente_lanzaUsernameNotFoundException() {
        when(usuarioRepository.findByEmail("fantasma@gmail.com")).thenReturn(Optional.empty());
        UsuarioDetailsService service = new UsuarioDetailsService(usuarioRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("fantasma@gmail.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
