package com.login.auth_service.services;

import com.login.auth_service.dtos.RegisterRequest;
import com.login.auth_service.dtos.UsuarioResponse;
import com.login.auth_service.modelo.Perfil;
import com.login.auth_service.modelo.Rol;
import com.login.auth_service.modelo.Usuario;
import com.login.auth_service.repository.RolRepository;
import com.login.auth_service.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository=usuarioRepository;
        this.rolRepository=rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UsuarioResponse getByEmail(String email){
        Usuario usuarioBuscado = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        Perfil perfilUsuarioBuscado= usuarioBuscado.getPerfil();
        if (perfilUsuarioBuscado !=null) {
            return new UsuarioResponse(usuarioBuscado.getEmail(), perfilUsuarioBuscado.getNombre(), perfilUsuarioBuscado.getApellidos(), perfilUsuarioBuscado.getPais(), perfilUsuarioBuscado.getTelefono(), perfilUsuarioBuscado.getDireccion(), perfilUsuarioBuscado.getCiudad());
        } else {
            return new UsuarioResponse(usuarioBuscado.getEmail(), "", "", "", "", "", "");
        }
    }

    public Usuario registrarUsuario(RegisterRequest usuarioRegistrado) {
        if (usuarioRepository.findByEmail(usuarioRegistrado.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Rol rolUser = rolRepository.findByNombre("USER")
                .orElseThrow(() -> new IllegalStateException("Rol USER no encontrado"));

        Usuario usuario = new Usuario();
        usuario.setEmail(usuarioRegistrado.getEmail());
        usuario.setPassword(passwordEncoder.encode(usuarioRegistrado.getPassword()));
        usuario.setRoles(List.of(rolUser));

        Perfil perfil = new Perfil();

        perfil.setTelefono(usuarioRegistrado.getTelefono());
        perfil.setPais(usuarioRegistrado.getPais());
        perfil.setNombre(usuarioRegistrado.getNombre());
        perfil.setDireccion(usuarioRegistrado.getDireccion());
        perfil.setCiudad(usuarioRegistrado.getCiudad());
        perfil.setApellidos(usuarioRegistrado.getApellidos());
        perfil.setUsuario(usuario);
        usuario.setPerfil(perfil);

        return usuarioRepository.save(usuario);
    }
}