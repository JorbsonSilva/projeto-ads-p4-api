package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.StatusAgendamento;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.AgendamentoRepository;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVIÇO DE GERENCIAMENTO DE AGENDAMENTOS (AULAS).
 * Rastreabilidade: Fase 4 - f4-t2, f4-t4, f4-t6, f4-t7
 * Validações de conflitos de horário, máquina de estados rigorosa e respostas em DTOs.
 */
@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public AgendamentoResponseDTO agendarAula(AgendamentoRequestDTO dto) {
        Usuario aluno = usuarioRepository.findById(dto.getIdAluno())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno solicitante não encontrado com ID: " + dto.getIdAluno()));

        Usuario professor = usuarioRepository.findById(dto.getIdProfessor())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado com ID: " + dto.getIdProfessor()));

        if (aluno.getId().equals(professor.getId())) {
            throw new RegraNegocioException("Não é permitido agendar uma aula com você mesmo.");
        }

        // Validação de horário no passado
        LocalDateTime dataHoraAula = LocalDateTime.of(dto.getData(), dto.getHora());
        if (dataHoraAula.isBefore(LocalDateTime.now())) {
            throw new RegraNegocioException("Não é permitido agendar aulas em data ou horário no passado.");
        }

        // Validação de conflito de horário do professor
        boolean horarioOcupado = agendamentoRepository.existsByProfessorAndDataAndHora(
                professor,
                dto.getData(),
                dto.getHora()
        );

        if (horarioOcupado) {
            throw new RegraNegocioException("Conflito de agenda: O professor já possui uma aula marcada neste dia e horário.");
        }

        Agendamento agendamento = Agendamento.builder()
                .aluno(aluno)
                .professor(professor)
                .data(dto.getData())
                .hora(dto.getHora())
                .status(StatusAgendamento.PENDENTE)
                .modalidade(dto.getModalidade() != null ? dto.getModalidade() : "PRESENCIAL")
                .assunto(dto.getAssunto() != null ? dto.getAssunto() : "Mentoria Acadêmica")
                .build();

        Agendamento salvo = agendamentoRepository.save(agendamento);
        return AgendamentoResponseDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> buscarPorAluno(Long alunoId) {
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aluno não encontrado com ID: " + alunoId));

        return agendamentoRepository.findByAluno(aluno)
                .stream()
                .map(AgendamentoResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> buscarPorProfessor(Long professorId) {
        Usuario professor = usuarioRepository.findById(professorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Professor não encontrado com ID: " + professorId));

        return agendamentoRepository.findByProfessor(professor)
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

    @Transactional
    public AgendamentoResponseDTO aceitarAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new RegraNegocioException("Apenas agendamentos com status PENDENTE podem ser aceitos pelo professor. Status atual: " + agendamento.getStatus());
        }

        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Agendamento atualizado = agendamentoRepository.save(agendamento);
        return AgendamentoResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public AgendamentoResponseDTO recusarAgendamento(Long id, RecusarAgendamentoDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new RegraNegocioException("Apenas agendamentos PENDENTES podem ser recusados.");
        }

        agendamento.setStatus(StatusAgendamento.RECUSADO);
        agendamento.setJustificativaCancelamento(dto.getJustificativa());
        Agendamento atualizado = agendamentoRepository.save(agendamento);
        return AgendamentoResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public AgendamentoResponseDTO cancelarAgendamento(Long id, CancelarAgendamentoDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() == StatusAgendamento.REALIZADA || agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Não é possível cancelar uma aula que já foi realizada ou cancelada.");
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
        agendamento.setJustificativaCancelamento(dto.getJustificativa());
        Agendamento atualizado = agendamentoRepository.save(agendamento);
        return AgendamentoResponseDTO.fromEntity(atualizado);
    }

    @Transactional
    public AgendamentoResponseDTO remarcarAgendamento(Long id, RemarcarAgendamentoDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado com ID: " + id));

        if (agendamento.getStatus() == StatusAgendamento.REALIZADA || agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new RegraNegocioException("Não é possível remarcar uma aula cancelada ou já realizada.");
        }

        boolean horarioOcupado = agendamentoRepository.existsByProfessorAndDataAndHora(
                agendamento.getProfessor(),
                dto.getData(),
                dto.getHora()
        );

        if (horarioOcupado) {
            throw new RegraNegocioException("O professor já possui outra aula agendada no novo horário selecionado.");
        }

        agendamento.setData(dto.getData());
        agendamento.setHora(dto.getHora());
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        if (dto.getMotivo() != null && !dto.getMotivo().isBlank()) {
            agendamento.setJustificativaCancelamento("Remarcado: " + dto.getMotivo());
        }

        Agendamento atualizado = agendamentoRepository.save(agendamento);
        return AgendamentoResponseDTO.fromEntity(atualizado);
    }
}