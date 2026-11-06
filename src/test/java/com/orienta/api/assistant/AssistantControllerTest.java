package com.orienta.api.assistant;

import com.equipe.projetoadsapi.security.CustomUserDetailsService;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orienta.api.assistant.controller.AssistantController;
import com.orienta.api.assistant.dto.*;
import com.orienta.api.assistant.model.TriageStep;
import com.orienta.api.assistant.service.DeterministicAssistantService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Testes Web Mock do AssistantController (Tarefa [BACK-018])")
public class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DeterministicAssistantService assistantService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    @Test
    @DisplayName("[BACK-018] POST /api/v1/assistente/triagem deve retornar HTTP 200 no envelope ApiResponse")
    void deveProcessarPassoDeTriagemNoEnvelopeApiResponse() throws Exception {
        TriageRequestDTO request = new TriageRequestDTO();
        request.setCurrentStep(TriageStep.INITIAL);

        TriageResponseDTO mockResponse = TriageResponseDTO.create(
                "Olá! Sou o Assistente Acadêmico do Orienta.",
                TriageStep.SUBJECT_SELECTION,
                "RULE_TREE_INITIAL_V9"
        );
        mockResponse.setQuickOptions(List.of(
                new QuickOptionDTO("opt_calc", "Cálculo I", "Cálculo I", "Limites e Derivadas")
        ));

        when(assistantService.processStep(any(TriageRequestDTO.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/assistente/triagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.data.assistantMessage").value("Olá! Sou o Assistente Acadêmico do Orienta."))
                .andExpect(jsonPath("$.data.nextStep").value("SUBJECT_SELECTION"))
                .andExpect(jsonPath("$.data.quickOptions[0].label").value("Cálculo I"));
    }

    @Test
    @DisplayName("[BACK-018] POST /api/v1/assistente/perguntar deve retornar resposta pedagógica no envelope ApiResponse")
    void deveProcessarPerguntaPedagogicaNoEnvelopeApiResponse() throws Exception {
        PerguntaAssistenteDTO request = new PerguntaAssistenteDTO(
                "Como resolver uma integral por partes?",
                "Cálculo I",
                "EXPLICAR_EXERCICIO"
        );

        TriageResponseDTO mockResponse = new TriageResponseDTO();
        mockResponse.setAssistantMessage("Vamos resolver passo a passo pela regra LIATE!");
        mockResponse.setFormula("∫ u dv = u·v - ∫ v du");
        mockResponse.setBulletPoints(List.of("Passo 1: Identificar u e dv", "Passo 2: Integrar"));
        mockResponse.setRuleReferenceCode("RULE_PEDAGOGIC_FALLBACK_V9");
        mockResponse.setRecommendedTeachers(List.of(
                new ProfessorResumoDTO(102L, "Profa. Juliana", "Mestre", new BigDecimal("55.00"), 4.8, 42, List.of("ONLINE"), "Centro", List.of("Cálculo I"), true)
        ));

        when(assistantService.processarPergunta(any(PerguntaAssistenteDTO.class), any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/assistente/perguntar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.assistantMessage").value("Vamos resolver passo a passo pela regra LIATE!"))
                .andExpect(jsonPath("$.data.formula").value("∫ u dv = u·v - ∫ v du"))
                .andExpect(jsonPath("$.data.bulletPoints[0]").value("Passo 1: Identificar u e dv"))
                .andExpect(jsonPath("$.data.recommendedTeachers[0].nome").value("Profa. Juliana"));
    }

    @Test
    @DisplayName("[BACK-018] GET /api/v1/assistente/regras deve retornar regras de decisão e chips homologados")
    void deveRetornarRegrasDeDecisao() throws Exception {
        mockMvc.perform(get("/api/v1/assistente/regras"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sistema").value("Orienta - Assistente Determinístico Fase 9"))
                .andExpect(jsonPath("$.data.chipsPedagogicos").isArray());
    }

    @Test
    @DisplayName("[BACK-018] GET /api/v1/assistente/faq deve retornar perguntas frequentes no envelope ApiResponse")
    void deveRetornarFAQNoEnvelopeApiResponse() throws Exception {
        mockMvc.perform(get("/api/v1/assistente/faq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].pergunta").isNotEmpty());
    }

    @Test
    @DisplayName("[BACK-018] POST /api/v1/assistente/perguntar deve validar pergunta vazia e rejeitar com HTTP 400")
    void deveValidarPerguntaVaziaERejeitarCom400() throws Exception {
        PerguntaAssistenteDTO request = new PerguntaAssistenteDTO("");

        mockMvc.perform(post("/api/v1/assistente/perguntar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
