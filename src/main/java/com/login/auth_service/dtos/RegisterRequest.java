package com.login.auth_service.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterRequest {
        private String email;
        private String password;
        private String nombre;
        private String apellidos;
        private String telefono;
        private String pais;
        private String ciudad;
        private String direccion;

}
