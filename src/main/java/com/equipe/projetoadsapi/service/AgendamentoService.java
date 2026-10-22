package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVIÇO DE GERENCIAMENTO DE AGENDAMENTOS (AULAS).
 * Rastreabilidade: Fase 4 (Padronização) & Fase 5 (Evolução de Domínio)
 * Tarefas: f5-t3..f5-t10 (Regra de 24h, Concorrência Atômica, Auditoria e Congelamento de Valor)
 */
@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private SaberRepository saberRepository;

    @Autowired
    private AptidaoRepository aptidaoRepository;

    @Autowired
    private AgendamentoHistoricoRepository historicoRepository;

    /**
     * Solicitação de aula pelo aluno com verificação de antecedência de 24h e congelamento de preço.
     */
    @Transactional
    public AgendamentoResponseDTO agendarAula(AgendamentoRequestDTO dto) {
        Usuario aluno = usuarioRepository.findById(dto.getIdAluno())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno solicitante não encontrado com ID: " + dto.getIdAluno()));

        Usuario professor = usuarioRepository.findById(dto.getIdProfessor())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado com ID: " + dto.getIdProfessor()));

        if (aluno.getId().equals(professor.getId())) {
            throw new RegraNegocioException("Não é permitido agendar uma aula consigo mesmo.");
        }

        // Regra de Negócio P1 (f5-t8): Antecedência mínima de 24 horas no servidor
        LocalDateTime dataHoraAula = LocalDateTime.of(dto.getData(), dto.getHora());
        LocalDateTime antecedenciaMinima = LocalDateTime.now().plusHours(24);
        if (dataHoraAula.isBefore(antecedenciaMinima)) {
            throw new RegraNegocioException("Agendamentos devem ser solicitados com antecedência mínima de 24 horas em relação ao horário atual.");
        }

        // Concorrência Atômica (f5-t9): Validação contra choques de horário
        boolean conflito = agendamentoRepository.existsByProfessorAndDataAndHoraAndStatusNotIn(
                professor,
                dto.getData(),
                dto.getHora(),
                List.of(StatusAgendamento.CANCELADO, StatusAgendamento.RECUSADO)
        );

        if (conflito) {
            throw new RegraNegocioException("Conflito de agenda: O professor já possui uma aula ativa marcada neste dia e horário.");
        }

        // Vínculo opcional de Saber/Disciplina (f5-t3)
        Saber saber = null;
        BigDecimal valorCongelado = BigDecimal.ZERO;
        if (dto.getIdDisciplina() != null) {
            saber = saberRepository.findById(dto.getIdDisciplina()).orElse(null);
            if (saber != null) {
                // Congela valor do professor para a disciplina (f5-t5)
                List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(professor.getId());
                for (Aptidao a : aptidoes) {
                    if (a.getSaber() != null && a.getSaber().getId().equals(saber.getId()) && a.getPreco() != null) {
                        valorCongelado = a.getPreco();
                        break;
                    }
                }
            }
        }

        Agendamento agendamento = Agendamento.builder()
                .aluno(aluno)
                .professor(professor)
                .saber(saber)
                .data(dto.getData())
                .hora(dto.getHora())
                .status(StatusAgendamento.PENDENTE)
                .valorAplicado(valorCongelado)
                .modalidade(dto.getModalidade() != null ? dto.getModalidade() : "PRESENCIAL")
                .assunto(dto.getAssunto() != null ? dto.getAssunto() : (saber != null ? saber.getNome() : "Mentoria Acadêmica"))
                .build();

        Agendamento salvo = agendamentoRepository.save(agendamento);

        // Registro de Auditoria e Histórico (f5-t7)
        registrarHistorico(salvo, null, StatusAgendamento.PENDENTE, "Solicitação inicial realizada pelo aluno", aluno);

        return AgendamentoResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> buscarPorAluno(Long alunoId) {
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado com ID: " + alunoId));

        return agendamentoRepository.findByAlunoOrderByDataAscHoraAsc(aluno)
                .stream()
                .map(AgendamentoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> buscarPorProfessor(Long professorId) {
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado com ID: " + professorId));

        return agendamentoRepository.findByProfessorOrderByDataAscHoraAsc(professor)
                .stream()
                .map(AgendamentoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgendamentoResponseDTO buscarPorId(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));
        return AgendamentoResponseDTO.fromEntity(agendamento);
    }

    /**
     * Aceite pelo professor (f5-t6, f5-t10)
     */
    @Transactional
    public AgendamentoResponseDTO aceitarAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new RegraNegocioException("Apenas agendamentos com status PENDENTE podem ser aceitos pelo professor. Status atual: " + agendamento.getStatus());
        }

        StatusAgendamento statusAnterior = agendamento.getStatus();
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Agendamento atualizado = agendamentoRepository.save(agendamento);

        registrarHistorico(atualizado, statusAnterior, StatusAgendamento.CONFIRMADO, "Aula aceita e confirmada pelo professor", agendamento.getProfessor());

        return AgendamentoResponseDTO.fromEntity(atualizado);
    }

    /**
     * Recusa formal pelo professor (f5-t10)
     */
    @Transactional
    public AgendamentoResponseDTO recusarAgendamento(Long id, RecusarAgendamentoDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new RegraNegocioException("Apenas agendamentos PENDENTES podem ser recusados pelo professor.");
        }

        StatusAgendamento statusAnterior = agendamento.getStatus();
        agendamento.setStatus(StatusAgendamento.RECUSADO);
        agendamento.setJustificativaCancelamento(dto.getJustificativa());
        Agendamento atualizado = agendamentoRepository.save(agendamento);

        registrarHistorico(atualizado, statusAnterior, StatusAgendamento.RECUSADO, dto.getJustificativa(), agendamento.getProfessor());

        return AgendamentoResponseDTO.fromEntity(atualizado);
    }

    /**
     * Cancelamento formal (f5-t10)
     */
    @Transactional
    public AgendamentoResponseDTO cancelarAgendamento(Long id, CancelarAgendamentoDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() == StatusAgendamento.REALIZADA || agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Não é possível cancelar uma aula que já foi realizada ou cancelada.");
        }

        StatusAgendamento statusAnterior = agendamento.getStatus();
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setJustificativaCancelamento(dto.getJustificativa());
        Agendamento atualizado = agendamentoRepository.save(agendamento);

        registrarHistorico(atualizado, statusAnterior, StatusAgendamento.CANCELADO, dto.getJustificativa(), agendamento.getAluno());

        return AgendamentoResponseDTO.fromEntity(atualizado);
    }

    /**
     * Remarcação de aula com verificação de conflitos e antecedência (f5-t8, f5-t9)
     */
    @Transactional
    public AgendamentoResponseDTO remarcarAgendamento(Long id, RemarcarAgendamentoDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() == StatusAgendamento.REALIZADA || agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Não é possível remarcar uma aula cancelada ou já realizada.");
        }

        // Validação de 24h para reagendamento
        LocalDateTime novaDataHora = LocalDateTime.of(dto.getData(), dto.getHora());
        if (novaDataHora.isBefore(LocalDateTime.now().plusHours(24))) {
            throw new RegraNegocioException("Reagendamentos devem respeitar a antecedência mínima de 24 horas.");
        }

        boolean conflito = agendamentoRepository.existsByProfessorAndDataAndHoraAndStatusNotIn(
                agendamento.getProfessor(),
                dto.getData(),
                dto.getHora(),
                List.of(StatusAgendamento.CANCELADO, StatusAgendamento.RECUSADO)
        );

        if (conflito) {
            throw new RegraNegocioException("O professor já possui outra aula ativa marcada no novo horário selecionado.");
        }

        StatusAgendamento statusAnterior = agendamento.getStatus();
        agendamento.setData(dto.getData());
        agendamento.setHora(dto.getHora());
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        if (dto.getMotivo() != null && !dto.getMotivo().isBlank()) {
            agendamento.setJustificativaCancelamento("Remarcado: " + dto.getMotivo());
        }

        Agendamento atualizado = agendamentoRepository.save(agendamento);

        registrarHistorico(atualizado, statusAnterior, StatusAgendamento.CONFIRMADO, 
                "Aula remarcada para " + dto.getData() + " às " + dto.getHora() + (dto.getMotivo() != null ? ": " + dto.getMotivo() : ""), 
                agendamento.getAluno());

        return AgendamentoResponseDTO.fromEntity(atualizado);
    }

    private void registrarHistorico(Agendamento agendamento, StatusAgendamento anterior, StatusAgendamento novo, String motivo, Usuario autor) {
        AgendamentoHistorico historico = AgendamentoHistorico.builder()
                .agendamento(agendamento)
                .statusAnterior(anterior)
                .statusNovo(novo)
                .motivo(motivo)
                .alteradoPor(autor)
                .build();
        historicoRepository.save(historico);
    }
}