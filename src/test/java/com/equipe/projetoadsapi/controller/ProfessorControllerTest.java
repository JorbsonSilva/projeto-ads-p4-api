package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.AlunoAtendidoDTO;
import com.equipe.projetoadsapi.security.CustomUserDetailsService;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
import com.equipe.projetoadsapi.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes unitários e web mock do ProfessorController (Tarefa [BACK-019]).
 * Valida a gestão de alunos atendidos pelo docente ("Meus Alunos"),
 * envelopamento em ApiResponse, atalhos de navegação e bloqueio contra ID Spoofing (RN-001).
 */
@WebMvcTest(ProfessorController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ProfessorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private ResourceOwnershipService ownershipService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @Test
    @DisplayName("Deve retornar lista de alunos atendidos para o professor com sucesso")
    @WithMockUser(username = "prof@orienta.com.br", roles = {"PROFESSOR"})
    void deveRetornarAlunosAtendidosComSucesso() throws Exception {
        Long professorId = 10L;

        AlunoAtendidoDTO aluno1 = AlunoAtendidoDTO.builder()
                .alunoId(101L)
                .id(101L)
                .nome("Lucas Ribeiro")
                .fotoUrl("https://orienta.com/avatar1.jpg")
                .email("lucas.ribeiro@academico.br")
                .cidade("João Pessoa")
                .estado("PB")
                .curso("Ciência da Computação")
                .instituicao("UFPB")
                .totalAulasConcluidas(4)
                .totalAulas(4)
                .horasAcumuladas(5.0)
                .horasTotais(5.0)
                .dataUltimaAula("2026-11-05")
                .ultimaAulaData("2026-11-05")
                .disciplinasCursadas(List.of("Estruturas de Dados", "Algoritmos em Grafos"))
                .disciplinas(List.of("Estruturas de Dados", "Algoritmos em Grafos"))
                .agendamentoIdMaisRecente(201L)
                .conversaId(55L)
                .statusMentoria("ATIVO")
                .formatoPreferido("HIBRIDO")
                .build();

        doNothing().when(ownershipService).validarAcessoDocente(professorId);
        when(usuarioService.listarAlunosAtendidosDoProfessor(professorId)).thenReturn(List.of(aluno1));

        mockMvc.perform(get("/api/v1/professores/{id}/alunos", professorId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.sucesso").value(true))
                .andExpect(jsonPath("$.dados[0].alunoId").value(101))
                .andExpect(jsonPath("$.dados[0].nome").value("Lucas Ribeiro"))
                .andExpect(jsonPath("$.dados[0].totalAulasConcluidas").value(4))
                .andExpect(jsonPath("$.dados[0].horasAcumuladas").value(5.0))
                .andExpect(jsonPath("$.dados[0].disciplinasCursadas[0]").value("Estruturas de Dados"))
                .andExpect(jsonPath("$.dados[0].agendamentoIdMaisRecente").value(201))
                .andExpect(jsonPath("$.dados[0].conversaId").value(55));

        verify(ownershipService, times(1)).validarAcessoDocente(professorId);
        verify(usuarioService, times(1)).listarAlunosAtendidosDoProfessor(professorId);
    }

    @Test
    @DisplayName("Deve bloquear com HTTP 403 Forbidden quando ocorrer tentativa de ID Spoofing")
    @WithMockUser(username = "prof.invasor@orienta.com.br", roles = {"PROFESSOR"})
    void deveBloquearTentativaIdSpoofing() throws Exception {
        Long professorAlvoId = 999L;

        doThrow(new AccessDeniedException("Acesso Proibido (ID Spoofing): Você não tem permissão para acessar dados de outro usuário."))
                .when(ownershipService).validarAcessoDocente(professorAlvoId);

        mockMvc.perform(get("/api/v1/professores/{id}/alunos", professorAlvoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.sucesso").value(false))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.containsString("ID Spoofing")));

        verify(usuarioService, never()).listarAlunosAtendidosDoProfessor(anyLong());
    }

    @Test
    @DisplayName("Deve bloquear aluno tentando acessar a lista de alunos atendidos de professor (RN-001)")
    @WithMockUser(username = "aluno@orienta.com.br", roles = {"ALUNO"})
    void deveBloquearPerfilAluno() throws Exception {
        Long professorId = 10L;

        doThrow(new AccessDeniedException("Acesso Proibido (RN-001): Este recurso é restrito exclusivamente ao perfil docente/professor."))
                .when(ownershipService).validarAcessoDocente(professorId);

        mockMvc.perform(get("/api/v1/professores/{id}/alunos", professorId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.sucesso").value(false));

        verify(usuarioService, never()).listarAlunosAtendidosDoProfessor(anyLong());
    }
}
