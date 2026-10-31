package com.equipe.projetoadsapi.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * CONFIGURAÇÃO DO SPRING SECURITY 6 (JWT & Stateless Filter Chain)
 *
 * Localização no repositório de origem:
 * com.equipe.projetoadsapi.security.SecurityConfig
 *
 * Roteiro de Defesa para a Banca Examinadora:
 * "Esta classe centraliza a arquitetura de segurança stateless da API.
 * Configura a proteção CSRF desabilitada para consumo por clientes REST/Mobile,
 * gerencia o ciclo de vida dos tokens JWT através do SecurityFilter interceptador,
 * e expõe os beans de autenticação e criptografia BCrypt."
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Cadeia principal de filtros de segurança HTTP.
     * Intercepta todas as requisições antes de chegarem aos Controllers.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Endpoints Públicos de Autenticação (Login, Cadastro e Ativação de E-mail)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/cadastrar").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/verificar-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/reenviar-codigo").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/api/auth/**").permitAll()

                        // 2. Documentação e Especificação OpenAPI / Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // 3. Qualquer outra rota exige token JWT Bearer válido
                        .anyRequest().authenticated()
                )
                // Insere o filtro JWT antes do filtro padrão de autenticação do Spring
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Exporta o AuthenticationManager do Spring Security para uso nos serviços de autenticação.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
