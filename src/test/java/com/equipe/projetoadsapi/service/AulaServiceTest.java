package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.AvaliacaoRequestDTO;
import com.equipe.projetoadsapi.dto.AvaliacaoResponseDTO;
import com.equipe.projetoadsapi.dto.SessaoAulaDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
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

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TESTES UNITÁRIOS DA SESSÃO DE AULA E CRONÔMETRO BILATERAL (TAREFA [BACK-007]).
 * Cobertura completa de regras de negócio:
 * - Check-in bilateral obrigatório (tolerância de 15 min no check-in unilateral).
 * - Retardo em TOLERÂNCIA sem avanço do cronômetro oficial do servidor.
 * - Cálculo de segundos decorridos no relógio UTC do servidor (anti-fraude).
 * - Identificação semântica do estado FINALIZANDO nos últimos 5 minutos (<= 300s).
 * - Controle de pausa e retomada com desconto compensatório do tempo pausado.
 * - Finalização mútua e transição de agendamento para REALIZADA.
 * - Desbloqueio e validação de avaliação pós-aula exclusiva para status REALIZADA.
 */
@ExtendWith(MockitoExtension.class)
public class AulaServiceTest {

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
    private Usuario terceiro;
    private Agendamento agendamentoConfirmado;
    private Agendamento agendamentoPendente;

