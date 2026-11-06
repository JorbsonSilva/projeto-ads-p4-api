package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.*;
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
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o serviço de Suporte, Tickets e Denúncias na Ouvidoria (BACK-015, BACK-016).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-016)
 */
@ExtendWith(MockitoExtension.class)
public class SuporteServiceTest {

    @Mock
    private TicketSuporteRepository ticketRepository;

    @Mock
    private DenunciaRepository denunciaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private SuporteService suporteService;

    private Usuario aluno;
    private Usuario professor;
    private Usuario admin;
    private Usuario outroAluno;
    private TicketSuporte ticketAluno;
    private Denuncia denunciaExemplo;

    @BeforeEach
    void setUp() {
        aluno = Usuario.builder()
                .id(10L)
                .nome("Ana Clara")
                .email("ana@orienta.com.br")
                .papel(PapelUsuario.ROLE_ALUNO)
                .build();

        professor = Usuario.builder()
                .id(20L)
                .nome("Prof. Lucas Rocha")
                .email("lucas@orienta.com.br")
                .papel(PapelUsuario.ROLE_PROFESSOR)
                .build();

        outroAluno = Usuario.builder()
                .id(99L)
                .nome("Carlos Intruso")
                .email("carlos@orienta.com.br")
                .papel(PapelUsuario.ROLE_ALUNO)
                .build();

        admin = Usuario.builder()
                .id(1L)
                .nome("Admin Geral")
                .email("admin@orienta.com.br")
                .papel(PapelUsuario.ROLE_ADMIN)
                .build();

        int ano = LocalDate.now().getYear();
        ticketAluno = TicketSuporte.builder()
                .id(100L)
                .usuario(aluno)
                .protocolo("SUP-" + ano + "-12345")
                .titulo("Dúvida sobre cancelamento")
                .assunto("Dúvida sobre cancelamento")
                .descricao("Como funciona a política de 2 horas?")
                .categoria(CategoriaTicket.DUVIDA)
                .prioridade(PrioridadeTicket.MEDIA)
                .status(StatusTicket.ABERTO)
                .criadoEm(Instant.now())
                .build();

        denunciaExemplo = Denuncia.builder()
                .id(200L)
                .protocolo("DEN-" + ano + "-54321")
                .denunciante(aluno)
                .denunciado(professor)
                .motivo("Atraso excessivo")
                .descricao("O professor não compareceu no horário combinado.")
                .status(StatusDenuncia.PENDENTE)
                .criadaEm(Instant.now())
                .dataAtualizacao(Instant.now())
                .build();
    }

