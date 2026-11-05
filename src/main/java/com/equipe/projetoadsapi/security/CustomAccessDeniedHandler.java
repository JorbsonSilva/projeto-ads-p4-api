package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * TRATAMENTO DE ACESSO NEGADO (HTTP 403 FORBIDDEN).
 * Rastreabilidade: Fase 3 (Auth & RBAC) - f3-t1, f3-t7, Requisito RN-001
 * 
 * Intercepta violações de segurança de método (@PreAuthorize) e tentativas de
 * acesso a recursos não autorizados, retornando envelope ApiResponse padronizado.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        
        String requestUri = request.getRequestURI();
        String mensagem = (accessDeniedException.getMessage() != null && !accessDeniedException.getMessage().isBlank())
                ? accessDeniedException.getMessage()
                : "Acesso Proibido: Você não possui permissão para acessar ou alterar este recurso.";

        log.warn("[SEGURANÇA - 403 FORBIDDEN] Acesso negado na rota {}: {}", requestUri, mensagem);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiResponse<Object> errorEnvelope = ApiResponse.forbidden(mensagem);
        response.getWriter().write(objectMapper.writeValueAsString(errorEnvelope));
    }
}
