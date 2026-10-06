package com.login.auth_service.services;

import com.login.auth_service.modelo.Rol;
import com.login.auth_service.modelo.Usuario;
import com.login.auth_service.security.RsaKeyProvider;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * No usa Mockito ni contexto de Spring: RsaKeyProvider es una clase normal que
 * carga las claves reales del proyecto (src/main/resources/keys), así que basta
 * con instanciarla directamente para tener un JwtService real y funcional.
 */
class JwtServiceTest {

    private RsaKeyProvider rsaKeyProvider;
    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        rsaKeyProvider = new RsaKeyProvider();
        jwtService = new JwtService(rsaKeyProvider);
    }

    private Usuario usuarioDeEjemplo() {
        Usuario usuario = new Usuario();
        usuario.setEmail("ana@gmail.com");
        Rol rol = new Rol();
        rol.setNombre("USER");
        usuario.setRoles(List.of(rol));
        return usuario;
    }

    @Test
    void generarToken_incluyeElEmailComoSubject() {
        String token = jwtService.generarToken(usuarioDeEjemplo());

        assertThat(jwtService.extraerEmail(token)).isEqualTo("ana@gmail.com");
    }

    @Test
    void validarToken_true_cuandoElUsernameCoincideYNoHaExpirado() {
        String token = jwtService.generarToken(usuarioDeEjemplo());
        UserDetails userDetails = User.builder()
                .username("ana@gmail.com")
                .password("da igual")
                .roles("USER")
                .build();

        assertThat(jwtService.validarToken(token, userDetails)).isTrue();
    }

    @Test
    void validarToken_false_cuandoElUsernameNoCoincide() {
        String token = jwtService.generarToken(usuarioDeEjemplo());
        UserDetails otroUsuario = User.builder()
                .username("otro@gmail.com")
                .password("da igual")
                .roles("USER")
                .build();

        assertThat(jwtService.validarToken(token, otroUsuario)).isFalse();
    }

    @Test
    @org.junit.jupiter.api.DisplayName(
            "un token caducado lanza ExpiredJwtException en vez de devolver false " +
            "(JwtAuthFilter no la captura: en producción esto probablemente da un 500, " +
            "no un 401/403 limpio — revisar si se quiere ese comportamiento)")
    void validarToken_tokenExpirado_lanzaExpiredJwtException() {
        String tokenExpirado = Jwts.builder()
                .subject("ana@gmail.com")
                .claim("roles", List.of("USER"))
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000L))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000L))
                .signWith(rsaKeyProvider.getPrivateKey())
                .compact();

        UserDetails userDetails = User.builder()
                .username("ana@gmail.com")
                .password("da igual")
                .roles("USER")
                .build();

        assertThatThrownBy(() -> jwtService.validarToken(tokenExpirado, userDetails))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }
}
