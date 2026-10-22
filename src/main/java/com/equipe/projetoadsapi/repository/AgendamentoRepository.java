package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    // verificar se o professor está ocupado
    boolean existsByProfessorAndDataAndHora(Usuario professor, LocalDate data, LocalTime hora);
    
    // buscar agendamento de um professor ou aluno específico
    List<Agendamento> findByAluno(Usuario aluno);
    List<Agendamento> findByProfessor(Usuario professor);
}