package com.equipe.projetoadsapi.security;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.exception.GlobalExceptionHandler;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TESTES DE SEGURANÇA RBAC E PREVENÇÃO DE ID SPOOFING (TAREFA [BACK-003]).
 * Rastreabilidade: Fase 3 (Auth & RBAC) - Tarefa [BACK-003] / Requisito RN-001
 * 
 * Valida:
 * 1. Bloqueio estrito de ID Spoofing via ResourceOwnershipService.
 * 2. Isolamento de Personas (Requisito RN-001): Aluno não pode acessar rotas docentes.
 * 3. Prevenção de Auto-Contratação de Mentorias.
 * 4. Tratamento de exceções retornando HTTP 403 Forbidden envelopado em ApiResponse<T>.
 */
class RBACIntegrationTest {

    private ResourceOwnershipService ownershipService;
    private CustomAccessDeniedHandler accessDeniedHandler;
    private GlobalExceptionHandler exceptionHandler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ownershipService = new ResourceOwnershipService();
        accessDeniedHandler = new CustomAccessDeniedHandler();
        exceptionHandler = new GlobalExceptionHandler();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(Long id, String email, PapelUsuario papel) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setPapel(papel);
        usuario.setEmailVerificado(true);

        UsuarioDetails details = new UsuarioDetails(usuario);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Deve permitir acesso quando ID do recurso pertence ao usuário autenticado")
    void devePermitirAcessoQuandoIdPertenceAoUsuarioAutenticado() {
        autenticarComo(1L, "aluno@orienta.com.br", PapelUsuario.ROLE_ALUNO);

        assertDoesNotThrow(() -> ownershipService.validarPropriedadeUsuario(1L));
        assertTrue(ownershipService.isDonoDoRecurso(1L));
    }

    @Test
    @DisplayName("Deve interceptar e bloquear tentativa de ID Spoofing com 403 Forbidden")
    void deveBloquearTentativaDeIDSpoofing() {
        autenticarComo(1L, "aluno@orienta.com.br", PapelUsuario.ROLE_ALUNO);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                ownershipService.validarPropriedadeUsuario(999L)
        );

        assertTrue(ex.getMessage().contains("ID Spoofing") || ex.getMessage().contains("permissão"));
        assertFalse(ownershipService.isDonoDoRecurso(999L));
    }

    @Test
    @DisplayName("Deve conceder acesso irrestrito ao usuário com papel ROLE_ADMIN")
    void devePermitirAcessoAoAdminEmQualquerRecurso() {
        autenticarComo(99L, "admin@orienta.com.br", PapelUsuario.ROLE_ADMIN);

        assertDoesNotThrow(() -> ownershipService.validarPropriedadeUsuario(1L));
        assertDoesNotThrow(() -> ownershipService.validarPropriedadeUsuario(500L));
        assertTrue(ownershipService.isDonoDoRecurso(500L));
    }

    @Test
    @DisplayName("RN-001: Deve bloquear tentativa de auto-contratação de mentoria por professor")
    void deveBloquearAutoContratacaoDocente() {
        autenticarComo(5L, "prof@orienta.com.br", PapelUsuario.ROLE_PROFESSOR);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () ->
                ownershipService.validarNaoAutoContratacao(5L, 5L)
        );

        assertTrue(ex.getMessage().contains("RN-001"));
    }

    @Test
    @DisplayName("RN-001: Deve bloquear perfil Aluno ao tentar acessar recurso exclusivo Docente")
    void deveBloquearAlunoEmRecursoDocente() {
        autenticarComo(1L, "aluno@orienta.com.br", PapelUsuario.ROLE_ALUNO);

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                ownershipService.validarAcessoDocente(1L)
        );

        assertTrue(ex.getMessage().contains("RN-001") || ex.getMessage().contains("docente"));
    }

    @Test
    @DisplayName("RN-001: Deve permitir acesso docente ao usuário com papel ROLE_PROFESSOR")
    void devePermitirAcessoDocenteAoProfessor() {
        autenticarComo(2L, "professor@orienta.com.br", PapelUsuario.ROLE_PROFESSOR);

        assertDoesNotThrow(() -> ownershipService.validarAcessoDocente(2L));
    }

    @Test
    @DisplayName("Deve validar participantes legítimos de um agendamento e bloquear intrusos")
    void deveValidarParticipantesDeAgendamento() {
        autenticarComo(1L, "aluno@orienta.com.br", PapelUsuario.ROLE_ALUNO);

        // Participante legítimo (aluno)
        assertDoesNotThrow(() -> ownershipService.validarParticipanteAgendamento(1L, 2L));
        assertTrue(ownershipService.isParticipanteAgendamento(1L, 2L));

        // Intruso (usuário 3)
        autenticarComo(3L, "intruso@orienta.com.br", PapelUsuario.ROLE_ALUNO);
        assertThrows(AccessDeniedException.class, () ->
                ownershipService.validarParticipanteAgendamento(1L, 2L)
        );
        assertFalse(ownershipService.isParticipanteAgendamento(1L, 2L));
    }

    @Test
    @DisplayName("CustomAccessDeniedHandler deve formatar resposta HTTP 403 no envelope ApiResponse")
    void deveFormatar403NoEnvelopeApiResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/professores/1/financeiro/resumo");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AccessDeniedException ex = new AccessDeniedException("Acesso Proibido: Rota exclusiva docente (RN-001).");
        accessDeniedHandler.handle(request, response, ex);

        assertEquals(HttpStatus.FORBIDDEN.value(), response.getStatus());
        assertTrue(response.getContentType().contains("application/json"));

        String body = response.getContentAsString();
        ApiResponse<?> apiResponse = objectMapper.readValue(body, ApiResponse.class);

        assertFalse(apiResponse.isSuccess());
        assertTrue(apiResponse.getMessage().contains("Acesso Proibido"));
        assertNotNull(apiResponse.getTimestamp());
    }

    @Test
    @DisplayName("GlobalExceptionHandler deve tratar AccessDeniedException retornando HTTP 403 com ApiResponse")
    void deveTratarAccessDeniedNoGlobalExceptionHandler() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/usuarios/999/perfil");

        AccessDeniedException ex = new AccessDeniedException("Acesso Proibido (ID Spoofing): ID não pertence ao usuário autenticado.");
        ResponseEntity<ApiResponse<Object>> response = exceptionHandler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("ID Spoofing"));
    }
}
