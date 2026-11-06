package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes automatizados do controller de Chat, Mensagens e Status de Leitura (BACK-013, BACK-014).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-014) & Segurança Anti-ID Spoofing.
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

        mockMvc.perform(get("/api/v1/chat/conversas?usuarioId=99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Acesso negado: Acesso negado: Você não tem permissão para visualizar dados de outro usuário."));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(99L);
        verify(chatService, never()).listarConversas(any());
    }

    @Test
    @DisplayName("[BACK-014] Deve listar histórico de mensagens por agendamento (GET /agendamentos/{id}/mensagens)")
    void deveListarMensagensPorAgendamento() throws Exception {
        MensagemDTO m1 = MensagemDTO.builder()
                .id(1L)
                .conversaId(5L)
                .agendamentoId(101L)
                .remetenteId(10L)
                .remetenteNome("Ana Clara")
                .remetentePapel("ALUNO")
                .conteudo("Boa tarde professor!")
                .tipo("TEXTO")
                .lida(true)
                .confirmadaLeitura(true)
                .enviadaEm(Instant.now())
                .build();

        when(chatService.listarMensagens(101L, 10L)).thenReturn(List.of(m1));

        mockMvc.perform(get("/api/v1/chat/agendamentos/101/mensagens?usuarioId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].conteudo").value("Boa tarde professor!"))
                .andExpect(jsonPath("$.data[0].lida").value(true))
                .andExpect(jsonPath("$.data[0].confirmadaLeitura").value(true));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(10L);
        verify(chatService, times(1)).listarMensagens(101L, 10L);
    }

    @Test
    @DisplayName("[BACK-014] Deve recuperar histórico paginado de mensagens por agendamento")
    void deveListarMensagensPaginadasPorAgendamento() throws Exception {
        MensagemDTO m1 = MensagemDTO.builder()
                .id(2L)
                .conversaId(5L)
                .agendamentoId(101L)
                .conteudo("Mensagem paginada")
                .build();

        var pageResult = new PageImpl<>(List.of(m1), PageRequest.of(0, 10, Sort.by("enviadaEm").ascending()), 1);

        when(chatService.listarMensagensPaginadasPorAgendamento(eq(101L), eq(10L), any()))
                .thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/chat/agendamentos/101/mensagens?usuarioId=10&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].conteudo").value("Mensagem paginada"));
    }

    @Test
    @DisplayName("[BACK-014] Deve enviar mensagem unificada com anexo (POST /api/v1/chat/mensagens)")
    void deveEnviarMensagemUnificadaComAnexo() throws Exception {
        MensagemRequestDTO request = MensagemRequestDTO.builder()
                .agendamentoId(101L)
                .remetenteId(10L)
                .conteudo("Segue o comprovante em anexo.")
                .tipo("ARQUIVO")
                .anexoUrl("https://orienta.com.br/anexos/doc.pdf")
                .nomeArquivo("doc.pdf")
                .tamanhoArquivo("1.1 MB")
                .build();

        MensagemDTO response = MensagemDTO.builder()
                .id(50L)
                .agendamentoId(101L)
                .remetenteId(10L)
                .remetenteNome("Ana Clara")
                .remetentePapel("ALUNO")
                .conteudo("Segue o comprovante em anexo.")
                .tipo("ARQUIVO")
                .anexoUrl("https://orienta.com.br/anexos/doc.pdf")
                .nomeArquivo("doc.pdf")
                .tamanhoArquivo("1.1 MB")
                .lida(false)
                .enviadaEm(Instant.now())
                .build();

        when(chatService.enviarMensagemUnificada(any(MensagemRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/mensagens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(50))
                .andExpect(jsonPath("$.data.tipo").value("ARQUIVO"))
                .andExpect(jsonPath("$.data.anexoUrl").value("https://orienta.com.br/anexos/doc.pdf"))
                .andExpect(jsonPath("$.data.lida").value(false))
                .andExpect(jsonPath("$.message").value("Mensagem enviada com sucesso"));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(10L);
        verify(chatService, times(1)).enviarMensagemUnificada(any(MensagemRequestDTO.class));
    }

    @Test
    @DisplayName("[BACK-014] Deve atualizar em lote status de leitura da conversa (PATCH /conversas/{id}/ler)")
    void deveAtualizarEmLoteStatusDeLeituraConversa() throws Exception {
        when(chatService.marcarMensagensComoLidas(1L, 10L)).thenReturn(4);

        mockMvc.perform(patch("/api/v1/chat/conversas/1/ler?usuarioId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(4))
                .andExpect(jsonPath("$.message").value("4 mensagens marcadas como lidas"));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(10L);
        verify(chatService, times(1)).marcarMensagensComoLidas(1L, 10L);
    }
}
