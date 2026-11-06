package com.equipe.projetoadsapi.model;

import com.equipe.projetoadsapi.dto.ResumoFinanceiroDTO;
import com.equipe.projetoadsapi.dto.SaqueRequestDTO;
import com.equipe.projetoadsapi.dto.SaqueResponseDTO;
import com.equipe.projetoadsapi.dto.TransacaoFinanceiraDTO;
import com.equipe.projetoadsapi.model.enums.StatusSaque;
import com.equipe.projetoadsapi.model.enums.TipoChavePix;
import com.equipe.projetoadsapi.model.enums.TipoTransacao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Year;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários do modelo de dados e ciclo de vida do módulo financeiro (BACK-009).
 */
public class FinanceiroModelTest {

    @Test
    @DisplayName("PrePersist do Saque deve gerar protocolo com padrão SAQ-YYYY-XXXXX e calcular valor líquido")
    void deveGerarProtocoloECalcularValorLiquidoNoPrePersist() {
        Usuario professor = Usuario.builder().id(10L).nome("Prof. Carlos").build();

        Saque saque = Saque.builder()
                .professor(professor)
                .valor(new BigDecimal("200.00"))
                .taxaAplicada(new BigDecimal("20.00")) // 10%
                .tipoChavePix(TipoChavePix.EMAIL)
                .chavePix("carlos@orienta.com.br")
                .build();

        saque.prePersist();

        assertNotNull(saque.getProtocoloSaque());
        assertTrue(saque.getProtocoloSaque().startsWith("SAQ-" + Year.now().getValue() + "-"));
        assertEquals(StatusSaque.PENDENTE, saque.getStatusSaque());
        assertEquals(new BigDecimal("180.00"), saque.getValorLiquido());
        assertNotNull(saque.getDataSolicitacao());
    }

    @Test
    @DisplayName("PrePersist de TransacaoFinanceira deve definir timestamp atual se nulo")
    void deveDefinirTimestampNaTransacaoFinanceira() {
        Usuario usuario = Usuario.builder().id(5L).nome("Aluno Lucas").build();

        TransacaoFinanceira tf = TransacaoFinanceira.builder()
                .usuario(usuario)
                .tipoTransacao(TipoTransacao.CREDITO_AULA)
                .valor(new BigDecimal("150.00"))
                .descricao("Crédito referente à mentoria de Física")
                .build();

        tf.prePersist();

        assertNotNull(tf.getDataTransacao());
        assertEquals(TipoTransacao.CREDITO_AULA, tf.getTipoTransacao());
    }

    @Test
    @DisplayName("Conversão DTO deve mapear corretamente entidade Saque para SaqueResponseDTO")
    void deveMapearSaqueParaDTO() {
        Usuario professor = Usuario.builder().id(7L).nome("Professora Helena").build();

        Saque saque = Saque.builder()
                .id(1L)
                .professor(professor)
                .valor(new BigDecimal("500.00"))
                .taxaAplicada(new BigDecimal("50.00"))
                .valorLiquido(new BigDecimal("450.00"))
                .statusSaque(StatusSaque.PROCESSANDO)
                .tipoChavePix(TipoChavePix.CPF)
                .chavePix("123.456.789-00")
                .protocoloSaque("SAQ-2026-ABC12")
                .dataSolicitacao(Instant.now())
                .observacoes("Saque prioritário")
                .build();

        SaqueResponseDTO dto = SaqueResponseDTO.fromEntity(saque);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(7L, dto.getProfessorId());
        assertEquals("Professora Helena", dto.getNomeProfessor());
        assertEquals(new BigDecimal("500.00"), dto.getValor());
        assertEquals(new BigDecimal("450.00"), dto.getValorLiquido());
        assertEquals(StatusSaque.PROCESSANDO, dto.getStatusSaque());
        assertEquals(TipoChavePix.CPF, dto.getTipoChavePix());
        assertEquals("SAQ-2026-ABC12", dto.getProtocoloSaque());
    }

    @Test
    @DisplayName("Conversão DTO deve mapear corretamente TransacaoFinanceira para TransacaoFinanceiraDTO")
    void deveMapearTransacaoFinanceiraParaDTO() {
        Usuario usuario = Usuario.builder().id(3L).build();
        Agendamento agendamento = Agendamento.builder().id(99L).build();

        TransacaoFinanceira tf = TransacaoFinanceira.builder()
                .id(42L)
                .usuario(usuario)
                .agendamento(agendamento)
                .tipoTransacao(TipoTransacao.RETENCAO_TAXA_INSTITUICAO)
                .valor(new BigDecimal("15.00"))
                .descricao("Retenção de 10% da plataforma")
                .dataTransacao(Instant.now())
                .build();

        TransacaoFinanceiraDTO dto = TransacaoFinanceiraDTO.fromEntity(tf);

        assertNotNull(dto);
        assertEquals(42L, dto.getId());
        assertEquals(3L, dto.getUsuarioId());
        assertEquals(99L, dto.getAgendamentoId());
        assertNull(dto.getSaqueId());
        assertEquals(TipoTransacao.RETENCAO_TAXA_INSTITUICAO, dto.getTipoTransacao());
        assertEquals(new BigDecimal("15.00"), dto.getValor());
    }

    @Test
    @DisplayName("ResumoFinanceiroDTO deve inicializar com valores default zerados e não-nulos")
    void deveInicializarResumoFinanceiroDefault() {
        ResumoFinanceiroDTO resumo = ResumoFinanceiroDTO.builder().build();

        assertEquals(BigDecimal.ZERO, resumo.getSaldoDisponivel());
        assertEquals(BigDecimal.ZERO, resumo.getSaldoPendente());
        assertEquals(BigDecimal.ZERO, resumo.getTotalGanhosBrutos());
        assertEquals(BigDecimal.ZERO, resumo.getTotalTaxasRetidas());
        assertEquals(BigDecimal.ZERO, resumo.getTotalSacado());
        assertFalse(resumo.getSaqueBloqueado());
        assertNotNull(resumo.getTransacoesRecentes());
        assertNotNull(resumo.getSaquesRecentes());
    }
}
