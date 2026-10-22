package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.dto.ErrorResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Ponto de entrada para requisições não autenticadas (HTTP 401).
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t1
 * Retorna RFC 7807 padronizado sem vazar stacktrace.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResponseDTO error = ErrorResponseDTO.builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("Não Autorizado")
                .message("Credenciais ausentes, inválidas ou sessão expirada. Por favor, efetue login.")
                .path(request.getRequestURI())
                .build();

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
