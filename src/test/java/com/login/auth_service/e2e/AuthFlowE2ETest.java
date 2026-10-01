package com.login.auth_service.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Testcontainers
class AuthFlowE2ETest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private RestTestClient restClient;

    @Test
    void flujoCompleto_registro_login_yAccesoAutenticado() {
        Map<String, Object> registro = Map.of(
                "email", "e2e@gmail.com",
                "password", "clave123",
                "nombre", "Test",
                "apellidos", "E2E",
                "telefono", "600000000",
                "pais", "España",
                "ciudad", "Madrid",
                "direccion", "Calle Falsa 1");

        // 1. Registro
        restClient.post().uri("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(body -> org.assertj.core.api.Assertions.assertThat(body.get("email")).isEqualTo("e2e@gmail.com"));

        // 2. Login y extracción de Token
        final String[] tokenWrapper = new String[1];
        restClient.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", "e2e@gmail.com", "password", "clave123"))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(body -> {
                    String t = (String) body.get("accessToken");
                    org.assertj.core.api.Assertions.assertThat(t).isNotBlank();
                    tokenWrapper[0] = t;
                });

        String token = tokenWrapper[0];

        // 3. Acceso a ruta autenticada /user/me
        restClient.get().uri("/user/me")
                .headers(headers -> headers.setBearerAuth(token))
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(body -> {
                    org.assertj.core.api.Assertions.assertThat(body.get("email")).isEqualTo("e2e@gmail.com");
                    org.assertj.core.api.Assertions.assertThat(body.get("nombre")).isEqualTo("Test");
                });
    }

    @Test
    void registroConEmailDuplicado_devuelve400() {
        Map<String, Object> registro = Map.of(
                "email", "duplicado-e2e@gmail.com", "password", "clave123", "nombre", "X");

        // Primer intento exitoso o controlado
        restClient.post().uri("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange();

        // Segundo intento duplicado
        restClient.post().uri("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void loginConPasswordIncorrecta_devuelve401() {
        Map<String, Object> registro = Map.of(
                "email", "malapass@gmail.com", "password", "claveBuena", "nombre", "X");

        restClient.post().uri("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(registro)
                .exchange();

        restClient.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", "malapass@gmail.com", "password", "claveMala"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void meSinToken_devuelve403() {
        restClient.get().uri("/user/me")
                .exchange()
                .expectStatus().isForbidden();
    }
}
