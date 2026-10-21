package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.Usuario;
import com.equipe.projetoadsapi.repository.AgendamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List; // Importação necessária para a lista

@Service
public class AgendamentoService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    public Agendamento agendarAula(Agendamento agendamento) {
        boolean horarioOcupado = agendamentoRepository.existsByProfessorAndDataAndHora(
                agendamento.getProfessor(),
                agendamento.getData(),
                agendamento.getHora()
        );

        if (horarioOcupado) {
            throw new RuntimeException("Erro: O professor já possui uma aula marcada neste horário.");
        }

        // 🟢 É NESTA LINHA AQUI! Deve estar PENDENTE e não CONFIRMADO.
        agendamento.setStatus("PENDENTE"); 
        
        return agendamentoRepository.save(agendamento);
    }

    public List<Agendamento> buscarPorAluno(Long alunoId) {
        Usuario aluno = new Usuario();
        aluno.setId(alunoId);
        return agendamentoRepository.findByAluno(aluno);
    }

    public Agendamento remarcarAgendamento(Long id, Agendamento dadosAtualizados) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));

        boolean horarioOcupado = agendamentoRepository.existsByProfessorAndDataAndHora(
                agendamento.getProfessor(), 
                dadosAtualizados.getData(), 
                dadosAtualizados.getHora()
        );

        if (horarioOcupado) {
            throw new RuntimeException("Erro: O professor já possui aula neste novo horário.");
        }

        agendamento.setData(dadosAtualizados.getData());
        agendamento.setHora(dadosAtualizados.getHora());
        agendamento.setStatus("CONFIRMADO"); 
        
        return agendamentoRepository.save(agendamento);
    }

public Agendamento cancelarAgendamento(Long id, String justificativa) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));
        
        // Regra de validação no back-end: não cancela se estiver vazio
        if (justificativa == null || justificativa.trim().isEmpty()) {
            throw new RuntimeException("A justificativa é obrigatória.");
        }
        
        agendamento.setStatus("CANCELADO");
        agendamento.setJustificativaCancelamento(justificativa); // Salva o motivo
        return agendamentoRepository.save(agendamento);
    }

    // Busca os agendamentos onde o usuário é o professor
    public List<Agendamento> buscarPorProfessor(Long professorId) {
        Usuario professor = new Usuario();
        professor.setId(professorId);
        return agendamentoRepository.findByProfessor(professor);
    }

    // Altera o status da aula de PENDENTE para CONFIRMADO
    public Agendamento aceitarAgendamento(Long id) {
        Agendamento agendamento = agendamentoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado."));
        
        if (!"PENDENTE".equals(agendamento.getStatus())) {
            throw new RuntimeException("Este agendamento não está mais pendente.");
        }
        
        agendamento.setStatus("CONFIRMADO");
        return agendamentoRepository.save(agendamento);
    }
}