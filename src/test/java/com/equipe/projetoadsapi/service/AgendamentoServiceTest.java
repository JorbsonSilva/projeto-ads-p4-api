package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.AgendamentoRequestDTO;
import com.equipe.projetoadsapi.dto.AgendamentoResponseDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TESTES UNITÁRIOS DE AGENDAMENTO (TAREFA [BACK-005])
 * Cobertura de regras de negócio:
 * - Validação estrita de antecedência de 24 horas no servidor.
 * - Bloqueio de auto-contratação.
 * - Concorrência atômica bilateral (conflito do professor e do aluno).
 * - Congelamento atômico do valorAplicado na entidade Agendamento.
 */
@ExtendWith(MockitoExtension.class)
public class AgendamentoServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SaberRepository saberRepository;

    @Mock
    private AptidaoRepository aptidaoRepository;

    @Mock
    private AgendamentoHistoricoRepository historicoRepository;

    @InjectMocks
    private AgendamentoService agendamentoService;

    private Usuario aluno;
    private Usuario professor;
    private Saber saberMatematica;
    private Aptidao aptidaoProfessor;

    @BeforeEach
    void setUp() {
        aluno = Usuario.builder()
                .id(1L)
                .nome("Maria Aluna")
                .email("maria@aluno.orienta.com.br")
                .papel(PapelUsuario.ROLE_ALUNO)
                .build();

        professor = Usuario.builder()
                .id(2L)
                .nome("Prof. Roberto Silva")
                .email("roberto@professor.orienta.com.br")
                .papel(PapelUsuario.ROLE_PROFESSOR)
                .build();

        saberMatematica = Saber.builder()
                .id(10L)
                .nome("Cálculo I")
                .categoria("Exatas")
                .build();

        aptidaoProfessor = new Aptidao();
        aptidaoProfessor.setId(100L);
        aptidaoProfessor.setUsuario(professor);
        aptidaoProfessor.setSaber(saberMatematica);
        aptidaoProfessor.setNivelDominio("Avançado");
        aptidaoProfessor.setPrecoHora(new BigDecimal("75.00"));
    }

    @Test
    @DisplayName("Deve criar agendamento válido com antecedência superior a 24h e congelar valorAplicado")
    void deveCriarAgendamentoComSucessoEPrecoCongelado() {
        LocalDate dataFutura = LocalDate.now().plusDays(3);
        LocalTime horaFutura = LocalTime.of(14, 0);

        AgendamentoRequestDTO dto = AgendamentoRequestDTO.builder()
                .idAluno(1L)
                .idProfessor(2L)
                .idDisciplina(10L)
                .data(dataFutura)
                .hora(horaFutura)
                .modalidade("ONLINE")
                .assunto("Dúvidas sobre Derivadas")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(agendamentoRepository.existsByProfessorAndDataAndHoraAndStatusNotIn(eq(professor), eq(dataFutura), eq(horaFutura), any()))
                .thenReturn(false);
        when(agendamentoRepository.existsByAlunoAndDataAndHoraAndStatusNotIn(eq(aluno), eq(dataFutura), eq(horaFutura), any()))
                .thenReturn(false);
        when(saberRepository.findById(10L)).thenReturn(Optional.of(saberMatematica));
        when(aptidaoRepository.findByUsuarioId(2L)).thenReturn(List.of(aptidaoProfessor));

        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(invocation -> {
            Agendamento a = invocation.getArgument(0);
            a.setId(500L);
            return a;
        });

        AgendamentoResponseDTO response = agendamentoService.agendarAula(dto);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(StatusAgendamento.PENDENTE, response.getStatus());
        assertEquals(new BigDecimal("75.00"), response.getValorAplicado());
        assertEquals("ONLINE", response.getModalidade());
        assertEquals("Dúvidas sobre Derivadas", response.getAssunto());

        verify(historicoRepository, times(1)).save(any(AgendamentoHistorico.class));
    }

    @Test
    @DisplayName("Deve rejeitar agendamento com menos de 24 horas de antecedência")
    void deveRejeitarAgendamentoMenosDe24Horas() {
        // Horário de aula daqui a apenas 2 horas
        LocalDate dataHoje = LocalDate.now();
        LocalTime horaProxima = LocalTime.now().plusHours(2);

        AgendamentoRequestDTO dto = AgendamentoRequestDTO.builder()
                .idAluno(1L)
                .idProfessor(2L)
                .data(dataHoje)
                .hora(horaProxima)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            agendamentoService.agendarAula(dto);
        });

        assertTrue(ex.getMessage().contains("antecedência mínima de 24 horas"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar agendamento quando o professor já possui aula ativa no mesmo horário")
    void deveRejeitarConflitoAgendaProfessor() {
        LocalDate dataFutura = LocalDate.now().plusDays(2);
        LocalTime horaFutura = LocalTime.of(10, 0);

        AgendamentoRequestDTO dto = AgendamentoRequestDTO.builder()
                .idAluno(1L)
                .idProfessor(2L)
                .data(dataFutura)
                .hora(horaFutura)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(agendamentoRepository.existsByProfessorAndDataAndHoraAndStatusNotIn(eq(professor), eq(dataFutura), eq(horaFutura), any()))
                .thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            agendamentoService.agendarAula(dto);
        });

        assertTrue(ex.getMessage().contains("O professor já possui uma aula ativa marcada"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar agendamento quando o próprio aluno já possui outra aula no mesmo horário")
    void deveRejeitarConflitoAgendaAluno() {
        LocalDate dataFutura = LocalDate.now().plusDays(2);
        LocalTime horaFutura = LocalTime.of(16, 0);

        AgendamentoRequestDTO dto = AgendamentoRequestDTO.builder()
                .idAluno(1L)
                .idProfessor(2L)
                .data(dataFutura)
                .hora(horaFutura)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(agendamentoRepository.existsByProfessorAndDataAndHoraAndStatusNotIn(eq(professor), eq(dataFutura), eq(horaFutura), any()))
                .thenReturn(false);
        when(agendamentoRepository.existsByAlunoAndDataAndHoraAndStatusNotIn(eq(aluno), eq(dataFutura), eq(horaFutura), any()))
                .thenReturn(true);

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            agendamentoService.agendarAula(dto);
        });

        assertTrue(ex.getMessage().contains("O aluno já possui outra mentoria marcada"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve bloquear tentativa de auto-contratação (aluno == professor)")
    void deveBloquearAutoContratacao() {
        LocalDate dataFutura = LocalDate.now().plusDays(2);
        LocalTime horaFutura = LocalTime.of(15, 0);

        AgendamentoRequestDTO dto = AgendamentoRequestDTO.builder()
                .idAluno(2L)
                .idProfessor(2L)
                .data(dataFutura)
                .hora(horaFutura)
                .build();

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            agendamentoService.agendarAula(dto);
        });

        assertTrue(ex.getMessage().contains("Não é permitido agendar uma aula consigo mesmo"));
        verify(agendamentoRepository, never()).save(any());
    }
}
