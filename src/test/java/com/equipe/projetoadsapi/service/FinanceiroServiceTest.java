package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.ResumoFinanceiroDTO;
import com.equipe.projetoadsapi.dto.TransacaoFinanceiraDTO;
import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.PapelUsuario;
import com.equipe.projetoadsapi.model.Saque;
import com.equipe.projetoadsapi.model.StatusAgendamento;
import com.equipe.projetoadsapi.model.TransacaoFinanceira;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.model.enums.StatusSaque;
import com.equipe.projetoadsapi.model.enums.TipoChavePix;
import com.equipe.projetoadsapi.model.enums.TipoTransacao;
import com.equipe.projetoadsapi.repository.AgendamentoRepository;
import com.equipe.projetoadsapi.repository.SaqueRepository;
import com.equipe.projetoadsapi.repository.TransacaoFinanceiraRepository;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do serviço financeiro, cálculo atômico de saldos e taxa de 10% (BACK-010).
 */
@ExtendWith(MockitoExtension.class)
public class FinanceiroServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private SaqueRepository saqueRepository;

    @Mock
    private TransacaoFinanceiraRepository transacaoRepository;

    @InjectMocks
    private FinanceiroService financeiroService;

    private Usuario professor;
    private Usuario aluno;
    private Agendamento aulaRealizada1;
    private Agendamento aulaRealizada2;
    private Agendamento aulaConfirmada;

    @BeforeEach
    void setUp() {
        professor = Usuario.builder()
                .id(10L)
                .nome("Prof. Carlos Magno")
                .email("carlos@orienta.com.br")
                .cpf("123.456.789-00")
                .papel(PapelUsuario.ROLE_PROFESSOR)
                .build();

        aluno = Usuario.builder()
                .id(20L)
                .nome("Lucas Aluno")
                .email("lucas@orienta.com.br")
                .cpf("987.654.321-11")
                .papel(PapelUsuario.ROLE_ALUNO)
                .build();

        aulaRealizada1 = Agendamento.builder()
                .id(101L)
                .professor(professor)
                .aluno(aluno)
                .data(LocalDate.now().minusDays(5))
                .hora(LocalTime.of(14, 0))
                .status(StatusAgendamento.REALIZADA)
                .valorAplicado(new BigDecimal("100.00"))
                .assunto("Física Quântica")
                .build();

        aulaRealizada2 = Agendamento.builder()
                .id(102L)
                .professor(professor)
                .aluno(aluno)
                .data(LocalDate.now().minusDays(2))
                .hora(LocalTime.of(16, 0))
                .status(StatusAgendamento.REALIZADA)
                .valorAplicado(new BigDecimal("150.00"))
                .assunto("Eletromagnetismo")
                .build();

        aulaConfirmada = Agendamento.builder()
                .id(103L)
                .professor(professor)
                .aluno(aluno)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(10, 0))
                .status(StatusAgendamento.CONFIRMADO)
                .valorAplicado(new BigDecimal("200.00"))
                .assunto("Mecânica Clássica")
                .build();
    }

    @Test
    @DisplayName("Deve calcular saldo disponível com dedução exata de 10% da taxa institucional")
    void deveCalcularSaldoDisponivelComDeducaoDeTaxa() {
        // Aula 1: R$ 100,00 * 0.90 = R$ 90,00
        // Aula 2: R$ 150,00 * 0.90 = R$ 135,00
        // Total Líquido Esperado: R$ 225,00
        when(agendamentoRepository.findByProfessorIdAndStatus(10L, StatusAgendamento.REALIZADA))
                .thenReturn(Arrays.asList(aulaRealizada1, aulaRealizada2));
        when(saqueRepository.findByProfessorIdOrderByDataSolicitacaoDesc(10L))
                .thenReturn(Collections.emptyList());

        BigDecimal saldoDisponivel = financeiroService.calcularSaldoDisponivel(10L);

        assertEquals(new BigDecimal("225.00"), saldoDisponivel);
    }

    @Test
    @DisplayName("Deve subtrair saques pendentes, em processamento e concluídos do saldo disponível, ignorando recusados")
    void deveSubtrairSaquesComprometidosDoSaldoDisponivel() {
        when(agendamentoRepository.findByProfessorIdAndStatus(10L, StatusAgendamento.REALIZADA))
                .thenReturn(Arrays.asList(aulaRealizada1, aulaRealizada2)); // Líquido = R$ 225,00

        Saque saqueConcluido = Saque.builder()
                .id(1L)
                .valor(new BigDecimal("50.00"))
                .statusSaque(StatusSaque.CONCLUIDO)
                .build();

        Saque saquePendente = Saque.builder()
                .id(2L)
                .valor(new BigDecimal("30.00"))
                .statusSaque(StatusSaque.PENDENTE)
                .build();

        Saque saqueRecusado = Saque.builder()
                .id(3L)
                .valor(new BigDecimal("80.00"))
                .statusSaque(StatusSaque.RECUSADO)
                .build();

        when(saqueRepository.findByProfessorIdOrderByDataSolicitacaoDesc(10L))
                .thenReturn(Arrays.asList(saqueConcluido, saquePendente, saqueRecusado));

        // R$ 225,00 - (R$ 50,00 + R$ 30,00) = R$ 145,00 (saque recusado não é deduzido)
        BigDecimal saldoDisponivel = financeiroService.calcularSaldoDisponivel(10L);

        assertEquals(new BigDecimal("145.00"), saldoDisponivel);
    }

    @Test
    @DisplayName("Deve calcular saldo pendente de aulas confirmadas/agendadas deduzindo 10%")
    void deveCalcularSaldoPendente() {
        // Aula confirmada: R$ 200,00 * 0.90 = R$ 180,00
        when(agendamentoRepository.findByProfessorId(10L))
                .thenReturn(Arrays.asList(aulaRealizada1, aulaConfirmada));

        BigDecimal pendente = financeiroService.calcularSaldoPendente(10L);

        assertEquals(new BigDecimal("180.00"), pendente);
    }

    @Test
    @DisplayName("Deve registrar lançamentos contábeis atômicos imutáveis ao concluir mentoria")
    void deveRegistrarTransacoesConclusaoAula() {
        when(transacaoRepository.findByAgendamentoId(101L)).thenReturn(Optional.empty());

        financeiroService.registrarTransacoesConclusaoAula(aulaRealizada1);

        // Deve persistir 2 transações: 1 de crédito bruto (R$ 100,00) e 1 de retenção de 10% (R$ 10,00)
        ArgumentCaptor<TransacaoFinanceira> captor = ArgumentCaptor.forClass(TransacaoFinanceira.class);
        verify(transacaoRepository, times(2)).save(captor.capture());

        List<TransacaoFinanceira> gravadas = captor.getAllValues();
        TransacaoFinanceira credito = gravadas.get(0);
        TransacaoFinanceira retencao = gravadas.get(1);

        assertEquals(TipoTransacao.CREDITO_AULA, credito.getTipoTransacao());
        assertEquals(new BigDecimal("100.00"), credito.getValor());
        assertEquals(10L, credito.getUsuario().getId());

        assertEquals(TipoTransacao.RETENCAO_TAXA_INSTITUICAO, retencao.getTipoTransacao());
        assertEquals(new BigDecimal("10.00"), retencao.getValor());
        assertEquals(10L, retencao.getUsuario().getId());
    }

    @Test
    @DisplayName("Não deve duplicar lançamentos contábeis se mentoria já tiver sido registrada (idempotência)")
    void deveGarantirIdempotenciaNoRegistroDeTransacoes() {
        TransacaoFinanceira existente = TransacaoFinanceira.builder().id(999L).build();
        when(transacaoRepository.findByAgendamentoId(101L)).thenReturn(Optional.of(existente));

        financeiroService.registrarTransacoesConclusaoAula(aulaRealizada1);

        verify(transacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve registrar lançamento de débito ao solicitar saque")
    void deveRegistrarTransacaoDeSaque() {
        Saque saque = Saque.builder()
                .id(50L)
                .professor(professor)
                .valor(new BigDecimal("100.00"))
                .tipoChavePix(TipoChavePix.CPF)
                .protocoloSaque("SAQ-2026-X9999")
                .build();

        financeiroService.registrarTransacaoSolicitacaoSaque(saque);

        ArgumentCaptor<TransacaoFinanceira> captor = ArgumentCaptor.forClass(TransacaoFinanceira.class);
        verify(transacaoRepository, times(1)).save(captor.capture());

        TransacaoFinanceira gravada = captor.getValue();
        assertEquals(TipoTransacao.SAQUE_DEBITO, gravada.getTipoTransacao());
        assertEquals(new BigDecimal("100.00"), gravada.getValor());
        assertEquals(50L, gravada.getSaque().getId());
    }

    @Test
    @DisplayName("Deve consolidar ResumoFinanceiroDTO completo com saldos, taxas e chave PIX")
    void deveConsolidarResumoFinanceiroDTOCompleto() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(professor));
        when(agendamentoRepository.findByProfessorIdAndStatus(10L, StatusAgendamento.REALIZADA))
                .thenReturn(Arrays.asList(aulaRealizada1, aulaRealizada2)); // Bruto: R$ 250, Líquido: R$ 225
        when(agendamentoRepository.findByProfessorId(10L))
                .thenReturn(Arrays.asList(aulaRealizada1, aulaRealizada2, aulaConfirmada)); // Pendente: R$ 180
        when(agendamentoRepository.countByProfessorIdAndStatus(10L, StatusAgendamento.REALIZADA))
                .thenReturn(2L);
        when(saqueRepository.somarValorPorProfessorEStatus(10L, StatusSaque.CONCLUIDO))
                .thenReturn(new BigDecimal("50.00"));
        when(saqueRepository.findByProfessorIdAndStatusSaque(10L, StatusSaque.CONCLUIDO))
                .thenReturn(Collections.emptyList());

        Saque saqueRealizado = Saque.builder()
                .id(1L)
                .valor(new BigDecimal("50.00"))
                .statusSaque(StatusSaque.CONCLUIDO)
                .chavePix("carlos@orienta.com.br")
                .tipoChavePix(TipoChavePix.EMAIL)
                .dataSolicitacao(Instant.now())
                .build();
        when(saqueRepository.findByProfessorIdOrderByDataSolicitacaoDesc(10L))
                .thenReturn(Collections.singletonList(saqueRealizado));

        when(saqueRepository.existsByProfessorIdAndStatusSaqueIn(eq(10L), any()))
                .thenReturn(false);

        ResumoFinanceiroDTO resumo = financeiroService.obterResumoFinanceiro(10L);

        assertNotNull(resumo);
        assertEquals(new BigDecimal("250.00"), resumo.getTotalGanhosBrutos());
        assertEquals(new BigDecimal("25.00"), resumo.getTotalTaxasRetidas()); // 10% de R$ 250
        assertEquals(new BigDecimal("175.00"), resumo.getSaldoDisponivel()); // R$ 225 líquido - R$ 50 sacado
        assertEquals(new BigDecimal("180.00"), resumo.getSaldoPendente());
        assertEquals(new BigDecimal("50.00"), resumo.getTotalSacado());
        assertEquals(2, resumo.getQuantidadeAulasRealizadas());
        assertEquals("carlos@orienta.com.br", resumo.getChavePixCadastrada());
        assertEquals(TipoChavePix.EMAIL, resumo.getTipoChavePixCadastrada());
        assertFalse(resumo.getSaqueBloqueado());
    }

    @Test
    @DisplayName("Deve retornar extrato ordenado das transações do usuário")
    void deveObterExtratoDoUsuario() {
        when(usuarioRepository.existsById(10L)).thenReturn(true);

        TransacaoFinanceira tf1 = TransacaoFinanceira.builder()
                .id(1L)
                .usuario(professor)
                .tipoTransacao(TipoTransacao.CREDITO_AULA)
                .valor(new BigDecimal("100.00"))
                .dataTransacao(Instant.now().minusSeconds(3600))
                .descricao("Crédito aula #101")
                .build();

        TransacaoFinanceira tf2 = TransacaoFinanceira.builder()
                .id(2L)
                .usuario(professor)
                .tipoTransacao(TipoTransacao.RETENCAO_TAXA_INSTITUICAO)
                .valor(new BigDecimal("10.00"))
                .dataTransacao(Instant.now().minusSeconds(3500))
                .descricao("Taxa 10% aula #101")
                .build();

        when(transacaoRepository.findByUsuarioIdOrderByDataTransacaoDesc(10L))
                .thenReturn(Arrays.asList(tf2, tf1));

        List<TransacaoFinanceiraDTO> extrato = financeiroService.obterExtrato(10L);

        assertEquals(2, extrato.size());
        assertEquals(2L, extrato.get(0).getId());
        assertEquals(TipoTransacao.RETENCAO_TAXA_INSTITUICAO, extrato.get(0).getTipoTransacao());
        assertEquals(1L, extrato.get(1).getId());
        assertEquals(TipoTransacao.CREDITO_AULA, extrato.get(1).getTipoTransacao());
    }
}
