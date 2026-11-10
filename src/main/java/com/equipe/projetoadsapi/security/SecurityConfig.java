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

/**
 * CONFIGURAÇÃO DO SPRING SECURITY 6 (JWT, Stateless Filter Chain & RBAC RN-001)
 *
 * Localização no repositório:
 * com.equipe.projetoadsapi.security.SecurityConfig
 *
 * Rastreabilidade: Fase 3 (Auth & RBAC) - Tarefa [BACK-003] / Requisito RN-001
 * Habilita segurança em nível de método com @EnableMethodSecurity(prePostEnabled = true),
 * delega o tratamento de 401 Unauthorized e 403 Forbidden aos handlers customizados,
 * e protege rotas docentes e financeiras contra invasão por perfil aluno.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

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
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. Endpoints Públicos de Autenticação JWT e Onboarding
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/cadastrar", "/api/v1/auth/cadastro", "/api/v1/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/verificar-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/reenviar-codigo").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/**", "/api/auth/**").permitAll()

                        // 2. Endpoints Públicos Legados de Usuários (Login, Cadastro e Ativação)
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/login", "/api/usuarios/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/cadastro", "/api/usuarios/cadastro", "/api/v1/usuarios/registro").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/verificar-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/usuarios/reenviar-verificacao").permitAll()

                        // 3. Health Check e Endpoints Públicos do Assistente Acadêmico IA
                        .requestMatchers("/api/v1/health", "/api/health", "/actuator/health").permitAll()
                        .requestMatchers("/api/v1/assistente/**", "/api/v1/assistant/**").permitAll()

                        // 4. Documentação e Especificação OpenAPI / Swagger
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/api/v1/api-docs/**", "/api/v1/swagger-ui/**", "/api/v1/swagger-ui.html", "/swagger-resources/**", "/webjars/**").permitAll()
                        
                        // 5. Isolamento de Personas e RBAC (Requisito RN-001):
                        // Rotas exclusivas de Docentes e Financeiras: bloqueio estrito para ALUNO
                        .requestMatchers("/api/v1/professores/*/financeiro/**", "/api/professores/*/financeiro/**").hasAnyRole("PROFESSOR", "ADMIN")
                        .requestMatchers("/api/v1/saques/**", "/api/saques/**").hasAnyRole("PROFESSOR", "ADMIN")
                        .requestMatchers(
                                "/api/v1/professores/*/alunos", "/api/professores/*/alunos", "/professores/*/alunos",
                                "/api/v1/professores/meus-alunos", "/api/professores/meus-alunos", "/professores/meus-alunos",
                                "/api/v1/usuarios/professores/*/alunos", "/api/usuarios/professores/*/alunos",
                                "/api/v1/usuarios/professores/meus-alunos", "/api/usuarios/professores/meus-alunos"
                        ).hasAnyRole("PROFESSOR", "ADMIN")

                        // 6. Qualquer outra rota exige token JWT Bearer válido
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
