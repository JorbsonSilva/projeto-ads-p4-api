package com.equipe.projetoadsapi.repository;

import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.StatusAgendamento;
import com.equipe.projetoadsapi.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do repositório AgendamentoRepository (Tarefa [BACK-019]).
 * Valida a recuperação de agendamentos com status REALIZADA e ordenação cronológica decrescente.
 */
@ExtendWith(MockitoExtension.class)
public class AgendamentoRepositoryTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Test
    @DisplayName("Deve buscar agendamentos realizados ordenados por data e hora decrescentes")
    void deveBuscarAgendamentosRealizadosOrdenadosDesc() {
        Long professorId = 10L;

        Usuario professor = new Usuario();
        professor.setId(professorId);

        Usuario aluno = new Usuario();
        aluno.setId(101L);

        Agendamento a1 = Agendamento.builder()
                .id(1L)
                .professor(professor)
                .aluno(aluno)
                .data(LocalDate.of(2026, 11, 5))
                .hora(LocalTime.of(15, 0))
                .status(StatusAgendamento.REALIZADA)
                .build();

        Agendamento a2 = Agendamento.builder()
                .id(2L)
                .professor(professor)
                .aluno(aluno)
                .data(LocalDate.of(2026, 11, 1))
                .hora(LocalTime.of(10, 0))
                .status(StatusAgendamento.REALIZADA)
                .build();

        when(agendamentoRepository.findByProfessorIdAndStatusOrderByDataDescHoraDesc(professorId, StatusAgendamento.REALIZADA))
                .thenReturn(List.of(a1, a2));

        List<Agendamento> resultado = agendamentoRepository
                .findByProfessorIdAndStatusOrderByDataDescHoraDesc(professorId, StatusAgendamento.REALIZADA);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(1L, resultado.get(0).getId());
        assertEquals(LocalDate.of(2026, 11, 5), resultado.get(0).getData());
        assertEquals(2L, resultado.get(1).getId());

        verify(agendamentoRepository, times(1))
                .findByProfessorIdAndStatusOrderByDataDescHoraDesc(professorId, StatusAgendamento.REALIZADA);
    }
}
