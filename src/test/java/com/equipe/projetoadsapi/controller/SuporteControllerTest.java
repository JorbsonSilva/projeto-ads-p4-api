package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.CriarTicketDTO;
import com.equipe.projetoadsapi.dto.ResponderTicketDTO;
import com.equipe.projetoadsapi.dto.TicketSuporteDTO;
import com.equipe.projetoadsapi.model.CategoriaTicket;
import com.equipe.projetoadsapi.model.PrioridadeTicket;
import com.equipe.projetoadsapi.model.StatusTicket;
import com.equipe.projetoadsapi.security.CustomUserDetailsService;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
import com.equipe.projetoadsapi.service.SuporteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * Testes automatizados do controller de Suporte, Tickets e Ouvidoria (BACK-015).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-015) & Segurança Anti-ID Spoofing e RBAC.
 */
@WebMvcTest(SuporteController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SuporteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResourceOwnershipService ownershipService;

    @MockBean
    private SuporteService suporteService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService blacklistService;

    @Test
    @DisplayName("[BACK-015] Deve abrir ticket de suporte com protocolo SUP-YYYY-XXXXX e retornar 201 Created")
    void deveAbrirTicketDeSuporte() throws Exception {
        CriarTicketDTO request = CriarTicketDTO.builder()
                .titulo("Dúvida sobre reembolso")
                .descricao("Como solicito o estorno de uma mentoria cancelada?")
                .categoria(CategoriaTicket.FINANCEIRO)
                .prioridade(PrioridadeTicket.MEDIA)
                .build();

        TicketSuporteDTO response = TicketSuporteDTO.builder()
                .id(101L)
                .usuarioId(10L)
                .usuarioNome("Ana Clara")
                .protocolo("SUP-2026-98124")
                .titulo("Dúvida sobre reembolso")
                .assunto("Dúvida sobre reembolso")
                .descricao("Como solicito o estorno de uma mentoria cancelada?")
                .categoria(CategoriaTicket.FINANCEIRO)
                .prioridade(PrioridadeTicket.MEDIA)
                .status(StatusTicket.ABERTO)
                .criadoEm(Instant.now())
                .build();

        when(suporteService.criarTicket(eq(10L), any(CriarTicketDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/suporte/tickets?usuarioId=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.protocolo").value("SUP-2026-98124"))
                .andExpect(jsonPath("$.data.status").value("ABERTO"))
                .andExpect(jsonPath("$.data.categoria").value("FINANCEIRO"))
                .andExpect(jsonPath("$.message").value("Ticket criado com sucesso! Protocolo: SUP-2026-98124"));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(10L);
        verify(suporteService, times(1)).criarTicket(eq(10L), any(CriarTicketDTO.class));
    }

    @Test
    @DisplayName("[BACK-015] Deve listar tickets do usuário autenticado (GET /tickets/meus)")
    void deveListarTicketsDoUsuario() throws Exception {
        TicketSuporteDTO t1 = TicketSuporteDTO.builder()
                .id(1L)
                .usuarioId(10L)
                .protocolo("SUP-2026-11223")
                .titulo("Problema com acesso à sala")
                .status(StatusTicket.RESOLVIDO)
                .respostaAdmin("Acesso reestabelecido pela equipe.")
                .build();

        when(suporteService.listarMeusTickets(10L)).thenReturn(List.of(t1));

        mockMvc.perform(get("/api/v1/suporte/tickets/meus?usuarioId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].protocolo").value("SUP-2026-11223"))
                .andExpect(jsonPath("$.data[0].status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.data[0].respostaAdmin").value("Acesso reestabelecido pela equipe."));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(10L);
        verify(suporteService, times(1)).listarMeusTickets(10L);
    }

    @Test
    @DisplayName("[BACK-015] Deve obter detalhes de um ticket por ID (GET /tickets/{id})")
    void deveObterDetalhesDoTicket() throws Exception {
        TicketSuporteDTO t1 = TicketSuporteDTO.builder()
                .id(50L)
                .usuarioId(10L)
                .protocolo("SUP-2026-55443")
                .titulo("Dúvida técnica")
                .status(StatusTicket.EM_ATENDIMENTO)
                .build();

        when(suporteService.obterTicket(eq(50L), eq(10L), anyBoolean())).thenReturn(t1);

        mockMvc.perform(get("/api/v1/suporte/tickets/50?usuarioId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(50))
                .andExpect(jsonPath("$.data.protocolo").value("SUP-2026-55443"));

        verify(suporteService, times(1)).obterTicket(eq(50L), eq(10L), anyBoolean());
    }

    @Test
    @DisplayName("[BACK-015] Deve bloquear com 403 Forbidden leitura de ticket de terceiro por usuário comum")
    void deveBloquearLeituraDeTicketAlheio() throws Exception {
        when(suporteService.obterTicket(eq(99L), eq(10L), anyBoolean()))
                .thenThrow(new AccessDeniedException("Acesso negado: Você não tem permissão para visualizar este chamado de suporte."));

        mockMvc.perform(get("/api/v1/suporte/tickets/99?usuarioId=10"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Acesso negado: Acesso negado: Você não tem permissão para visualizar este chamado de suporte."));

        verify(suporteService, times(1)).obterTicket(eq(99L), eq(10L), anyBoolean());
    }

    @Test
    @DisplayName("[BACK-015] Deve responder ticket com sucesso via PATCH (exclusivo para ADMIN)")
    void deveResponderTicket() throws Exception {
        ResponderTicketDTO request = ResponderTicketDTO.builder()
                .resposta("Sua solicitação foi processada com sucesso.")
                .novoStatus(StatusTicket.RESOLVIDO)
                .build();

        TicketSuporteDTO response = TicketSuporteDTO.builder()
                .id(101L)
                .protocolo("SUP-2026-98124")
                .status(StatusTicket.RESOLVIDO)
                .resposta("Sua solicitação foi processada com sucesso.")
                .respostaAdmin("Sua solicitação foi processada com sucesso.")
                .respondidoPorNome("Admin Geral")
                .build();

        when(suporteService.responderTicket(eq(101L), eq(1L), any(ResponderTicketDTO.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/suporte/tickets/101/responder?adminId=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVIDO"))
                .andExpect(jsonPath("$.data.resposta").value("Sua solicitação foi processada com sucesso."))
                .andExpect(jsonPath("$.message").value("Resposta enviada e ticket atualizado"));

        verify(suporteService, times(1)).responderTicket(eq(101L), eq(1L), any(ResponderTicketDTO.class));
    }
}
