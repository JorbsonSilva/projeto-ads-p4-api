package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.StatusAgendamento;
import com.equipe.projetoadsapi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

/**
 * Repositório de dados para agendamentos de aulas.
 * Rastreabilidade: Fase 2 (Baseline), Fase 5 (Concorrência Atômica - f5-t9)
 */
@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    
    // Conflito considerando apenas agendamentos ativos do professor (não cancelados nem recusados)
    boolean existsByProfessorAndDataAndHoraAndStatusNotIn(
            Usuario professor, LocalDate data, LocalTime hora, Collection<StatusAgendamento> statusInativos);

    // Conflito considerando agendamentos ativos do aluno (não cancelados nem recusados)
    boolean existsByAlunoAndDataAndHoraAndStatusNotIn(
            Usuario aluno, LocalDate data, LocalTime hora, Collection<StatusAgendamento> statusInativos);

    // Conflito excluindo o próprio ID (útil em reagendamentos)
    boolean existsByProfessorAndDataAndHoraAndIdNotAndStatusNotIn(
            Usuario professor, LocalDate data, LocalTime hora, Long id, Collection<StatusAgendamento> statusInativos);

    boolean existsByAlunoAndDataAndHoraAndIdNotAndStatusNotIn(
            Usuario aluno, LocalDate data, LocalTime hora, Long id, Collection<StatusAgendamento> statusInativos);

    // Consulta legada mantida para retrocompatibilidade
    boolean existsByProfessorAndDataAndHora(Usuario professor, LocalDate data, LocalTime hora);

    List<Agendamento> findByAlunoOrderByDataAscHoraAsc(Usuario aluno);
    List<Agendamento> findByProfessorOrderByDataAscHoraAsc(Usuario professor);

    List<Agendamento> findByAluno(Usuario aluno);
    List<Agendamento> findByProfessor(Usuario professor);
}