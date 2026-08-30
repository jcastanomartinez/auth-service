package com.login.auth_service.repository;

import com.login.auth_service.modelo.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository  extends JpaRepository<Rol, Long> {
    Optional<Rol> findByNombre(String nombre);

}
