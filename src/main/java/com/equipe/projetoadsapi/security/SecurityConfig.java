package com.equipe.projetoadsapi.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuração Principal do Spring Security 6.
 * Rastreabilidade: Fase 3 (Auth & RBAC) & Fase 4 (Contrato v1 e Swagger UI)
 * Arquitetura Stateless com JWT, CORS unificado, fail-closed e proteção de endpoints.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationEntryPoint unauthorizedHandler;

    @Autowired
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(unauthorizedHandler)
                .accessDeniedHandler(accessDeniedHandler)
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Preflight CORS (OPTIONS) deve ser sempre liberado
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Documentação OpenAPI / Swagger (rotas v1 e raiz)
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/v1/api-docs/**",
                    "/api/v1/swagger-ui.html"
                ).permitAll()

                // Endpoints Públicos de Autenticação e Onboarding (v1, legado e tolerância a prefixos)
                .requestMatchers("/api/v1/auth/**", "/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/v1/usuarios/login", "/api/usuarios/login",
                    "/api/v1/usuarios/cadastro", "/api/usuarios/cadastro",
                    "/api/v1/usuarios/verificar-email", "/api/usuarios/verificar-email",
                    "/api/v1/usuarios/reenviar-verificacao", "/api/usuarios/reenviar-verificacao"
                ).permitAll()
                
                // Health Check e Diagnóstico
                .requestMatchers("/api/v1/health/**", "/api/health/**").permitAll()
                
                // Consulta pública de disciplinas/saberes
                .requestMatchers(HttpMethod.GET, "/api/v1/saberes/**", "/api/saberes/**").permitAll()
                
                // Endpoints Administrativos restritos a ADMIN
                .requestMatchers("/api/v1/admin/**", "/api/admin/**").hasRole("ADMIN")
                
                // Demais rotas exigem token JWT autenticado
                .anyRequest().authenticated()
            );

        http.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
