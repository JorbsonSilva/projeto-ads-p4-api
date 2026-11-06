package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ConversaDTO;
import com.equipe.projetoadsapi.security.CustomUserDetailsService;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
import com.equipe.projetoadsapi.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes automatizados do controller de Chat e Caixa de Entrada (BACK-013).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-013) & Segurança Anti-ID Spoofing.
 */
@WebMvcTest(ChatController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResourceOwnershipService ownershipService;

    @MockBean
    private ChatService chatService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService blacklistService;

    @Test
    @DisplayName("[BACK-013] Deve retornar 200 OK com a caixa de entrada unificada de conversas do usuário")
    void deveRetornarCaixaDeEntradaDeConversas() throws Exception {
        ConversaDTO c1 = ConversaDTO.builder()
                .id(1L)
                .agendamentoId(101L)
                .alunoId(10L)
                .alunoNome("Ana Clara")
                .professorId(20L)
                .professorNome("Prof. Lucas Rocha")
                .assunto("Matemática")
                .disciplinaNome("Matemática")
                .interlocutorId(20L)
                .nomeInterlocutor("Prof. Lucas Rocha")
                .fotoUrlInterlocutor("https://orienta.com.br/fotos/lucas.jpg")
                .papelInterlocutor("PROFESSOR")
                .ultimaMensagem("Olá Ana, nos vemos na aula de hoje!")
                .dataHoraUltimaMensagem(Instant.now())
                .enviadaPorMim(false)
                .mensagensNaoLidas(2L)
                .ativa(true)
                .build();

        when(chatService.listarConversas(10L)).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/v1/chat/conversas?usuarioId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].agendamentoId").value(101))
                .andExpect(jsonPath("$.data[0].disciplinaNome").value("Matemática"))
                .andExpect(jsonPath("$.data[0].interlocutorId").value(20))
                .andExpect(jsonPath("$.data[0].nomeInterlocutor").value("Prof. Lucas Rocha"))
                .andExpect(jsonPath("$.data[0].papelInterlocutor").value("PROFESSOR"))
                .andExpect(jsonPath("$.data[0].ultimaMensagem").value("Olá Ana, nos vemos na aula de hoje!"))
                .andExpect(jsonPath("$.data[0].mensagensNaoLidas").value(2))
                .andExpect(jsonPath("$.message").value("Caixa de entrada recuperada com sucesso"));

        verify(chatService, times(1)).listarConversas(10L);
    }

    @Test
    @DisplayName("[BACK-013] Deve bloquear com 403 Forbidden para tentativa de ID Spoofing na caixa de entrada")
    void deveBloquearTentativaDeIdSpoofing() throws Exception {
        doThrow(new AccessDeniedException("Acesso negado: Você não tem permissão para visualizar dados de outro usuário."))
                .when(ownershipService).validarPropriedadeUsuario(99L);

        // Simulando tentativa de acessar conversas de outro usuário
        mockMvc.perform(get("/api/v1/chat/conversas?usuarioId=99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Acesso negado: Acesso negado: Você não tem permissão para visualizar dados de outro usuário."));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(99L);
        verify(chatService, never()).listarConversas(any());
    }

    @Test
    @DisplayName("[BACK-013] Deve retornar metadados da conversa vinculada ao agendamento")
    void deveRetornarConversaPorAgendamento() throws Exception {
        ConversaDTO dto = ConversaDTO.builder()
                .id(1L)
                .agendamentoId(101L)
                .assunto("Química Geral")
                .mensagensNaoLidas(0L)
                .build();

        when(chatService.obterOuCriarConversa(101L, 10L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/chat/agendamento/101?usuarioId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.agendamentoId").value(101));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(10L);
        verify(chatService, times(1)).obterOuCriarConversa(101L, 10L);
    }
}
