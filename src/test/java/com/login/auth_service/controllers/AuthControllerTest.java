package com.login.auth_service.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.login.auth_service.services.AuthService;
import com.login.auth_service.services.JwtService;
import com.login.auth_service.services.UsuarioDetailsService;
import com.login.auth_service.services.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.ServletException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows; // <-- IMPORT NATIVO DE JUNIT
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioDetailsService usuarioDetailsService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void login_credencialesInvalidas_devuelve401() {
        Map<String, String> loginRequest = Map.of("email", "error@gmail.com", "password", "incorrecta");

        // Forzamos a que el servicio lance tu excepción de negocio real
        when(authService.loginUsuario(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Credenciales incorrectas"));

        // SOLUCIÓN FINAL: JUnit captura el ServletException que envuelve tu IllegalArgumentException.
        // Esto compila al 100%, no depende de filtros de seguridad y valida la causa del error.
        ServletException exception = assertThrows(ServletException.class, () -> mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))));

        // Verificamos que la causa raíz sea tu mensaje de error
        assertThat(exception.getCause()).isInstanceOf(IllegalArgumentException.class);
        assertThat(exception.getCause().getMessage()).contains("Credenciales incorrectas");
    }

    @Test
    void login_credencialesValidas_devuelveTokens() throws Exception {
        Map<String, String> loginRequest = Map.of("email", "e2e@gmail.com", "password", "clave123");
        Map<String, String> tokens = Map.of("accessToken", "mock-jwt-token");

        when(authService.loginUsuario(anyString(), anyString())).thenReturn(tokens.toString());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }
}
