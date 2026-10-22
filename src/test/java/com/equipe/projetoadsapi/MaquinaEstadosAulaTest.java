package com.equipe.projetoadsapi;

import com.equipe.projetoadsapi.dto.AvaliacaoRequestDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import com.equipe.projetoadsapi.service.AulaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes Unitários da Máquina de Estados e Ciclo de Vida da Aula.
 * Rastreabilidade: Fase 6 - f6-t9
 */
@ExtendWith(MockitoExtension.class)
public class MaquinaEstadosAulaTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private SessaoAulaRepository sessaoRepository;

    @Mock
    private AulaEventoRepository eventoRepository;

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private AulaService aulaService;

    private Usuario aluno;
    private Usuario professor;
    private Agendamento agendamentoConfirmado;
    private Agendamento agendamentoPendente;

    @BeforeEach
    void setUp() {
        aluno = Usuario.builder().id(1L).nome("Aluno Teste").email("aluno@orienta.com").build();
        professor = Usuario.builder().id(2L).nome("Prof Teste").email("prof@orienta.com").build();

        agendamentoConfirmado = Agendamento.builder()
                .id(10L)
                .aluno(aluno)
                .professor(professor)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(14, 0))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        agendamentoPendente = Agendamento.builder()
                .id(11L)
                .aluno(aluno)
                .professor(professor)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(15, 0))
                .status(StatusAgendamento.PENDENTE)
                .build();
    }

    @Test
    @DisplayName("Deve rejeitar início de sessão para agendamentos não confirmados (ex: PENDENTE)")
    void testRejeitarInicioSessaoSeNaoConfirmada() {
        when(agendamentoRepository.findById(11L)).thenReturn(Optional.of(agendamentoPendente));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            aulaService.obterOuCriarSessao(11L);
        });

        assertTrue(ex.getMessage().contains("Apenas aulas com status CONFIRMADO podem ter sessão iniciada"));
    }

    @Test
    @DisplayName("Deve rejeitar avaliação se a aula ainda não atingiu o status REALIZADA")
    void testRejeitarAvaliacaoSeNaoRealizada() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamentoConfirmado));

        AvaliacaoRequestDTO dto = AvaliacaoRequestDTO.builder().nota(5).build();

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            aulaService.avaliarAula(10L, 1L, dto);
        });

        assertTrue(ex.getMessage().contains("A avaliação de aula só é liberada após a conclusão mútua (status REALIZADA)"));
    }

    @Test
    @DisplayName("Deve rejeitar check-in de usuário que não faz parte da aula")
    void testRejeitarCheckinDeTerceiro() {
        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(10L)).thenReturn(Optional.of(
                SessaoAula.builder().id(100L).agendamento(agendamentoConfirmado).build()
        ));
        when(sessaoRepository.findById(100L)).thenReturn(Optional.of(
                SessaoAula.builder().id(100L).agendamento(agendamentoConfirmado).build()
        ));

        Usuario terceiro = Usuario.builder().id(999L).nome("Intruso").build();
        when(usuarioRepository.findById(999L)).thenReturn(Optional.of(terceiro));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            aulaService.realizarCheckin(10L, 999L);
        });

        assertTrue(ex.getMessage().contains("Apenas o aluno ou o professor vinculados à aula podem realizar check-in"));
    }

    @Test
    @DisplayName("Deve manter AGUARDANDO_CHECKIN quando apenas um participante realiza check-in")
    void testCheckinUnilateral() {
        SessaoAula sessao = SessaoAula.builder()
                .id(100L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.AGUARDANDO_CHECKIN)
                .alunoCheckin(false)
                .professorCheckin(false)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(10L)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.findById(100L)).thenReturn(Optional.of(sessao));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = aulaService.realizarCheckin(10L, 1L);

        assertTrue(dto.getAlunoCheckin());
        assertFalse(dto.getProfessorCheckin());
        assertEquals(StatusSessao.AGUARDANDO_CHECKIN, dto.getStatusSessao());
        verify(eventoRepository, times(1)).save(any(AulaEvento.class));
    }

    @Test
    @DisplayName("Deve transitar para EM_ANDAMENTO e disparar cronômetro oficial quando ambos dão check-in")
    void testCheckinBilateralDisparaCronometro() {
        SessaoAula sessaoComAluno = SessaoAula.builder()
                .id(100L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.AGUARDANDO_CHECKIN)
                .alunoCheckin(true)
                .professorCheckin(false)
                .build();

        when(agendamentoRepository.findById(10L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(10L)).thenReturn(Optional.of(sessaoComAluno));
        when(sessaoRepository.findById(100L)).thenReturn(Optional.of(sessaoComAluno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = aulaService.realizarCheckin(10L, 2L);

        assertTrue(dto.getAlunoCheckin());
        assertTrue(dto.getProfessorCheckin());
        assertEquals(StatusSessao.EM_ANDAMENTO, dto.getStatusSessao());
        assertNotNull(dto.getInicioEfetivo());
        assertEquals(StatusAgendamento.EM_ANDAMENTO, agendamentoConfirmado.getStatus());
    }

    @Test
    @DisplayName("Deve rejeitar pausa se a aula não estiver EM_ANDAMENTO")
    void testRejeitarPausaEmAulaNaoAtiva() {
        SessaoAula sessaoNaoAtiva = SessaoAula.builder()
                .id(100L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.AGUARDANDO_CHECKIN)
                .build();

        when(sessaoRepository.findByAgendamentoId(10L)).thenReturn(Optional.of(sessaoNaoAtiva));

        assertThrows(RegraNegocioException.class, () -> {
            aulaService.pausarAula(10L, 1L, "Pausa teste");
        });
    }

    @Test
    @DisplayName("Deve transitar para AGUARDANDO_CONFIRMACAO na primeira confirmação e CONCLUIDA na confirmação mútua")
    void testFinalizacaoBilateral() {
        SessaoAula sessaoEmAndamento = SessaoAula.builder()
                .id(100L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.EM_ANDAMENTO)
                .alunoConfirmouFim(false)
                .professorConfirmouFim(false)
                .build();

        when(sessaoRepository.findByAgendamentoId(10L)).thenReturn(Optional.of(sessaoEmAndamento));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Aluno solicita término
        var dto1 = aulaService.finalizarAula(10L, 1L);
        assertEquals(StatusSessao.AGUARDANDO_CONFIRMACAO, dto1.getStatusSessao());
        assertEquals(StatusAgendamento.AGUARDANDO_CONFIRMACAO, agendamentoConfirmado.getStatus());

        // 2. Professor aceita término
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        var dto2 = aulaService.finalizarAula(10L, 2L);
        assertEquals(StatusSessao.CONCLUIDA, dto2.getStatusSessao());
        assertEquals(StatusAgendamento.REALIZADA, agendamentoConfirmado.getStatus());
        assertNotNull(dto2.getFimEfetivo());
    }
}
