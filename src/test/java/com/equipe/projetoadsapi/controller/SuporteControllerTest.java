package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.CriarDenunciaDTO;
import com.equipe.projetoadsapi.dto.CriarTicketDTO;
import com.equipe.projetoadsapi.dto.DenunciaDTO;
import com.equipe.projetoadsapi.dto.ModerarDenunciaDTO;
import com.equipe.projetoadsapi.dto.ResponderTicketDTO;
import com.equipe.projetoadsapi.dto.TicketSuporteDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.CategoriaTicket;
import com.equipe.projetoadsapi.model.PrioridadeTicket;
import com.equipe.projetoadsapi.model.StatusDenuncia;
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

    @Test
    @DisplayName("[BACK-016] Deve registrar denúncia ética com protocolo oficial DEN-YYYY-XXXXX e retornar 201 Created")
    void deveCriarDenunciaComProtocoloOficial() throws Exception {
        CriarDenunciaDTO request = CriarDenunciaDTO.builder()
                .denunciadoId(20L)
                .agendamentoId(101L)
                .motivo("CONDUTA_INAPROPRIADA")
                .descricao("O professor utilizou linguajar ofensivo durante a aula.")
                .build();

        DenunciaDTO response = DenunciaDTO.builder()
                .id(1L)
                .denuncianteId(10L)
                .denuncianteNome("Ana Clara")
                .denunciadoId(20L)
                .denunciadoNome("Prof. Lucas Rocha")
                .agendamentoId(101L)
                .protocolo("DEN-2026-88771")
                .motivo("CONDUTA_INAPROPRIADA")
                .descricao("O professor utilizou linguajar ofensivo durante a aula.")
                .status(StatusDenuncia.PENDENTE)
                .criadoEm(Instant.now())
                .build();

        when(suporteService.criarDenuncia(eq(10L), any(CriarDenunciaDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/suporte/denuncias?denuncianteId=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.protocolo").value("DEN-2026-88771"))
                .andExpect(jsonPath("$.data.status").value("PENDENTE"))
                .andExpect(jsonPath("$.message").value("Denúncia registrada com sucesso! Protocolo: DEN-2026-88771"));

        verify(ownershipService, times(1)).validarPropriedadeUsuario(10L);
        verify(suporteService, times(1)).criarDenuncia(eq(10L), any(CriarDenunciaDTO.class));
    }

    @Test
    @DisplayName("[BACK-016] Deve bloquear auto-denúncia retornando 422 Unprocessable Entity (RN-001)")
    void deveBloquearAutoDenuncia() throws Exception {
        CriarDenunciaDTO request = CriarDenunciaDTO.builder()
                .denunciadoId(10L)
                .motivo("FRAUDE")
                .descricao("Auto denúncia indevida")
                .build();

        when(suporteService.criarDenuncia(eq(10L), any(CriarDenunciaDTO.class)))
                .thenThrow(new RegraNegocioException("Não é permitido denunciar o próprio perfil."));

        mockMvc.perform(post("/api/v1/suporte/denuncias?denuncianteId=10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Violação de Regra de Negócio"))
                .andExpect(jsonPath("$.message").value("Não é permitido denunciar o próprio perfil."));

        verify(suporteService, times(1)).criarDenuncia(eq(10L), any(CriarDenunciaDTO.class));
    }

    @Test
    @DisplayName("[BACK-016] Deve listar fila de moderação de denúncias para moderadores/admin")
    void deveListarDenunciasParaAdmin() throws Exception {
        DenunciaDTO d1 = DenunciaDTO.builder()
                .id(1L)
                .protocolo("DEN-2026-88771")
                .status(StatusDenuncia.PENDENTE)
                .motivo("CONDUTA_INAPROPRIADA")
                .build();

        when(suporteService.listarDenuncias(null)).thenReturn(List.of(d1));

        mockMvc.perform(get("/api/v1/suporte/denuncias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].protocolo").value("DEN-2026-88771"))
                .andExpect(jsonPath("$.message").value("Fila de moderação de denúncias recuperada"));

        verify(suporteService, times(1)).listarDenuncias(null);
    }

    @Test
    @DisplayName("[BACK-016] Deve moderar denúncia com atribuição de parecer técnico e julgamento ético (PATCH /denuncias/{id}/moderar)")
    void deveModerarDenunciaComSucesso() throws Exception {
        ModerarDenunciaDTO request = ModerarDenunciaDTO.builder()
                .novoStatus(StatusDenuncia.PROCEDENTE)
                .parecerModerador("Denúncia confirmada após análise dos logs do chat e histórico da mentoria.")
                .build();

        DenunciaDTO response = DenunciaDTO.builder()
                .id(1L)
                .protocolo("DEN-2026-88771")
                .status(StatusDenuncia.PROCEDENTE)
                .parecerModerador("Denúncia confirmada após análise dos logs do chat e histórico da mentoria.")
                .build();

        when(suporteService.moderarDenuncia(eq(1L), eq(1L), any(ModerarDenunciaDTO.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/suporte/denuncias/1/moderar?adminId=1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCEDENTE"))
                .andExpect(jsonPath("$.data.parecerModerador").value("Denúncia confirmada após análise dos logs do chat e histórico da mentoria."))
                .andExpect(jsonPath("$.message").value("Denúncia moderada com sucesso! Status: PROCEDENTE"));

        verify(suporteService, times(1)).moderarDenuncia(eq(1L), eq(1L), any(ModerarDenunciaDTO.class));
    }
}
