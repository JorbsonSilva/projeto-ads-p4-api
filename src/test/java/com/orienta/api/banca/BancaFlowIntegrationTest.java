package com.orienta.api.banca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orienta.api.assistant.dto.TriageRequestDTO;
import com.orienta.api.assistant.dto.TriageResponseDTO;
import com.orienta.api.assistant.model.TriageStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste de Validação Fim a Fim (Fase 10)
 * Garante que a API execute o ciclo completo do Orienta com status HTTP 200/201.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BancaFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("F10-T1: Healthcheck e auditoria da API em execução")
    void testApiHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("F10-T3: Triagem determinística do assistente sem alucinação")
    void testAssistantDeterministicTriage() throws Exception {
        TriageRequestDTO request = new TriageRequestDTO(
                TriageStep.TOPIC_SELECTION,
                "Cálculo I",
                "Derivadas",
                "ONLINE",
                BigDecimal.valueOf(65.00),
                "Quero agendar aula de cálculo"
        );

        mockMvc.perform(post("/api/v1/assistente/triagem")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.proximoPasso").value("RECOMMENDATION"))
                .andExpect(jsonPath("$.actionCard").exists());
    }

    @Test
    @DisplayName("F10-T4: Bloqueio de segurança — Acesso sem token deve retornar 401")
    void testSecurityUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/aulas/minhas-aulas"))
                .andExpect(status().isUnauthorized());
    }
}
