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

    // =========================================================================
    // TESTES DA MÁQUINA DE ESTADOS E POLÍTICA DE CANCELAMENTO (BACK-006)
    // =========================================================================

    @Test
    @DisplayName("Deve transitar com sucesso de PENDENTE para CONFIRMADO no aceite do professor")
    void deveAceitarAgendamentoComSucesso() {
        Agendamento pendente = Agendamento.builder()
                .id(100L)
                .aluno(aluno)
                .professor(professor)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(14, 0))
                .status(StatusAgendamento.PENDENTE)
                .build();

        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(pendente));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));

        AgendamentoResponseDTO response = agendamentoService.aceitarAgendamento(100L);

        assertNotNull(response);
        assertEquals(StatusAgendamento.CONFIRMADO, response.getStatus());
        verify(historicoRepository, times(1)).save(argThat(h -> 
                h.getStatusAnterior() == StatusAgendamento.PENDENTE &&
                h.getStatusNovo() == StatusAgendamento.CONFIRMADO &&
                h.getAlteradoPor().getId().equals(2L)
        ));
    }

    @Test
    @DisplayName("Deve rejeitar aceite de agendamento que não esteja PENDENTE")
    void deveRejeitarAceiteSeNaoPendente() {
        Agendamento jaConfirmado = Agendamento.builder()
                .id(101L)
                .aluno(aluno)
                .professor(professor)
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        when(agendamentoRepository.findById(101L)).thenReturn(Optional.of(jaConfirmado));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            agendamentoService.aceitarAgendamento(101L);
        });

        assertTrue(ex.getMessage().contains("Apenas agendamentos com status PENDENTE podem ser aceitos"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve recusar agendamento PENDENTE registrando justificativa obrigatória")
    void deveRecusarAgendamentoComJustificativa() {
        Agendamento pendente = Agendamento.builder()
                .id(102L)
                .aluno(aluno)
                .professor(professor)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(15, 0))
                .status(StatusAgendamento.PENDENTE)
                .build();

        com.equipe.projetoadsapi.dto.RecusarAgendamentoDTO dto = com.equipe.projetoadsapi.dto.RecusarAgendamentoDTO.builder()
                .justificativa("Horário incompatível com banca de TCC")
                .build();

        when(agendamentoRepository.findById(102L)).thenReturn(Optional.of(pendente));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));

        AgendamentoResponseDTO response = agendamentoService.recusarAgendamento(102L, dto);

        assertNotNull(response);
        assertEquals(StatusAgendamento.RECUSADO, response.getStatus());
        verify(historicoRepository, times(1)).save(argThat(h -> 
                h.getStatusAnterior() == StatusAgendamento.PENDENTE &&
                h.getStatusNovo() == StatusAgendamento.RECUSADO &&
                h.getMotivo().contains("Horário incompatível com banca de TCC")
        ));
    }

    @Test
    @DisplayName("Deve permitir cancelamento pelo aluno quando realizado com mais de 2 horas de antecedência")
    void devePermitirCancelamentoAlunoComMaisDe2Horas() {
        // Aula daqui a 24 horas
        LocalDate dataAula = LocalDate.now().plusDays(1);
        LocalTime horaAula = LocalTime.now().plusHours(2);

        Agendamento confirmado = Agendamento.builder()
                .id(103L)
                .aluno(aluno)
                .professor(professor)
                .data(dataAula)
                .hora(horaAula)
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        com.equipe.projetoadsapi.dto.CancelarAgendamentoDTO dto = com.equipe.projetoadsapi.dto.CancelarAgendamentoDTO.builder()
                .justificativa("Imprevisto familiar justificado com antecedência")
                .build();

        when(agendamentoRepository.findById(103L)).thenReturn(Optional.of(confirmado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));

        AgendamentoResponseDTO response = agendamentoService.cancelarAgendamento(103L, dto, 1L);

        assertNotNull(response);
        assertEquals(StatusAgendamento.CANCELADO, response.getStatus());
        verify(historicoRepository, times(1)).save(argThat(h -> 
                h.getStatusNovo() == StatusAgendamento.CANCELADO &&
                h.getAlteradoPor().getId().equals(1L)
        ));
    }

    @Test
    @DisplayName("Deve rejeitar cancelamento pelo aluno quando realizado com menos de 2 horas de antecedência")
    void deveRejeitarCancelamentoAlunoMenosDe2Horas() {
        // Aula daqui a apenas 30 minutos
        LocalDate dataHoje = LocalDate.now();
        LocalTime horaProxima = LocalTime.now().plusMinutes(30);

        Agendamento confirmado = Agendamento.builder()
                .id(104L)
                .aluno(aluno)
                .professor(professor)
                .data(dataHoje)
                .hora(horaProxima)
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        com.equipe.projetoadsapi.dto.CancelarAgendamentoDTO dto = com.equipe.projetoadsapi.dto.CancelarAgendamentoDTO.builder()
                .justificativa("Desistência de última hora")
                .build();

        when(agendamentoRepository.findById(104L)).thenReturn(Optional.of(confirmado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            agendamentoService.cancelarAgendamento(104L, dto, 1L);
        });

        assertTrue(ex.getMessage().contains("menos de 2 horas de antecedência"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir cancelamento pelo professor a qualquer momento com justificativa e estorno")
    void devePermitirCancelamentoProfessorAQualquerMomento() {
        // Aula daqui a 30 minutos
        LocalDate dataHoje = LocalDate.now();
        LocalTime horaProxima = LocalTime.now().plusMinutes(30);

        Agendamento confirmado = Agendamento.builder()
                .id(105L)
                .aluno(aluno)
                .professor(professor)
                .data(dataHoje)
                .hora(horaProxima)
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        com.equipe.projetoadsapi.dto.CancelarAgendamentoDTO dto = com.equipe.projetoadsapi.dto.CancelarAgendamentoDTO.builder()
                .justificativa("Emergência médica urgente do professor")
                .build();

        when(agendamentoRepository.findById(105L)).thenReturn(Optional.of(confirmado));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(agendamentoRepository.save(any(Agendamento.class))).thenAnswer(i -> i.getArgument(0));

        AgendamentoResponseDTO response = agendamentoService.cancelarAgendamento(105L, dto, 2L);

        assertNotNull(response);
        assertEquals(StatusAgendamento.CANCELADO, response.getStatus());
        verify(historicoRepository, times(1)).save(argThat(h -> 
                h.getStatusNovo() == StatusAgendamento.CANCELADO &&
                h.getAlteradoPor().getId().equals(2L) &&
                h.getMotivo().contains("PROFESSOR")
        ));
    }
}
