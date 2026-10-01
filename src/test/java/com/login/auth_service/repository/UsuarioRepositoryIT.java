package com.login.auth_service.repository;

import com.login.auth_service.modelo.Perfil;
import com.login.auth_service.modelo.Rol;
import com.login.auth_service.modelo.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @DataJpaTest en Boot 3.2 carga automáticamente data.sql (defer-datasource-
 * initialization + sql.init.mode=always están en application.yml), así que los
 * roles ADMIN/USER ya existen al arrancar cada test.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UsuarioRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Test
    void dataSqlSiembraLosRolesAdminYUser() {
        assertThat(rolRepository.findByNombre("USER")).isPresent();
        assertThat(rolRepository.findByNombre("ADMIN")).isPresent();
    }

    @Test
    void guardaUsuarioConRolYPerfil_yLoRecuperaPorEmail() {
        Rol rolUser = rolRepository.findByNombre("USER").orElseThrow();

        Usuario usuario = new Usuario();
        usuario.setEmail("ana@gmail.com");
        usuario.setPassword("hash");
        usuario.setRoles(List.of(rolUser));

        Perfil perfil = new Perfil();
        perfil.setNombre("Ana");
        perfil.setApellidos("García");
        perfil.setUsuario(usuario);
        usuario.setPerfil(perfil);

        usuarioRepository.saveAndFlush(usuario);

        Usuario recuperado = usuarioRepository.findByEmail("ana@gmail.com").orElseThrow();
        assertThat(recuperado.getRoles()).extracting(Rol::getNombre).containsExactly("USER");
        assertThat(recuperado.getPerfil()).isNotNull();
        assertThat(recuperado.getPerfil().getNombre()).isEqualTo("Ana");
    }

    @Test
    void emailDuplicado_violaConstraintUnique() {
        Usuario primero = new Usuario();
        primero.setEmail("duplicado@gmail.com");
        primero.setPassword("hash1");
        usuarioRepository.saveAndFlush(primero);

        Usuario segundo = new Usuario();
        segundo.setEmail("duplicado@gmail.com");
        segundo.setPassword("hash2");

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(segundo))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByEmail_devuelveVacioSiNoExiste() {
        assertThat(usuarioRepository.findByEmail("no-existe@gmail.com")).isEmpty();
    }
}
