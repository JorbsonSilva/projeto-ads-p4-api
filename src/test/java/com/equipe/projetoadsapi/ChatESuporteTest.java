package com.equipe.projetoadsapi;

import com.equipe.projetoadsapi.dto.CriarDenunciaDTO;
import com.equipe.projetoadsapi.dto.CriarTicketDTO;
import com.equipe.projetoadsapi.dto.EnviarMensagemDTO;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import com.equipe.projetoadsapi.service.ChatService;
import com.equipe.projetoadsapi.service.NotificacaoService;
import com.equipe.projetoadsapi.service.SuporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes Unitários de Chat Interno, Suporte e Denúncias.
 * Rastreabilidade: Fase 7 - f7-t2, f7-t3, f7-t4, f7-t7
 */
@ExtendWith(MockitoExtension.class)
public class ChatESuporteTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private ConversaRepository conversaRepository;

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private TicketSuporteRepository ticketRepository;

    @Mock
    private DenunciaRepository denunciaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private ChatService chatService;

    @InjectMocks
    private SuporteService suporteService;

    private Usuario aluno;
    private Usuario professor;
    private Agendamento agendamento;

    @BeforeEach
    void setUp() {
        aluno = Usuario.builder().id(10L).nome("Aluno Chat").email("aluno@orienta.com").build();
        professor = Usuario.builder().id(20L).nome("Prof Chat").email("prof@orienta.com").build();
        agendamento = Agendamento.builder()
                .id(100L)
                .aluno(aluno)
                .professor(professor)
                .status(StatusAgendamento.CONFIRMADO)
                .build();
    }

    @Test
    @DisplayName("Deve rejeitar acesso ao chat de usuário que não faz parte da aula (Fail-closed)")
    void testBloqueioAcessoChatTerceiro() {
        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamento));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            chatService.obterOuCriarConversa(100L, 999L);
        });

        assertTrue(ex.getMessage().contains("Acesso negado: apenas o aluno ou o professor vinculados à aula"));
    }

    @Test
    @DisplayName("Deve permitir envio de mensagem e gerar notificação interna para o destinatário")
    void testEnvioMensagemSucessoENotificacao() {
        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamento));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(aluno));

        Conversa conversa = Conversa.builder().id(50L).agendamento(agendamento).ativa(true).build();
        when(conversaRepository.findByAgendamentoId(100L)).thenReturn(Optional.of(conversa));
        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(i -> {
            Mensagem m = i.getArgument(0);
            m.setId(1L);
            return m;
        });

        EnviarMensagemDTO dto = EnviarMensagemDTO.builder()
                .remetenteId(10L)
                .conteudo("Olá Professor, nos vemos na aula presencial!")
                .build();

        var res = chatService.enviarMensagem(100L, dto);

        assertNotNull(res);
        assertEquals("Olá Professor, nos vemos na aula presencial!", res.getConteudo());
        verify(notificacaoService, times(1)).criarNotificacao(
                eq(professor),
                contains("Nova mensagem"),
                anyString(),
                eq(TipoNotificacao.CHAT),
                eq(100L)
        );
    }

    @Test
    @DisplayName("Deve rejeitar envio de mensagem em agendamento já cancelado")
    void testBloqueioMensagemAgendamentoCancelado() {
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        when(agendamentoRepository.findById(100L)).thenReturn(Optional.of(agendamento));

        EnviarMensagemDTO dto = EnviarMensagemDTO.builder()
                .remetenteId(10L)
                .conteudo("Teste")
                .build();

        assertThrows(RegraNegocioException.class, () -> {
            chatService.enviarMensagem(100L, dto);
        });
    }

    @Test
    @DisplayName("Deve gerar protocolo único na abertura de ticket de suporte e status ABERTO")
    void testCriacaoTicketSuporte() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(aluno));
        when(ticketRepository.save(any(TicketSuporte.class))).thenAnswer(i -> {
            TicketSuporte t = i.getArgument(0);
            t.setId(300L);
            return t;
        });

        CriarTicketDTO dto = CriarTicketDTO.builder()
                .titulo("Dúvida sobre preço congelado")
                .descricao("Gostaria de entender se o valor da mentoria muda após aceite.")
                .categoria(CategoriaTicket.FINANCEIRO)
                .build();

        var ticket = suporteService.criarTicket(10L, dto);

        assertNotNull(ticket);
        assertTrue(ticket.getProtocolo().startsWith("SUP-"));
        assertEquals(StatusTicket.ABERTO, ticket.getStatus());
        verify(notificacaoService, times(1)).criarNotificacao(
                eq(aluno),
                contains("Ticket criado"),
                anyString(),
                eq(TipoNotificacao.SUPORTE),
                eq(300L)
        );
    }

    @Test
    @DisplayName("Deve impedir que um usuário denuncie o próprio perfil")
    void testBloqueioAutoDenuncia() {
        CriarDenunciaDTO dto = CriarDenunciaDTO.builder()
                .denunciadoId(10L)
                .motivo("Conduta")
                .descricao("Tentativa inválida")
                .build();

        RegraNegocioException ex = assertThrows(RegraNegocioException.class, () -> {
            suporteService.criarDenuncia(10L, dto);
        });

        assertTrue(ex.getMessage().contains("Não é permitido denunciar o próprio perfil"));
    }
}
