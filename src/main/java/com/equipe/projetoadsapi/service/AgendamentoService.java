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

        // Regra de Negócio P1 (f5-t8): Antecedência mínima estrita de 24 horas no servidor
        LocalDateTime dataHoraAula = LocalDateTime.of(dto.getData(), dto.getHora());
        LocalDateTime antecedenciaMinima = LocalDateTime.now().plusHours(24);
        if (dataHoraAula.isBefore(antecedenciaMinima)) {
            throw new RegraNegocioException("Agendamentos devem ser solicitados com antecedência mínima de 24 horas em relação ao horário atual.");
        }

        // Concorrência Atômica Bilateral (f5-t9): Validação contra choques de horário na agenda do Professor e do Aluno
        List<StatusAgendamento> statusInativos = List.of(StatusAgendamento.CANCELADO, StatusAgendamento.RECUSADO);

        boolean conflitoProfessor = agendamentoRepository.existsByProfessorAndDataAndHoraAndStatusNotIn(
                professor,
                dto.getData(),
                dto.getHora(),
                statusInativos
        );

        if (conflitoProfessor) {
            throw new RegraNegocioException("Conflito de agenda: O professor já possui uma aula ativa marcada neste dia e horário.");
        }

        boolean conflitoAluno = agendamentoRepository.existsByAlunoAndDataAndHoraAndStatusNotIn(
                aluno,
                dto.getData(),
                dto.getHora(),
                statusInativos
        );

        if (conflitoAluno) {
            throw new RegraNegocioException("Conflito de agenda: O aluno já possui outra mentoria marcada neste mesmo dia e horário.");
        }

        // Vínculo opcional de Saber/Disciplina (f5-t3) e congelamento atômico de preço (f5-t5)
        Saber saber = null;
        BigDecimal valorCongelado = BigDecimal.ZERO;
        List<Aptidao> aptidoes = aptidaoRepository.findByUsuarioId(professor.getId());

        if (dto.getIdDisciplina() != null) {
            saber = saberRepository.findById(dto.getIdDisciplina()).orElse(null);
            if (saber != null && aptidoes != null) {
                // Busca o preço específico configurado para a disciplina selecionada
                for (Aptidao a : aptidoes) {
                    if (a.getSaber() != null && a.getSaber().getId().equals(saber.getId()) && a.getPreco() != null) {
                        valorCongelado = a.getPreco();
                        break;
                    }
                }
            }
        }

        // Se não encontrou valor específico para a matéria mas há aptidões cadastradas com preço, utiliza a primeira válida
        if (valorCongelado.compareTo(BigDecimal.ZERO) == 0 && aptidoes != null && !aptidoes.isEmpty()) {
            for (Aptidao a : aptidoes) {
                if (a.getPreco() != null && a.getPreco().compareTo(BigDecimal.ZERO) > 0) {
                    valorCongelado = a.getPreco();
                    if (saber == null && a.getSaber() != null) {
                        saber = a.getSaber();
                    }
                    break;
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
     * Cancelamento formal com regra bilateral e política de 2 horas (BACK-006).
     * - Aluno cancelando com > 2h: cancelamento permitido com estorno integral.
     * - Aluno cancelando com < 2h: rejeitado com 422 (custo de oportunidade do docente).
     * - Professor cancelando a qualquer momento: permitido com estorno integral e registro de auditoria.
     * - Administrador: cancelamento irrestrito para moderação e suporte.
     */
    @Transactional
    public AgendamentoResponseDTO cancelarAgendamento(Long id, CancelarAgendamentoDTO dto, Long usuarioAutenticadoId) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() == StatusAgendamento.REALIZADA || agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Não é possível cancelar uma aula que já foi realizada ou cancelada. Status atual: " + agendamento.getStatus());
        }

        Usuario autor = null;
        if (usuarioAutenticadoId != null) {
            autor = usuarioRepository.findById(usuarioAutenticadoId).orElse(null);
        }
        if (autor == null) {
            autor = agendamento.getAluno(); // Fallback
        }

        boolean isAluno = autor.getId().equals(agendamento.getAluno().getId());
        boolean isProfessor = autor.getId().equals(agendamento.getProfessor().getId());

        // Regra de Cancelamento Bilateral (Janela de 2 Horas)
        LocalDateTime dataHoraAula = LocalDateTime.of(agendamento.getData(), agendamento.getHora());
        LocalDateTime agora = LocalDateTime.now();

        if (isAluno && !isProfessor) {
            // Aluno cancelando: deve possuir pelo menos 2 horas de antecedência
            if (agora.plusHours(2).isAfter(dataHoraAula)) {
                throw new RegraNegocioException(
                        "Cancelamento pelo aluno não permitido com menos de 2 horas de antecedência do início da aula. " +
                        "Horário agendado: " + dataHoraAula + ". O valor é retido em favor do professor por custo de oportunidade de horário bloqueado.");
            }
        }

        StatusAgendamento statusAnterior = agendamento.getStatus();
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setJustificativaCancelamento(dto.getJustificativa());
        Agendamento atualizado = agendamentoRepository.save(agendamento);

        String motivoAuditoria = String.format("Cancelado por %s (%s): %s",
                autor.getNome(),
                isProfessor ? "PROFESSOR" : (isAluno ? "ALUNO" : "ADMIN"),
                dto.getJustificativa());

        registrarHistorico(atualizado, statusAnterior, StatusAgendamento.CANCELADO, motivoAuditoria, autor);

        return AgendamentoResponseDTO.fromEntity(atualizado);
    }

    /**
     * Sobrecarga para retrocompatibilidade
     */
    @Transactional
    public AgendamentoResponseDTO cancelarAgendamento(Long id, CancelarAgendamentoDTO dto) {
        return cancelarAgendamento(id, dto, null);
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

        List<StatusAgendamento> statusInativos = List.of(StatusAgendamento.CANCELADO, StatusAgendamento.RECUSADO);

        boolean conflitoProfessor = agendamentoRepository.existsByProfessorAndDataAndHoraAndIdNotAndStatusNotIn(
                agendamento.getProfessor(),
                dto.getData(),
                dto.getHora(),
                agendamento.getId(),
                statusInativos
        );

        if (conflitoProfessor) {
            throw new RegraNegocioException("O professor já possui outra aula ativa marcada no novo horário selecionado.");
        }

        boolean conflitoAluno = agendamentoRepository.existsByAlunoAndDataAndHoraAndIdNotAndStatusNotIn(
                agendamento.getAluno(),
                dto.getData(),
                dto.getHora(),
                agendamento.getId(),
                statusInativos
        );

        if (conflitoAluno) {
            throw new RegraNegocioException("O aluno já possui outra mentoria marcada no novo horário selecionado.");
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