    @Test
    @DisplayName("[BACK-015] Deve criar ticket com protocolo no formato SUP-YYYY-XXXXX e status ABERTO")
    void deveCriarTicketComProtocoloOficial() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(aluno));
        when(ticketRepository.save(any(TicketSuporte.class))).thenAnswer(invocation -> {
            TicketSuporte t = invocation.getArgument(0);
            t.setId(101L);
            return t;
        });

        CriarTicketDTO dto = CriarTicketDTO.builder()
                .titulo("Problema com pagamento via Pix")
                .descricao("O valor não foi creditado na carteira.")
                .categoria(CategoriaTicket.PAGAMENTO)
                .prioridade(PrioridadeTicket.ALTA)
                .build();

        TicketSuporteDTO criado = suporteService.criarTicket(10L, dto);

        assertNotNull(criado);
        assertEquals(101L, criado.getId());
        assertEquals(10L, criado.getUsuarioId());
        assertEquals("Problema com pagamento via Pix", criado.getTitulo());
        assertEquals(CategoriaTicket.PAGAMENTO, criado.getCategoria());
        assertEquals(StatusTicket.ABERTO, criado.getStatus());

        int anoAtual = LocalDate.now().getYear();
        assertTrue(criado.getProtocolo().matches("^SUP-" + anoAtual + "-\\d{5}$"),
                "O protocolo deve seguir o padrão auditável SUP-YYYY-XXXXX. Gerado: " + criado.getProtocolo());

        verify(notificacaoService, times(1)).criarNotificacao(
                eq(aluno),
                contains("Ticket criado: " + criado.getProtocolo()),
                anyString(),
                eq(TipoNotificacao.SUPORTE),
                eq(101L)
        );
    }

    @Test
    @DisplayName("[BACK-015] Deve listar tickets do usuário autenticado em ordem cronológica decrescente")
    void deveListarTicketsDoUsuario() {
        when(ticketRepository.findByUsuarioIdOrderByCriadoEmDesc(10L))
                .thenReturn(Collections.singletonList(ticketAluno));

        List<TicketSuporteDTO> meusTickets = suporteService.listarMeusTickets(10L);

        assertNotNull(meusTickets);
        assertEquals(1, meusTickets.size());
        assertEquals(100L, meusTickets.get(0).getId());
        assertEquals("SUP-" + LocalDate.now().getYear() + "-12345", meusTickets.get(0).getProtocolo());
        verify(ticketRepository, times(1)).findByUsuarioIdOrderByCriadoEmDesc(10L);
    }

    @Test
    @DisplayName("[BACK-015] Deve obter detalhes do chamado por participante legítimo")
    void deveObterDetalhesDoChamadoPorDono() {
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticketAluno));

        TicketSuporteDTO detalhe = suporteService.obterTicket(100L, 10L, false);

        assertNotNull(detalhe);
        assertEquals(100L, detalhe.getId());
        assertEquals(10L, detalhe.getUsuarioId());
        assertEquals("Dúvida sobre cancelamento", detalhe.getTitulo());
    }

    @Test
    @DisplayName("[BACK-015] Deve permitir que administrador visualize qualquer ticket")
    void devePermitirQueAdminVisualizeQualquerTicket() {
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticketAluno));

        TicketSuporteDTO detalhe = suporteService.obterTicket(100L, 1L, true);

        assertNotNull(detalhe);
        assertEquals(100L, detalhe.getId());
        assertEquals("Ana Clara", detalhe.getUsuarioNome());
    }

    @Test
    @DisplayName("[BACK-015] Deve bloquear com AccessDeniedException tentativa de leitura de ticket de terceiro")
    void deveBloquearLeituraDeTicketAlheio() {
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticketAluno));

        assertThrows(AccessDeniedException.class, () -> suporteService.obterTicket(100L, 99L, false));
    }

    @Test
    @DisplayName("[BACK-015] Deve responder ticket com sucesso e atualizar status para RESOLVIDO")
    void deveResponderTicketComSucesso() {
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticketAluno));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(ticketRepository.save(any(TicketSuporte.class))).thenAnswer(i -> i.getArgument(0));

        ResponderTicketDTO respostaDto = ResponderTicketDTO.builder()
                .resposta("A política de cancelamento permite estorno total até 2h antes da aula.")
                .novoStatus(StatusTicket.RESOLVIDO)
                .build();

        TicketSuporteDTO respondido = suporteService.responderTicket(100L, 1L, respostaDto);

        assertNotNull(respondido);
        assertEquals(StatusTicket.RESOLVIDO, respondido.getStatus());
        assertEquals("A política de cancelamento permite estorno total até 2h antes da aula.", respondido.getResposta());
        assertEquals("Admin Geral", respondido.getRespondidoPorNome());
        assertNotNull(respondido.getRespondidoEm());

        verify(notificacaoService, times(1)).criarNotificacao(
                eq(aluno),
                contains("Resposta ao Ticket"),
                contains("suporte respondeu"),
                eq(TipoNotificacao.SUPORTE),
                eq(100L)
        );
    }

    // =========================================================================
    // TESTES UNITÁRIOS DE DENÚNCIAS E MODERAÇÃO (BACK-016)
    // =========================================================================

    @Test
    @DisplayName("[BACK-016] Deve criar denúncia com protocolo auditável DEN-YYYY-XXXXX e status PENDENTE")
    void deveCriarDenunciaComProtocoloOficial() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(aluno));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(professor));
        when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(invocation -> {
            Denuncia d = invocation.getArgument(0);
            d.setId(301L);
            return d;
        });

        CriarDenunciaDTO dto = CriarDenunciaDTO.builder()
                .denunciadoId(20L)
                .motivo("Conduta Antiprofissional")
                .descricao("O professor utilizou termos impróprios durante o atendimento.")
                .build();

        DenunciaDTO criada = suporteService.criarDenuncia(10L, dto);

        assertNotNull(criada);
        assertEquals(301L, criada.getId());
        assertEquals(10L, criada.getDenuncianteId());
        assertEquals(20L, criada.getDenunciadoId());
        assertEquals("Conduta Antiprofissional", criada.getMotivo());
        assertEquals(StatusDenuncia.PENDENTE, criada.getStatus());

        int anoAtual = LocalDate.now().getYear();
        assertTrue(criada.getProtocolo().matches("^DEN-" + anoAtual + "-\\d{5}$"),
                "O protocolo de ouvidoria deve seguir o padrão DEN-YYYY-XXXXX. Gerado: " + criada.getProtocolo());

        verify(notificacaoService, times(1)).criarNotificacao(
                eq(aluno),
                contains("Denúncia Registrada: " + criada.getProtocolo()),
                anyString(),
                eq(TipoNotificacao.SISTEMA),
                eq(301L)
        );
    }

    @Test
    @DisplayName("[BACK-016] Deve bloquear auto-denúncia com RegraNegocioException (RN-001)")
    void deveBloquearAutoDenuncia() {
        CriarDenunciaDTO dto = CriarDenunciaDTO.builder()
                .denunciadoId(10L) // denunciante tentando denunciar a si mesmo
                .motivo("Tentativa inválida")
                .descricao("Descrição de auto-denúncia")
                .build();

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () ->
                suporteService.criarDenuncia(10L, dto));

        assertTrue(ex.getMessage().contains("Não é permitido denunciar o próprio perfil"));
        verify(denunciaRepository, never()).save(any());
    }

    @Test
    @DisplayName("[BACK-016] Deve moderar denúncia com sucesso por administrador (julgamento PROCEDENTE)")
    void deveModerarDenunciaComSucessoComoAdmin() {
        when(denunciaRepository.findById(200L)).thenReturn(Optional.of(denunciaExemplo));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(denunciaRepository.save(any(Denuncia.class))).thenAnswer(i -> i.getArgument(0));

        ModerarDenunciaDTO moderacaoDto = ModerarDenunciaDTO.builder()
                .parecerModerador("Ocorrência apurada e confirmada através da gravação da sessão.")
                .novoStatus(StatusDenuncia.PROCEDENTE)
                .build();

        DenunciaDTO moderada = suporteService.moderarDenuncia(200L, 1L, moderacaoDto);

        assertNotNull(moderada);
        assertEquals(StatusDenuncia.PROCEDENTE, moderada.getStatus());
        assertEquals("Ocorrência apurada e confirmada através da gravação da sessão.", moderada.getParecerModerador());
        assertEquals("Admin Geral", moderada.getAnalisadoPorNome());
        assertNotNull(moderada.getAnalisadaEm());

        verify(notificacaoService, times(1)).criarNotificacao(
                eq(aluno),
                contains("Moderação de Denúncia: " + denunciaExemplo.getProtocolo()),
                contains("PROCEDENTE"),
                eq(TipoNotificacao.SISTEMA),
                eq(200L)
        );
    }
}
