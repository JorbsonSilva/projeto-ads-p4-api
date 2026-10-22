package com.equipe.projetoadsapi.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provedor de criptografia adaptativa de senhas via BCrypt.
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t2
 * Cost factor 12 para resistência contra força bruta e Rainbow Tables.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