    @BeforeEach
    void setUp() {
        aluno = Usuario.builder()
                .id(1L)
                .nome("Lucas Silveira")
                .email("lucas@orienta.com.br")
                .papel(PapelUsuario.ALUNO)
                .build();

        professor = Usuario.builder()
                .id(2L)
                .nome("Prof. Ricardo Santos")
                .email("ricardo@orienta.com.br")
                .papel(PapelUsuario.PROFESSOR)
                .build();

        terceiro = Usuario.builder()
                .id(99L)
                .nome("Usuário Intruso")
                .email("intruso@orienta.com.br")
                .papel(PapelUsuario.ALUNO)
                .build();

        agendamentoConfirmado = Agendamento.builder()
                .id(100L)
                .aluno(aluno)
                .professor(professor)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(14, 0))
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        agendamentoPendente = Agendamento.builder()
                .id(101L)
                .aluno(aluno)
                .professor(professor)
                .data(LocalDate.now().plusDays(2))
                .hora(LocalTime.of(16, 0))
                .status(StatusAgendamento.PENDENTE)
                .build();
    }

    @Test
    @DisplayName("Deve criar nova sessão em AGUARDANDO_CHECKIN quando agendamento estiver CONFIRMADO")
    void deveCriarSessaoEmAguardandoCheckinParaAulaConfirmada() {
        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.empty());
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> {
            SessaoAula sessao = invocation.getArgument(0);
            sessao.setId(500L);
            return sessao;
        });

        SessaoAulaDTO dto = aulaService.obterOuCriarSessao(100L);

        assertNotNull(dto);
        assertEquals(StatusSessao.AGUARDANDO_CHECKIN, dto.getStatusSessao());
        assertFalse(dto.getAlunoCheckin());
        assertFalse(dto.getProfessorCheckin());
        assertEquals(0, dto.getSegundosDecorridos());
        assertEquals(3600, dto.getTempoRestanteSegundos());
        assertNotNull(dto.getTimestampServidor());
    }

    @Test
    @DisplayName("Deve rejeitar criação de sessão se o agendamento estiver com status PENDENTE")
    void deveRejeitarCriacaoDeSessaoParaAgendamentoPendente() {
        when(agendamentoRepository.findById(101L)).thenReturn(Optional.of(agendamentoPendente));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            aulaService.obterOuCriarSessao(101L);
        });

        assertTrue(ex.getMessage().contains("Apenas aulas com status CONFIRMADO podem ter sessão iniciada"));
    }

    @Test
    @DisplayName("Check-in unilateral do Aluno deve transitar para TOLERANCIA (15 min) e manter cronômetro congelado")
    void deveTransitarParaToleranciaNoCheckinUnilateralDoAluno() {
        SessaoAula sessao = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.AGUARDANDO_CHECKIN)
                .alunoCheckin(false)
                .professorCheckin(false)
                .build();

        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.findById(500L)).thenReturn(Optional.of(sessao));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessaoAulaDTO dto = aulaService.realizarCheckin(100L, 1L);

        assertTrue(dto.getAlunoCheckin());
        assertFalse(dto.getProfessorCheckin());
        assertEquals(StatusSessao.TOLERANCIA, dto.getStatusSessao());
        assertNull(dto.getInicioEfetivo());
        assertEquals(0, dto.getSegundosDecorridos());
        assertTrue(dto.getToleranciaRestanteSegundos() > 0 && dto.getToleranciaRestanteSegundos() <= 900);
        verify(eventoRepository, times(1)).save(any(AulaEvento.class));
    }

    @Test
    @DisplayName("Check-in unilateral do Professor deve transitar para TOLERANCIA")
    void deveTransitarParaToleranciaNoCheckinUnilateralDoProfessor() {
        SessaoAula sessao = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.AGUARDANDO_CHECKIN)
                .alunoCheckin(false)
                .professorCheckin(false)
                .build();

        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.findById(500L)).thenReturn(Optional.of(sessao));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessaoAulaDTO dto = aulaService.realizarCheckin(100L, 2L);

        assertFalse(dto.getAlunoCheckin());
        assertTrue(dto.getProfessorCheckin());
        assertEquals(StatusSessao.TOLERANCIA, dto.getStatusSessao());
        assertEquals(0, dto.getSegundosDecorridos());
    }

    @Test
    @DisplayName("Check-in bilateral deve disparar cronômetro oficial no servidor e mudar agendamento para EM_ANDAMENTO")
    void deveDispararCronometroOficialNoCheckinBilateral() {
        SessaoAula sessaoComAluno = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.TOLERANCIA)
                .alunoCheckin(true)
                .alunoCheckinEm(Instant.now().minusSeconds(60))
                .professorCheckin(false)
                .build();

        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessaoComAluno));
        when(sessaoRepository.findById(500L)).thenReturn(Optional.of(sessaoComAluno));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessaoAulaDTO dto = aulaService.realizarCheckin(100L, 2L);

        assertTrue(dto.getAlunoCheckin());
        assertTrue(dto.getProfessorCheckin());
        assertEquals(StatusSessao.EM_ANDAMENTO, dto.getStatusSessao());
        assertNotNull(dto.getInicioEfetivo());
        assertEquals(StatusAgendamento.EM_ANDAMENTO, agendamentoConfirmado.getStatus());
        verify(agendamentoRepository, times(1)).save(agendamentoConfirmado);
    }

    @Test
    @DisplayName("Cálculo anti-fraude no servidor deve computar segundos decorridos e tempo restante estritamente por UTC")
    void deveCalcularSegundosDecorridosAntiFraudePeloServidor() {
        Instant dezMinutosAtras = Instant.now().minusSeconds(600);
        SessaoAula sessaoEmAndamento = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.EM_ANDAMENTO)
                .alunoCheckin(true)
                .professorCheckin(true)
                .inicioEfetivo(dezMinutosAtras)
                .segundosPausados(0)
                .build();

        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessaoEmAndamento));

        SessaoAulaDTO dto = aulaService.obterOuCriarSessao(100L);

        assertEquals(StatusSessao.EM_ANDAMENTO, dto.getStatusSessao());
        assertTrue(dto.getSegundosDecorridos() >= 599 && dto.getSegundosDecorridos() <= 605);
        assertTrue(dto.getTempoRestanteSegundos() <= 3001 && dto.getTempoRestanteSegundos() >= 2995);
    }

    @Test
    @DisplayName("Deve refletir estado FINALIZANDO quando tempo restante for menor ou igual a 5 minutos (<= 300s)")
    void deveRefletirEstadoFinalizandoNosUltimos5Minutos() {
        // Aula com 56 minutos decorridos (restam 4 minutos = 240 segundos)
        Instant cinquentaESeisMinAtras = Instant.now().minusSeconds(3360);
        SessaoAula sessaoFinalizando = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.EM_ANDAMENTO)
                .alunoCheckin(true)
                .professorCheckin(true)
                .inicioEfetivo(cinquentaESeisMinAtras)
                .segundosPausados(0)
                .build();

        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessaoFinalizando));

        SessaoAulaDTO dto = aulaService.obterOuCriarSessao(100L);

        assertEquals(StatusSessao.FINALIZANDO, dto.getStatusSessao());
        assertTrue(dto.getTempoRestanteSegundos() <= 300);
        assertTrue(dto.getTempoRestanteSegundos() > 0);
    }

    @Test
    @DisplayName("Pausa deve congelar contagem e retomada deve aplicar desconto compensatório")
    void devePausarERetomarComDescontoCompensatorio() {
        Instant vinteMinAtras = Instant.now().minusSeconds(1200);
        SessaoAula sessao = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.EM_ANDAMENTO)
                .alunoCheckin(true)
                .professorCheckin(true)
                .inicioEfetivo(vinteMinAtras)
                .segundosPausados(0)
                .build();

        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessao));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 1. Pausar a aula
        SessaoAulaDTO dtoPausada = aulaService.pausarAula(100L, 1L, "Dúvida de conexão");
        assertEquals(StatusSessao.PAUSADA, dtoPausada.getStatusSessao());
        assertNotNull(sessao.getUltimaPausaEm());

        // 2. Simular pausa de 3 minutos (180s)
        sessao.setUltimaPausaEm(Instant.now().minusSeconds(180));

        // 3. Retomar a aula
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        SessaoAulaDTO dtoRetomada = aulaService.retomarAula(100L, 2L);

        assertEquals(StatusSessao.EM_ANDAMENTO, dtoRetomada.getStatusSessao());
        assertNull(sessao.getUltimaPausaEm());
        assertTrue(sessao.getSegundosPausados() >= 180);
    }

    @Test
    @DisplayName("Deve rejeitar pausa se a aula estiver em AGUARDANDO_CHECKIN ou TOLERANCIA")
    void deveRejeitarPausaSeNaoEstiverEmAndamento() {
        SessaoAula sessaoEmTolerancia = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.TOLERANCIA)
                .build();

        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessaoEmTolerancia));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            aulaService.pausarAula(100L, 1L, "Pausa inválida");
        });

        assertTrue(ex.getMessage().contains("Apenas aulas em andamento podem ser pausadas"));
    }

    @Test
    @DisplayName("Finalização bilateral: 1 confirmação vai para AGUARDANDO_CONFIRMACAO, 2ª confirmação transita para REALIZADA")
    void deveFinalizarBilateralmenteComSucesso() {
        SessaoAula sessao = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.EM_ANDAMENTO)
                .alunoCheckin(true)
                .professorCheckin(true)
                .inicioEfetivo(Instant.now().minusSeconds(3600))
                .alunoConfirmouFim(false)
                .professorConfirmouFim(false)
                .build();

        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessao));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(sessaoRepository.save(any(SessaoAula.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Passo 1: Aluno confirma término
        SessaoAulaDTO dtoPasso1 = aulaService.finalizarAula(100L, 1L);
        assertEquals(StatusSessao.AGUARDANDO_CONFIRMACAO, dtoPasso1.getStatusSessao());
        assertEquals(StatusAgendamento.AGUARDANDO_CONFIRMACAO, agendamentoConfirmado.getStatus());

        // Passo 2: Professor confirma término
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(professor));
        SessaoAulaDTO dtoPasso2 = aulaService.finalizarAula(100L, 2L);
        assertEquals(StatusSessao.CONCLUIDA, dtoPasso2.getStatusSessao());
        assertEquals(StatusAgendamento.REALIZADA, agendamentoConfirmado.getStatus());
        assertNotNull(dtoPasso2.getFimEfetivo());
        assertTrue(dtoPasso2.getSegundosDecorridos() >= 3590);
    }

    @Test
    @DisplayName("Deve permitir avaliação exclusivamente após agendamento atingir status REALIZADA")
    void devePermitirAvaliacaoApenasAposRealizada() {
        agendamentoConfirmado.setStatus(StatusAgendamento.REALIZADA);

        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
        when(avaliacaoRepository.existsByAgendamentoIdAndAvaliadorId(100L, 1L)).thenReturn(false);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(invocation -> {
            Avaliacao av = invocation.getArgument(0);
            av.setId(888L);
            return av;
        });
        when(avaliacaoRepository.findByAvaliadoId(2L)).thenReturn(Collections.emptyList());

        AvaliacaoRequestDTO dto = AvaliacaoRequestDTO.builder()
                .nota(5)
                .didatica(5)
                .pontualidade(5)
                .comentario("Excelente aula de Cálculo!")
                .build();

        AvaliacaoResponseDTO resp = aulaService.avaliarAula(100L, 1L, dto);

        assertNotNull(resp);
        assertEquals(5, resp.getNota());
        assertEquals(100L, resp.getAgendamentoId());
        assertEquals("Lucas Silveira", resp.getNomeAvaliador());
    }

    @Test
    @DisplayName("Deve rejeitar avaliação se agendamento ainda estiver em CONFIRMADO")
    void deveRejeitarAvaliacaoSeNaoRealizada() {
        agendamentoConfirmado.setStatus(StatusAgendamento.CONFIRMADO);
        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));

        AvaliacaoRequestDTO dto = AvaliacaoRequestDTO.builder().nota(5).build();

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            aulaService.avaliarAula(100L, 1L, dto);
        });

        assertTrue(ex.getMessage().contains("A avaliação de aula só é liberada após a conclusão mútua (status REALIZADA)"));
    }

    @Test
    @DisplayName("Deve rejeitar check-in de usuário terceiro não vinculado à aula")
    void deveRejeitarCheckinDeTerceiro() {
        SessaoAula sessao = SessaoAula.builder()
                .id(500L)
                .agendamento(agendamentoConfirmado)
                .statusSessao(StatusSessao.AGUARDANDO_CHECKIN)
                .build();

        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamentoConfirmado));
        when(sessaoRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(sessao));
        when(sessaoRepository.findById(500L)).thenReturn(Optional.of(sessao));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(terceiro));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            aulaService.realizarCheckin(100L, 99L);
        });

        assertTrue(ex.getMessage().contains("Apenas o aluno ou o professor vinculados à aula podem realizar check-in"));
    }
}
