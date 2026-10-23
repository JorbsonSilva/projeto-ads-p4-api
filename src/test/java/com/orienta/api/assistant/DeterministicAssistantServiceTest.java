package com.orienta.api.assistant;

import com.orienta.api.assistant.dto.TriageRequestDTO;
import com.orienta.api.assistant.dto.TriageResponseDTO;
import com.orienta.api.assistant.model.TriageStep;
import com.orienta.api.assistant.service.DeterministicAssistantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes do Assistente Determinístico por Regras (Fase 9)")
class DeterministicAssistantServiceTest {

    private DeterministicAssistantService assistantService;

    @BeforeEach
    void setUp() {
        assistantService = new DeterministicAssistantService();
    }

    @Test
    @DisplayName("F9-T1: Deve iniciar a triagem com opções de disciplinas do catálogo oficial")
    void deveIniciarTriagemComOpcoesDoCatalogo() {
        TriageRequestDTO request = new TriageRequestDTO();
        request.setCurrentStep(TriageStep.INITIAL);

        TriageResponseDTO response = assistantService.processStep(request);

        assertNotNull(response);
        assertEquals(TriageStep.SUBJECT_SELECTION, response.getNextStep());
        assertTrue(response.isDeterministicRuleBased());
        assertFalse(response.getQuickOptions().isEmpty());
        assertTrue(response.getQuickOptions().stream().anyMatch(opt -> "Cálculo I".equals(opt.getLabel())));
    }

    @Test
    @DisplayName("F9-T2 & F9-T3: Deve validar disciplina e retornar professores reais dentro do orçamento")
    void deveFiltrarProfessoresReaisDentroDoOrcamento() {
        TriageRequestDTO step1 = new TriageRequestDTO();
        step1.setCurrentStep(TriageStep.SUBJECT_SELECTION);
        step1.setSelectedOptionPayload("Cálculo I");
        step1.setDisciplinaNome("Cálculo I");

        TriageResponseDTO respStep1 = assistantService.processStep(step1);
        assertEquals(TriageStep.TOPIC_SELECTION, respStep1.getNextStep());

        // Avança para modalidade e orçamento
        TriageRequestDTO step2 = new TriageRequestDTO();
        step2.setCurrentStep(TriageStep.MODALITY_BUDGET);
        step2.setDisciplinaNome("Cálculo I");
        step2.setAssuntoNome("Derivadas");
        step2.setSelectedOptionPayload("ONLINE:60.00");

        TriageResponseDTO respStep2 = assistantService.processStep(step2);

        assertEquals(TriageStep.RECOMMENDATION, respStep2.getNextStep());
        assertFalse(respStep2.getRecommendedTeachers().isEmpty());
        // Garante que nenhum professor acima de R$ 60/h seja retornado
        respStep2.getRecommendedTeachers().forEach(prof ->
                assertTrue(prof.getValorHora().compareTo(new BigDecimal("60.00")) <= 0)
        );
    }

    @Test
    @DisplayName("F9-T4 & F9-T7: Deve gerar card de resumo que exige confirmação manual do aluno")
    void deveGerarCardDeResumoSemAgendarAutomaticamente() {
        TriageRequestDTO request = new TriageRequestDTO();
        request.setCurrentStep(TriageStep.RECOMMENDATION);
        request.setSelectedOptionPayload("102"); // Profa. Juliana
        request.setDisciplinaNome("Cálculo I");
        request.setAssuntoNome("Limites");

        TriageResponseDTO response = assistantService.processStep(request);

        assertEquals(TriageStep.COMPLETED, response.getNextStep());
        assertNotNull(response.getActionCard());
        assertTrue(response.getActionCard().getRequiresUserConfirmation());
        assertEquals(102L, response.getActionCard().getProfessorId());
        assertNotNull(response.getActionCard().getTargetScreenRoute());
    }

    @Test
    @DisplayName("F9-T5: Deve direcionar perguntas fora do escopo acadêmico para fila de suporte humano")
    void deveEncaminharPerguntaForaDoEscopoParaSuporte() {
        TriageRequestDTO request = new TriageRequestDTO();
        request.setCurrentStep(TriageStep.INITIAL);
        request.setUserMessage("Quero comprar uma pizza de calabresa");

        TriageResponseDTO response = assistantService.processStep(request);

        assertEquals(TriageStep.SUPPORT_HANDOFF, response.getNextStep());
        assertNotNull(response.getSupportTicketDraftProtocol());
        assertTrue(response.getSupportTicketDraftProtocol().startsWith("SUP-"));
    }
}
