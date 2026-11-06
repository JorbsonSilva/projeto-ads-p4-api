package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.ResumoFinanceiroDTO;
import com.equipe.projetoadsapi.dto.SaqueResponseDTO;
import com.equipe.projetoadsapi.dto.TransacaoFinanceiraDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.Agendamento;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela gestão contábil, cálculo atômico de saldos,
 * retenção de taxa institucional (10%) e extratos financeiros do Orienta.
 * Rastreabilidade: Fase 7 - Bloco J (BACK-010)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceiroService {

    public static final BigDecimal TAXA_RETENCAO_INSTITUCIONAL = new BigDecimal("0.10"); // 10% de taxa da plataforma
    public static final BigDecimal PERCENTUAL_LIQUIDO_DOCENTE = new BigDecimal("0.90"); // 90% repassado ao professor
    public static final BigDecimal VALOR_MINIMO_SAQUE = new BigDecimal("10.00"); // R$ 10,00 mínimo para saque

    private final UsuarioRepository usuarioRepository;
    private final AgendamentoRepository agendamentoRepository;
    private final SaqueRepository saqueRepository;
    private final TransacaoFinanceiraRepository transacaoRepository;

    /**
     * Consolida a posição contábil e financeira completa do docente.
     * Rastreabilidade: f7-back-010
     */
    @Transactional(readOnly = true)
    public ResumoFinanceiroDTO obterResumoFinanceiro(Long professorId) {
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado: " + professorId));

        BigDecimal totalGanhosBrutos = calcularTotalGanhosBrutos(professorId);
        BigDecimal totalTaxasRetidas = calcularTotalTaxasRetidas(professorId);
        BigDecimal saldoDisponivel = calcularSaldoDisponivel(professorId);
        BigDecimal saldoPendente = calcularSaldoPendente(professorId);
        BigDecimal totalSacado = calcularTotalSacado(professorId);
        BigDecimal totalSacadoNoMes = calcularTotalSacadoNoMes(professorId);

        Long qtdRealizadas = agendamentoRepository.countByProfessorIdAndStatus(professorId, StatusAgendamento.REALIZADA);

        // Identificação da chave PIX cadastrada ou sugerida
        List<Saque> saques = saqueRepository.findByProfessorIdOrderByDataSolicitacaoDesc(professorId);
        String chavePix = null;
        TipoChavePix tipoChavePix = null;

        if (!saques.isEmpty()) {
            Saque ultimo = saques.get(0);
            chavePix = ultimo.getChavePix();
            tipoChavePix = ultimo.getTipoChavePix();
        } else if (professor.getCpf() != null && !professor.getCpf().trim().isEmpty()) {
            chavePix = professor.getCpf();
            tipoChavePix = TipoChavePix.CPF;
        } else if (professor.getEmail() != null) {
            chavePix = professor.getEmail();
            tipoChavePix = TipoChavePix.EMAIL;
        }

        // Validação de bloqueio de saque
        boolean saqueBloqueado = false;
        String motivoBloqueio = null;

        boolean temSaqueEmProcessamento = saqueRepository.existsByProfessorIdAndStatusSaqueIn(
                professorId, Arrays.asList(StatusSaque.PENDENTE, StatusSaque.PROCESSANDO));

        if (temSaqueEmProcessamento) {
            saqueBloqueado = true;
            motivoBloqueio = "Você já possui uma solicitação de saque em processamento.";
        } else if (saldoDisponivel.compareTo(VALOR_MINIMO_SAQUE) < 0) {
            saqueBloqueado = true;
            motivoBloqueio = "Saldo mínimo para solicitação de saque é de R$ " + VALOR_MINIMO_SAQUE.setScale(2) + ".";
        }

        List<TransacaoFinanceiraDTO> transacoesRecentes = transacaoRepository
                .findByUsuarioIdOrderByDataTransacaoDesc(professorId)
                .stream()
                .limit(10)
                .map(TransacaoFinanceiraDTO::fromEntity)
                .collect(Collectors.toList());

        List<SaqueResponseDTO> saquesRecentes = saques.stream()
                .limit(5)
                .map(SaqueResponseDTO::fromEntity)
                .collect(Collectors.toList());

        return ResumoFinanceiroDTO.builder()
                .saldoDisponivel(saldoDisponivel)
                .saldoPendente(saldoPendente)
                .totalGanhosBrutos(totalGanhosBrutos)
                .totalTaxasRetidas(totalTaxasRetidas)
                .totalSacado(totalSacado)
                .totalSacadoNoMes(totalSacadoNoMes)
                .quantidadeAulasRealizadas(qtdRealizadas != null ? qtdRealizadas.intValue() : 0)
                .chavePixCadastrada(chavePix)
                .tipoChavePixCadastrada(tipoChavePix)
                .saqueBloqueado(saqueBloqueado)
                .motivoBloqueio(motivoBloqueio)
                .transacoesRecentes(transacoesRecentes)
                .saquesRecentes(saquesRecentes)
                .build();
    }

    /**
     * Calcula o saldo líquido disponível do professor para resgate.
     * Saldo Disponível = (Ganhos Líquidos de Aulas Realizadas) - (Saques Solicitados / Processando / Concluídos)
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularSaldoDisponivel(Long professorId) {
        List<Agendamento> realizadas = agendamentoRepository.findByProfessorIdAndStatus(professorId, StatusAgendamento.REALIZADA);

        BigDecimal totalLiquidoAulas = BigDecimal.ZERO;
        for (Agendamento aula : realizadas) {
            BigDecimal valor = aula.getValorAplicado() != null ? aula.getValorAplicado() : BigDecimal.ZERO;
            BigDecimal liquido = valor.multiply(PERCENTUAL_LIQUIDO_DOCENTE).setScale(2, RoundingMode.HALF_UP);
            totalLiquidoAulas = totalLiquidoAulas.add(liquido);
        }

        // Subtrai saques já solicitados (PENDENTE, PROCESSANDO ou CONCLUIDO)
        // Saques RECUSADOS não consomem saldo disponível
        List<Saque> saques = saqueRepository.findByProfessorIdOrderByDataSolicitacaoDesc(professorId);
        BigDecimal totalSaquesComprometidos = BigDecimal.ZERO;
        for (Saque s : saques) {
            if (s.getStatusSaque() == StatusSaque.PENDENTE ||
                s.getStatusSaque() == StatusSaque.PROCESSANDO ||
                s.getStatusSaque() == StatusSaque.CONCLUIDO) {
                totalSaquesComprometidos = totalSaquesComprometidos.add(s.getValor());
            }
        }

        BigDecimal disponivel = totalLiquidoAulas.subtract(totalSaquesComprometidos);
        return disponivel.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(2) : disponivel.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o saldo pendente (mentorias agendadas/confirmadas que aguardam realização para liquidação).
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularSaldoPendente(Long professorId) {
        List<Agendamento> agendamentos = agendamentoRepository.findByProfessorId(professorId);

        BigDecimal totalPendente = BigDecimal.ZERO;
        for (Agendamento a : agendamentos) {
            if (a.getStatus() == StatusAgendamento.PENDENTE ||
                a.getStatus() == StatusAgendamento.CONFIRMADO ||
                a.getStatus() == StatusAgendamento.EM_ANDAMENTO ||
                a.getStatus() == StatusAgendamento.AGUARDANDO_CONFIRMACAO) {
                BigDecimal valor = a.getValorAplicado() != null ? a.getValorAplicado() : BigDecimal.ZERO;
                BigDecimal liquido = valor.multiply(PERCENTUAL_LIQUIDO_DOCENTE).setScale(2, RoundingMode.HALF_UP);
                totalPendente = totalPendente.add(liquido);
            }
        }
        return totalPendente.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total bruto faturado em mentorias concluídas.
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalGanhosBrutos(Long professorId) {
        List<Agendamento> realizadas = agendamentoRepository.findByProfessorIdAndStatus(professorId, StatusAgendamento.REALIZADA);
        BigDecimal total = BigDecimal.ZERO;
        for (Agendamento a : realizadas) {
            BigDecimal valor = a.getValorAplicado() != null ? a.getValorAplicado() : BigDecimal.ZERO;
            total = total.add(valor);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total de taxas institucionais retidas (10% sobre o faturamento bruto).
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalTaxasRetidas(Long professorId) {
        BigDecimal bruto = calcularTotalGanhosBrutos(professorId);
        return bruto.multiply(TAXA_RETENCAO_INSTITUCIONAL).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total histórico efetivamente sacado pelo professor (status CONCLUIDO).
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalSacado(Long professorId) {
        return saqueRepository.somarValorPorProfessorEStatus(professorId, StatusSaque.CONCLUIDO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula o total sacado no mês e ano correntes.
     */
    @Transactional(readOnly = true)
    public BigDecimal calcularTotalSacadoNoMes(Long professorId) {
        List<Saque> saques = saqueRepository.findByProfessorIdAndStatusSaque(professorId, StatusSaque.CONCLUIDO);
        YearMonth mesAtual = YearMonth.now();

        BigDecimal totalMes = BigDecimal.ZERO;
        for (Saque s : saques) {
            Instant data = s.getDataProcessamento() != null ? s.getDataProcessamento() : s.getDataSolicitacao();
            if (data != null) {
                YearMonth mesSaque = YearMonth.from(data.atZone(ZoneId.systemDefault()));
                if (mesAtual.equals(mesSaque)) {
                    totalMes = totalMes.add(s.getValor());
                }
            }
        }
        return totalMes.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Retorna o extrato cronológico completo de lançamentos de um usuário.
     */
    @Transactional(readOnly = true)
    public List<TransacaoFinanceiraDTO> obterExtrato(Long usuarioId) {
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + usuarioId);
        }
        return transacaoRepository.findByUsuarioIdOrderByDataTransacaoDesc(usuarioId)
                .stream()
                .map(TransacaoFinanceiraDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Registra os lançamentos contábeis atômicos e imutáveis da conclusão de uma mentoria:
     * 1. Crédito bruto da aula para o docente (CREDITO_AULA)
     * 2. Retenção institucional da taxa de 10% (RETENCAO_TAXA_INSTITUICAO)
     */
    @Transactional
    public void registrarTransacoesConclusaoAula(Agendamento agendamento) {
        if (agendamento == null || agendamento.getId() == null) {
            throw new RegraNegocioException("Agendamento inválido para geração de transações contábeis.");
        }

        // Idempotência: verifica se já existem transações registradas para este agendamento
        if (transacaoRepository.findByAgendamentoId(agendamento.getId()).isPresent()) {
            log.info("Lançamentos contábeis já registrados anteriormente para agendamento #{}", agendamento.getId());
            return;
        }

        BigDecimal valorBruto = agendamento.getValorAplicado() != null ? agendamento.getValorAplicado() : BigDecimal.ZERO;
        BigDecimal taxaRetencao = valorBruto.multiply(TAXA_RETENCAO_INSTITUCIONAL).setScale(2, RoundingMode.HALF_UP);

        Usuario professor = agendamento.getProfessor();

        // 1. Lançamento de Crédito Bruto
        TransacaoFinanceira credito = TransacaoFinanceira.builder()
                .usuario(professor)
                .agendamento(agendamento)
                .tipoTransacao(TipoTransacao.CREDITO_AULA)
                .valor(valorBruto)
                .descricao(String.format("Crédito referente à mentoria concluída #%d (%s)",
                        agendamento.getId(),
                        agendamento.getAssunto() != null ? agendamento.getAssunto() : "Aula particular"))
                .dataTransacao(Instant.now())
                .build();
        transacaoRepository.save(credito);

        // 2. Lançamento de Retenção da Taxa Institucional (10%)
        if (taxaRetencao.compareTo(BigDecimal.ZERO) > 0) {
            TransacaoFinanceira retencao = TransacaoFinanceira.builder()
                    .usuario(professor)
                    .agendamento(agendamento)
                    .tipoTransacao(TipoTransacao.RETENCAO_TAXA_INSTITUICAO)
                    .valor(taxaRetencao)
                    .descricao(String.format("Taxa de intermediação institucional (10%%) - Mentoria #%d", agendamento.getId()))
                    .dataTransacao(Instant.now())
                    .build();
            transacaoRepository.save(retencao);
        }

        log.info("Transações contábeis registradas com sucesso para agendamento #{}: Bruto R$ {}, Taxa R$ {}",
                agendamento.getId(), valorBruto, taxaRetencao);
    }

    /**
     * Registra o débito correspondente a uma solicitação de saque (SAQUE_DEBITO).
     */
    @Transactional
    public void registrarTransacaoSolicitacaoSaque(Saque saque) {
        if (saque == null || saque.getId() == null) {
            throw new RegraNegocioException("Saque inválido para lançamento contábil.");
        }

        TransacaoFinanceira debito = TransacaoFinanceira.builder()
                .usuario(saque.getProfessor())
                .saque(saque)
                .tipoTransacao(TipoTransacao.SAQUE_DEBITO)
                .valor(saque.getValor())
                .descricao(String.format("Solicitação de Saque PIX (%s) - Protocolo %s",
                        saque.getTipoChavePix() != null ? saque.getTipoChavePix().name() : "PIX",
                        saque.getProtocoloSaque()))
                .dataTransacao(Instant.now())
                .build();

        transacaoRepository.save(debito);
        log.info("Lançamento de débito registrado para saque protocolado {}", saque.getProtocoloSaque());
    }

    /**
     * Registra lançamento de reembolso ao aluno em caso de cancelamento elegível.
     */
    @Transactional
    public void registrarReembolsoAluno(Agendamento agendamento, String motivo) {
        if (agendamento == null || agendamento.getAluno() == null) {
            throw new RegraNegocioException("Agendamento inválido para estorno ao aluno.");
        }

        BigDecimal valor = agendamento.getValorAplicado() != null ? agendamento.getValorAplicado() : BigDecimal.ZERO;

        TransacaoFinanceira reembolso = TransacaoFinanceira.builder()
                .usuario(agendamento.getAluno())
                .agendamento(agendamento)
                .tipoTransacao(TipoTransacao.REEMBOLSO_ALUNO)
                .valor(valor)
                .descricao(String.format("Reembolso de mentoria cancelada #%d: %s",
                        agendamento.getId(),
                        motivo != null ? motivo : "Cancelamento regulamentar"))
                .dataTransacao(Instant.now())
                .build();

        transacaoRepository.save(reembolso);
        log.info("Reembolso registrado para aluno ID #{}, agendamento #{}, valor R$ {}",
                agendamento.getAluno().getId(), agendamento.getId(), valor);
    }
}
