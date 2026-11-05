package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.AptidaoRepository;
import com.equipe.projetoadsapi.repository.DisponibilidadeRepository;
import com.equipe.projetoadsapi.repository.InteresseRepository;
import com.equipe.projetoadsapi.security.CustomUserDetailsService;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
import com.equipe.projetoadsapi.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TESTES AUTOMATIZADOS DO ENDPOINT DE PERFIL UNIFICADO EM USUARIOCONTROLLER (TAREFA [BACK-004]).
 * Rastreabilidade: Fase 4 - f4-t3 & Tarefa [BACK-004] (Requisito RN-001)
 * 
 * Valida:
 * 1. GET /api/v1/usuarios/{id}/perfil retornando o PerfilCompletoDTO consolidado.
 * 2. PUT /api/v1/usuarios/{id}/aluno atualizando bio e disciplinas de interesse.
 * 3. PUT /api/v1/usuarios/{id}/professor atualizando bio, aptidões e grade de horários.
 */
@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private ResourceOwnershipService ownershipService;

    @MockBean
    private InteresseRepository interesseRepository;

    @MockBean
    private AptidaoRepository aptidaoRepository;

    @MockBean
    private DisponibilidadeRepository disponibilidadeRepository;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService blacklistService;

    @Test
    @DisplayName("Deve retornar PerfilCompletoDTO consolidado com interesses e disponibilidades via /usuarios/{id}/perfil")
    void deveRetornarPerfilConsolidadoComSucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Lucas Silva");
        usuario.setEmail("lucas@orienta.com.br");
        usuario.setCpf("12345678900");
        usuario.setPapel(PapelUsuario.ROLE_ALUNO);
        usuario.setBioAluno("Aluno dedicado de Ciência da Computação");
        usuario.setEmailVerificado(true);

        Saber saber = new Saber();
        saber.setId(10L);
        saber.setNome("Algoritmos");
        saber.setCategoria("Tecnologia");

        Interesse interesse = new Interesse();
        interesse.setId(100L);
        interesse.setUsuario(usuario);
        interesse.setSaber(saber);
        interesse.setNivel("Básico");

        when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
        when(interesseRepository.findByUsuarioId(1L)).thenReturn(List.of(interesse));
        when(aptidaoRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(disponibilidadeRepository.findByUsuarioId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/usuarios/1/perfil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nome").value("Lucas Silva"))
                .andExpect(jsonPath("$.data.email").value("lucas@orienta.com.br"))
                .andExpect(jsonPath("$.data.tipo").value("ALUNO"))
                .andExpect(jsonPath("$.data.interesses[0].nome").value("Algoritmos"))
                .andExpect(jsonPath("$.data.progresso.materiasCursando").exists());

        verify(ownershipService, times(1)).validarPropriedadeUsuario(1L);
    }

    @Test
    @DisplayName("Deve atualizar perfil de aluno via /usuarios/{id}/aluno e retornar PerfilCompletoDTO")
    void deveAtualizarPerfilAlunoComSucesso() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Lucas Silva");
        usuario.setEmail("lucas@orienta.com.br");
        usuario.setPapel(PapelUsuario.ROLE_ALUNO);

        Map<String, Object> payload = Map.of(
                "nome", "Lucas Silva Atualizado",
                "bio", "Bio atualizada via endpoint /usuarios/1/aluno",
                "interessesIds", List.of(10, 20)
        );

        when(usuarioService.buscarPorId(1L)).thenReturn(usuario);
        when(usuarioService.atualizarBioAluno(eq(1L), anyString(), anyList())).thenReturn(usuario);
        when(interesseRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(aptidaoRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(disponibilidadeRepository.findByUsuarioId(1L)).thenReturn(List.of());

        mockMvc.perform(put("/api/v1/usuarios/1/aluno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Perfil de aluno atualizado com sucesso"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(ownershipService, times(1)).validarAcessoAluno(1L);
        verify(usuarioService, times(1)).atualizarBioAluno(eq(1L), anyString(), anyList());
    }

    @Test
    @DisplayName("Deve atualizar perfil de professor via /usuarios/{id}/professor e retornar PerfilCompletoDTO")
    void deveAtualizarPerfilProfessorComSucesso() throws Exception {
        Usuario professor = new Usuario();
        professor.setId(2L);
        professor.setNome("Prof. Roberto Gomes");
        professor.setEmail("roberto@orienta.com.br");
        professor.setPapel(PapelUsuario.ROLE_PROFESSOR);

        Map<String, Object> payload = Map.of(
                "nome", "Prof. Roberto Gomes Ph.D.",
                "bio", "Bio docente atualizada",
                "aptidoesNova", List.of(Map.of("saberId", 5L, "nivelDominio", "Avançado", "precoHora", 90.00)),
                "disponibilidadesNova", List.of(Map.of("diaSemana", "TERCA", "horaInicio", "08:00", "horaFim", "12:00"))
        );

        when(usuarioService.buscarPorId(2L)).thenReturn(professor);
        when(usuarioService.atualizarPerfilProfessor(eq(2L), anyString(), anyList(), anyList())).thenReturn(professor);
        when(interesseRepository.findByUsuarioId(2L)).thenReturn(List.of());
        when(aptidaoRepository.findByUsuarioId(2L)).thenReturn(List.of());
        when(disponibilidadeRepository.findByUsuarioId(2L)).thenReturn(List.of());

        mockMvc.perform(put("/api/v1/usuarios/2/professor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Perfil de professor atualizado com sucesso"))
                .andExpect(jsonPath("$.data.id").value(2));

        verify(ownershipService, times(1)).validarAcessoDocente(2L);
        verify(usuarioService, times(1)).atualizarPerfilProfessor(eq(2L), anyString(), anyList(), anyList());
    }
}
