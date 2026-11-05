package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.AptidaoRequestDTO;
import com.equipe.projetoadsapi.dto.DisponibilidadeRequestDTO;
import com.equipe.projetoadsapi.dto.PerfilAtualizacaoDTO;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import com.equipe.projetoadsapi.security.CustomUserDetailsService;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TESTES AUTOMATIZADOS DO PERFIL POR PERSONA E UNIFICADO (TAREFA [BACK-004]).
 * Rastreabilidade: Fase 4 - f4-t1, f4-t3 & Tarefa [BACK-004] (Requisito RN-001)
 * 
 * Valida:
 * 1. Consulta consolidada de Perfil Aluno (dados cadastrais + interesses + progresso).
 * 2. Consulta consolidada de Perfil Professor (dados cadastrais + aptidões + resumo + horários).
 * 3. Atualização de dados cadastrais e disciplinas de interesse (Aluno).
 * 4. Atualização de valor/hora, biografia, disciplinas e disponibilidade semanal (Professor).
 * 5. Respostas encapsuladas no padrão ApiResponse<PerfilCompletoDTO>.
 */
@WebMvcTest(PerfilController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private SaberRepository saberRepository;

    @MockBean
    private InteresseRepository interesseRepository;

    @MockBean
    private AptidaoRepository aptidaoRepository;

    @MockBean
    private DisponibilidadeRepository disponibilidadeRepository;

    @MockBean
    private ResourceOwnershipService ownershipService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService blacklistService;

    @Test
    @DisplayName("Deve retornar perfil completo do aluno com interesses e progresso")
    void deveRetornarPerfilCompletoAluno() throws Exception {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        aluno.setNome("Jorbson Silva");
        aluno.setEmail("aluno@orienta.com.br");
        aluno.setPapel(PapelUsuario.ROLE_ALUNO);
        aluno.setBioAluno("Estudante de ADS na UFPB");
        aluno.setEmailVerificado(true);

        Saber saberCalculo = new Saber();
        saberCalculo.setId(10L);
        saberCalculo.setNome("Cálculo I");
        saberCalculo.setCategoria("Exatas");

        Interesse interesse = new Interesse();
        interesse.setId(100L);
        interesse.setUsuario(aluno);
        interesse.setSaber(saberCalculo);
        interesse.setNivel("Intermediário");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(interesseRepository.findByUsuarioId(1L)).thenReturn(List.of(interesse));
        when(aptidaoRepository.findByUsuarioId(1L)).thenReturn(List.of());
        when(disponibilidadeRepository.findByUsuarioId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/perfil/aluno/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.nome").value("Jorbson Silva"))
                .andExpect(jsonPath("$.data.tipo").value("ALUNO"))
                .andExpect(jsonPath("$.data.bio").value("Estudante de ADS na UFPB"))
                .andExpect(jsonPath("$.data.interesses[0].nome").value("Cálculo I"))
                .andExpect(jsonPath("$.data.progresso").exists());

        verify(ownershipService, times(1)).validarAcessoAluno(1L);
    }

    @Test
    @DisplayName("Deve retornar perfil completo do professor com aptidões, resumo mensal e disponibilidades")
    void deveRetornarPerfilCompletoProfessor() throws Exception {
        Usuario professor = new Usuario();
        professor.setId(2L);
        professor.setNome("Prof. Carlos Mendes");
        professor.setEmail("carlos@orienta.com.br");
        professor.setPapel(PapelUsuario.ROLE_PROFESSOR);
        professor.setBioProfessor("Especialista em Algoritmos e IA");
        professor.setOferecePresencial(true);
        professor.setRegiaoAtendimento("Manaíra");
        professor.setRaioAtendimentoKm(15);
        professor.setEmailVerificado(true);

        Saber saberAlg = new Saber();
        saberAlg.setId(20L);
        saberAlg.setNome("Estrutura de Dados");
        saberAlg.setCategoria("Tecnologia");

        Aptidao aptidao = new Aptidao();
        aptidao.setId(200L);
        aptidao.setUsuario(professor);
        aptidao.setSaber(saberAlg);
        aptidao.setNivelDominio("Avançado");
        aptidao.setPrecoHora(BigDecimal.valueOf(70.00));

        Disponibilidade disp = new Disponibilidade();
        disp.setId(300L);
        disp.setUsuario(professor);
        disp.setDiaSemana("SEGUNDA");
        disp.setHoraInicio(LocalTime.of(14, 0));
        disp.setHoraFim(LocalTime.of(18, 0));

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(interesseRepository.findByUsuarioId(2L)).thenReturn(List.of());
        when(aptidaoRepository.findByUsuarioId(2L)).thenReturn(List.of(aptidao));
        when(disponibilidadeRepository.findByUsuarioId(2L)).thenReturn(List.of(disp));

        mockMvc.perform(get("/api/v1/perfil/professor/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.nome").value("Prof. Carlos Mendes"))
                .andExpect(jsonPath("$.data.tipo").value("PROFESSOR"))
                .andExpect(jsonPath("$.data.aptidoes[0].nome").value("Estrutura de Dados"))
                .andExpect(jsonPath("$.data.aptidoes[0].preco").value(70.00))
                .andExpect(jsonPath("$.data.disponibilidades[0].diaSemana").value("SEGUNDA"))
                .andExpect(jsonPath("$.data.resumoMes").exists())
                .andExpect(jsonPath("$.data.configuracoesEnsino").exists());

        verify(ownershipService, times(1)).validarAcessoDocente(2L);
    }

    @Test
    @DisplayName("Deve atualizar perfil do aluno com biografia e novas disciplinas de interesse")
    void deveAtualizarPerfilAluno() throws Exception {
        Usuario aluno = new Usuario();
        aluno.setId(1L);
        aluno.setNome("Jorbson Silva");
        aluno.setEmail("aluno@orienta.com.br");
        aluno.setPapel(PapelUsuario.ROLE_ALUNO);

        PerfilAtualizacaoDTO dto = PerfilAtualizacaoDTO.builder()
                .nome("Jorbson Silva Atualizado")
                .bio("Nova biografia de estudante")
                .interessesNomes(List.of("Banco de Dados SQL", "Python"))
                .build();

        Saber saberBd = new Saber();
        saberBd.setId(30L);
        saberBd.setNome("Banco de Dados SQL");

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(aluno);
        when(saberRepository.findByNome(anyString())).thenReturn(Optional.of(saberBd));
        when(interesseRepository.findByUsuarioId(1L)).thenReturn(List.of());

        mockMvc.perform(put("/api/v1/perfil/aluno/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Perfil do aluno atualizado com sucesso!"));

        verify(ownershipService, times(1)).validarAcessoAluno(1L);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve atualizar perfil do professor com tarifas, disciplinas e grade de horários")
    void deveAtualizarPerfilProfessor() throws Exception {
        Usuario professor = new Usuario();
        professor.setId(2L);
        professor.setNome("Prof. Carlos Mendes");
        professor.setEmail("carlos@orienta.com.br");
        professor.setPapel(PapelUsuario.ROLE_PROFESSOR);

        AptidaoRequestDTO aptidaoDTO = AptidaoRequestDTO.builder()
                .nomeSaber("Matemática Discreta")
                .nivelDominio("Avançado")
                .precoHora(BigDecimal.valueOf(85.00))
                .build();

        DisponibilidadeRequestDTO dispDTO = DisponibilidadeRequestDTO.builder()
                .diaSemana("QUARTA")
                .horaInicio("09:00")
                .horaFim("12:00")
                .build();

        PerfilAtualizacaoDTO dto = PerfilAtualizacaoDTO.builder()
                .nome("Prof. Carlos Mendes M.Sc.")
                .bio("Nova biografia docente")
                .oferecePresencial(true)
                .regiaoAtendimento("Tambaú")
                .raioAtendimentoKm(20)
                .aptidoes(List.of(aptidaoDTO))
                .disponibilidades(List.of(dispDTO))
                .build();

        Saber saberMat = new Saber();
        saberMat.setId(40L);
        saberMat.setNome("Matemática Discreta");

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(professor);
        when(saberRepository.findByNome(anyString())).thenReturn(Optional.of(saberMat));
        when(aptidaoRepository.findByUsuarioId(2L)).thenReturn(List.of());
        when(disponibilidadeRepository.findByUsuarioId(2L)).thenReturn(List.of());

        mockMvc.perform(put("/api/v1/perfil/professor/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Perfil do professor atualizado com sucesso!"));

        verify(ownershipService, times(1)).validarAcessoDocente(2L);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }
}
