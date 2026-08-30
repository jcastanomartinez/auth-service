package com.login.auth_service.repository;

import com.login.auth_service.modelo.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
            Optional<Usuario> findByEmail(String email);


}
