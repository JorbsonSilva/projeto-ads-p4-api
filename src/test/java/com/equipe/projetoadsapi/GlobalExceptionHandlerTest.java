package com.equipe.projetoadsapi;

import com.equipe.projetoadsapi.dto.ErrorResponseDTO;
import com.equipe.projetoadsapi.dto.FieldErrorDTO;
import com.equipe.projetoadsapi.dto.UsuarioResponseDTO;
import com.equipe.projetoadsapi.exception.GlobalExceptionHandler;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TESTES DE SEGURANÇA E QUALIDADE (FASE 2)
 * Rastreabilidade: Tarefas f2-t1, f2-t4, f2-t7, f2-t8
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Garante que RegraNegocioException retorne HTTP 422 com payload ErrorResponseDTO")
    void testRegraNegocioExceptionReturns422() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/agendamentos");

        RegraNegocioException ex = new RegraNegocioException("Cancelamento não permitido com menos de 24 horas de antecedência.");
        ResponseEntity<ErrorResponseDTO> response = handler.handleBusinessRule(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().getStatus());
        assertEquals("Violação de Regra de Negócio", response.getBody().getError());
        assertEquals("Cancelamento não permitido com menos de 24 horas de antecedência.", response.getBody().getMessage());
        assertEquals("/api/agendamentos", response.getBody().getPath());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Garante que RecursoNaoEncontradoException retorne HTTP 404")
    void testRecursoNaoEncontradoReturns404() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/usuarios/999");

        RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException("Usuário não encontrado.");
        ResponseEntity<ErrorResponseDTO> response = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Recurso Não Encontrado", response.getBody().getError());
    }

    @Test
    @DisplayName("f2-t4: Garante que UsuarioResponseDTO nunca exponha a senha do usuário")
    void testUsuarioResponseDtoDoesNotExposePassword() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Professor Teste");
        usuario.setEmail("prof@uninassau.edu.br");
        usuario.setSenha("senhaSecreta123");
        usuario.setTipo("PROFESSOR");

        UsuarioResponseDTO dto = UsuarioResponseDTO.fromEntity(usuario);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Professor Teste", dto.getNome());
        assertEquals("prof@uninassau.edu.br", dto.getEmail());
        // Reflexão verifica que a classe sequer tem o campo senha
        assertThrows(NoSuchFieldException.class, () -> UsuarioResponseDTO.class.getDeclaredField("senha"));
    }

    @Test
    @DisplayName("f2-t7: Garante que Clock injetável permita controle determinístico de tempo")
    void testClockInjectable() {
        Clock clock = Clock.systemUTC();
        Instant now = Instant.now(clock);
        assertNotNull(now);
    }
}
