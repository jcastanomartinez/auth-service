package com.login.auth_service.controllers;

import com.login.auth_service.dtos.RegisterRequest;
import com.login.auth_service.modelo.Usuario;
import com.login.auth_service.services.UsuarioService;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/user")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUsuarioActual(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(usuarioService.getByEmail(email));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest usuarioRegistrado) {
        try {
            Usuario usuario = usuarioService.registrarUsuario(usuarioRegistrado);
            return ResponseEntity.ok(Map.of("email", usuario.getEmail()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
