package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ResumoFinanceiroDTO;
import com.equipe.projetoadsapi.dto.TransacaoFinanceiraDTO;
import com.equipe.projetoadsapi.model.enums.TipoChavePix;
import com.equipe.projetoadsapi.model.enums.TipoTransacao;
import com.equipe.projetoadsapi.security.CustomUserDetailsService;
import com.equipe.projetoadsapi.security.JwtTokenProvider;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.TokenBlacklistService;
import com.equipe.projetoadsapi.service.FinanceiroService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes automatizados do controller financeiro e extratos docentes (BACK-011).
 * Rastreabilidade: Fase 7 - Bloco J (BACK-011) & Requisito RN-001 (RBAC e Anti-ID Spoofing).
 */
@WebMvcTest(FinanceiroController.class)
@AutoConfigureMockMvc(addFilters = false)
public class FinanceiroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ResourceOwnershipService ownershipService;

    @MockBean
    private FinanceiroService financeiroService;

    @MockBean
    private JwtTokenProvider tokenProvider;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private TokenBlacklistService blacklistService;

    @Test
    @DisplayName("Deve retornar 200 OK com ResumoFinanceiroDTO para o professor autenticado dono da conta")
    void deveRetornarResumoFinanceiroDocente() throws Exception {
        ResumoFinanceiroDTO resumo = ResumoFinanceiroDTO.builder()
                .saldoDisponivel(new BigDecimal("1250.00"))
                .saldoPendente(new BigDecimal("320.00"))
                .totalGanhosBrutos(new BigDecimal("1570.00"))
                .totalTaxasRetidas(new BigDecimal("157.00"))
                .totalSacado(new BigDecimal("450.00"))
                .totalSacadoNoMes(new BigDecimal("450.00"))
                .quantidadeAulasRealizadas(14)
                .chavePixCadastrada("carlos@orienta.com.br")
                .tipoChavePixCadastrada(TipoChavePix.EMAIL)
                .saqueBloqueado(false)
                .build();

        when(financeiroService.obterResumoFinanceiro(10L)).thenReturn(resumo);

        mockMvc.perform(get("/api/v1/professores/10/financeiro/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.saldoDisponivel").value(1250.00))
                .andExpect(jsonPath("$.data.saldoPendente").value(320.00))
                .andExpect(jsonPath("$.data.totalGanhosBrutos").value(1570.00))
                .andExpect(jsonPath("$.data.totalTaxasRetidas").value(157.00))
                .andExpect(jsonPath("$.data.totalSacado").value(450.00))
                .andExpect(jsonPath("$.data.quantidadeAulasRealizadas").value(14))
                .andExpect(jsonPath("$.data.chavePixCadastrada").value("carlos@orienta.com.br"))
                .andExpect(jsonPath("$.data.tipoChavePixCadastrada").value("EMAIL"))
                .andExpect(jsonPath("$.data.saqueBloqueado").value(false));

        verify(ownershipService, times(1)).validarAcessoDocente(10L);
    }

    @Test
    @DisplayName("Deve retornar 200 OK com o extrato cronológico de transações")
    void deveRetornarExtratoTransacoesDocente() throws Exception {
        TransacaoFinanceiraDTO t1 = TransacaoFinanceiraDTO.builder()
                .id(1L)
                .usuarioId(10L)
                .tipoTransacao(TipoTransacao.CREDITO_AULA)
                .valor(new BigDecimal("100.00"))
                .descricao("Crédito referente à mentoria concluída #101")
                .dataTransacao(Instant.now())
                .build();

        TransacaoFinanceiraDTO t2 = TransacaoFinanceiraDTO.builder()
                .id(2L)
                .usuarioId(10L)
                .tipoTransacao(TipoTransacao.RETENCAO_TAXA_INSTITUICAO)
                .valor(new BigDecimal("10.00"))
                .descricao("Taxa de intermediação institucional (10%) - Mentoria #101")
                .dataTransacao(Instant.now())
                .build();

        when(financeiroService.obterExtrato(10L)).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/v1/professores/10/financeiro/extrato"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].tipoTransacao").value("CREDITO_AULA"))
                .andExpect(jsonPath("$.data[0].valor").value(100.00))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].tipoTransacao").value("RETENCAO_TAXA_INSTITUICAO"))
                .andExpect(jsonPath("$.data[1].valor").value(10.00));

        verify(ownershipService, times(1)).validarAcessoDocente(10L);
    }

    @Test
    @DisplayName("Deve bloquear com 403 Forbidden para tentativa de ID Spoofing ou acesso por perfil de Aluno")
    void deveBloquearAcessoIndevidoAoResumoFinanceiro() throws Exception {
        doThrow(new AccessDeniedException("Acesso Proibido (ID Spoofing): Você não tem permissão para acessar ou alterar dados de outro usuário."))
                .when(ownershipService).validarAcessoDocente(99L);

        mockMvc.perform(get("/api/v1/professores/99/financeiro/resumo"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Acesso negado: Acesso Proibido (ID Spoofing): Você não tem permissão para acessar ou alterar dados de outro usuário."));

        verify(ownershipService, times(1)).validarAcessoDocente(99L);
        verify(financeiroService, never()).obterResumoFinanceiro(any());
    }

    @Test
    @DisplayName("Deve bloquear com 403 Forbidden para tentativa de ID Spoofing no extrato financeiro")
    void deveBloquearAcessoIndevidoAoExtratoFinanceiro() throws Exception {
        doThrow(new AccessDeniedException("Acesso Proibido (RN-001): Este recurso é restrito exclusivamente ao perfil docente/professor."))
                .when(ownershipService).validarAcessoDocente(99L);

        mockMvc.perform(get("/api/v1/professores/99/financeiro/extrato"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Acesso negado: Acesso Proibido (RN-001): Este recurso é restrito exclusivamente ao perfil docente/professor."));

        verify(ownershipService, times(1)).validarAcessoDocente(99L);
        verify(financeiroService, never()).obterExtrato(any());
    }
}
