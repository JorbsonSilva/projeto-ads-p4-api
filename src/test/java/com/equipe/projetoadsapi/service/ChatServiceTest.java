package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.ConversaDTO;
import com.equipe.projetoadsapi.dto.EnviarMensagemDTO;
import com.equipe.projetoadsapi.dto.MensagemDTO;
import com.equipe.projetoadsapi.dto.MensagemRequestDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.AgendamentoRepository;
import com.equipe.projetoadsapi.repository.ConversaRepository;
import com.equipe.projetoadsapi.repository.MensagemRepository;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o serviço de Chat, Mensagens e Status de Leitura (BACK-013, BACK-014).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-014)
 */
@ExtendWith(MockitoExtension.class)
public class ChatServiceTest {

    @Mock
    private AgendamentoRepository agendamentoRepository;

    @Mock
    private ConversaRepository conversaRepository;

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private NotificacaoService notificacaoService;

    @InjectMocks
    private ChatService chatService;

    private Usuario aluno;
    private Usuario professorLucas;
    private Usuario professoraMariana;
    private Usuario usuarioTerceiro;
    private Agendamento agendamento1;
    private Agendamento agendamento2;
    private Conversa conversa1;
    private Conversa conversa2;

    @BeforeEach
    void setUp() {
        aluno = Usuario.builder()
                .id(10L)
                .nome("Ana Clara")
                .email("ana@orienta.com.br")
                .fotoUrl("https://orienta.com.br/fotos/ana.jpg")
                .papel(PapelUsuario.ROLE_ALUNO)
                .build();

        professorLucas = Usuario.builder()
                .id(20L)
                .nome("Prof. Lucas Rocha")
                .email("lucas@orienta.com.br")
                .fotoUrl("https://orienta.com.br/fotos/lucas.jpg")
                .papel(PapelUsuario.ROLE_PROFESSOR)
                .build();

        professoraMariana = Usuario.builder()
                .id(30L)
                .nome("Profa. Mariana Costa")
                .email("mariana@orienta.com.br")
                .fotoUrl("https://orienta.com.br/fotos/mariana.jpg")
                .papel(PapelUsuario.ROLE_PROFESSOR)
                .build();

        usuarioTerceiro = Usuario.builder()
                .id(99L)
                .nome("Carlos Intruso")
                .email("carlos@orienta.com.br")
                .papel(PapelUsuario.ROLE_ALUNO)
                .build();

        agendamento1 = Agendamento.builder()
                .id(101L)
                .aluno(aluno)
                .professor(professorLucas)
                .assunto("Matemática Avançada")
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        agendamento2 = Agendamento.builder()
                .id(102L)
                .aluno(aluno)
                .professor(professoraMariana)
                .assunto("Física Mecânica")
                .status(StatusAgendamento.CONFIRMADO)
                .build();

        conversa1 = Conversa.builder()
                .id(1L)
                .agendamento(agendamento1)
                .ativa(true)
                .criadaEm(Instant.now().minus(2, ChronoUnit.DAYS))
                .atualizadaEm(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        conversa2 = Conversa.builder()
                .id(2L)
                .agendamento(agendamento2)
                .ativa(true)
                .criadaEm(Instant.now().minus(3, ChronoUnit.DAYS))
                .atualizadaEm(Instant.now().minus(2, ChronoUnit.HOURS))
                .build();
    }

    @Test
    @DisplayName("[BACK-013] Deve listar conversas ativas ordenadas por data da última mensagem (decrescente)")
    void deveListarConversasOrdenadasPorDataUltimaMensagemDecrescente() {
        when(usuarioRepository.existsById(10L)).thenReturn(true);
        when(conversaRepository.findByUsuarioParticipante(10L))
                .thenReturn(Arrays.asList(conversa2, conversa1));

        Instant agoraMenos10Min = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant agoraMenos2Horas = Instant.now().minus(2, ChronoUnit.HOURS);

        Mensagem msgRecente = Mensagem.builder()
                .id(201L)
                .conversa(conversa1)
                .remetente(professorLucas)
                .conteudo("Combinado, te espero na sala às 14h!")
                .enviadaEm(agoraMenos10Min)
                .lida(false)
                .build();

        Mensagem msgAntiga = Mensagem.builder()
                .id(202L)
                .conversa(conversa2)
                .remetente(professoraMariana)
                .conteudo("Favor trazer a lista de exercícios resolvida.")
                .enviadaEm(agoraMenos2Horas)
                .lida(true)
                .build();

        when(mensagemRepository.findFirstByConversaIdOrderByEnviadaEmDesc(1L))
                .thenReturn(Optional.of(msgRecente));
        when(mensagemRepository.findFirstByConversaIdOrderByEnviadaEmDesc(2L))
                .thenReturn(Optional.of(msgAntiga));

        when(mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(1L, 10L))
                .thenReturn(1L);
        when(mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(2L, 10L))
                .thenReturn(0L);

        List<ConversaDTO> resultado = chatService.listarConversas(10L);

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        ConversaDTO primeira = resultado.get(0);
        assertEquals(1L, primeira.getId());
        assertEquals("Prof. Lucas Rocha", primeira.getNomeInterlocutor());
        assertEquals("Combinado, te espero na sala às 14h!", primeira.getUltimaMensagem());
    }

    @Test
    @DisplayName("[BACK-014] Deve enviar mensagem com sucesso por participante legítimo e notificar outra parte")
    void deveEnviarMensagemComSucessoPorParticipanteLegitimo() {
        when(agendamentoRepository.findById(101L)).thenReturn(Optional.of(agendamento1));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(aluno));
        when(conversaRepository.findByAgendamentoId(101L)).thenReturn(Optional.of(conversa1));

        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> {
            Mensagem m = invocation.getArgument(0);
            m.setId(501L);
            return m;
        });

        EnviarMensagemDTO dto = EnviarMensagemDTO.builder()
                .remetenteId(10L)
                .conteudo("Professor, preparei as dúvidas da lista!")
                .tipo("TEXTO")
                .build();

        MensagemDTO enviada = chatService.enviarMensagem(101L, dto);

        assertNotNull(enviada);
        assertEquals(501L, enviada.getId());
        assertEquals(1L, enviada.getConversaId());
        assertEquals(101L, enviada.getAgendamentoId());
        assertEquals("Professor, preparei as dúvidas da lista!", enviada.getConteudo());
        assertEquals("Ana Clara", enviada.getRemetenteNome());
        assertEquals("ALUNO", enviada.getRemetentePapel());
        assertFalse(enviada.getLida());

        // Verifica notificação enviada ao professor Lucas
        verify(notificacaoService, times(1)).criarNotificacao(
                eq(professorLucas),
                contains("Nova mensagem de Ana Clara"),
                contains("Professor, preparei as dúvidas"),
                eq(TipoNotificacao.CHAT),
                eq(101L)
        );
        verify(conversaRepository, times(1)).save(conversa1);
    }

    @Test
    @DisplayName("[BACK-014] Deve enviar mensagem unificada com anexo de documento (MensagemRequestDTO)")
    void deveEnviarMensagemUnificadaComAnexo() {
        when(agendamentoRepository.findById(101L)).thenReturn(Optional.of(agendamento1));
        when(usuarioRepository.findById(20L)).thenReturn(Optional.of(professorLucas));
        when(conversaRepository.findByAgendamentoId(101L)).thenReturn(Optional.of(conversa1));

        when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(invocation -> {
            Mensagem m = invocation.getArgument(0);
            m.setId(601L);
            return m;
        });

        MensagemRequestDTO request = MensagemRequestDTO.builder()
                .agendamentoId(101L)
                .remetenteId(20L)
                .conteudo("Segue o material de apoio em PDF para a mentoria.")
                .tipo("ARQUIVO")
                .anexoUrl("https://orienta.com.br/anexos/calculo1.pdf")
                .nomeArquivo("calculo1.pdf")
                .tamanhoArquivo("3.4 MB")
                .build();

        MensagemDTO salva = chatService.enviarMensagemUnificada(request);

        assertNotNull(salva);
        assertEquals(601L, salva.getId());
        assertEquals("ARQUIVO", salva.getTipo());
        assertEquals("https://orienta.com.br/anexos/calculo1.pdf", salva.getAnexoUrl());
        assertEquals("calculo1.pdf", salva.getNomeArquivo());
        assertEquals("3.4 MB", salva.getTamanhoArquivo());
        assertEquals("PROFESSOR", salva.getRemetentePapel());

        // Notificação disparada para o aluno
        verify(notificacaoService, times(1)).criarNotificacao(
                eq(aluno),
                contains("Nova mensagem de Prof. Lucas Rocha"),
                anyString(),
                eq(TipoNotificacao.CHAT),
                eq(101L)
        );
    }

    @Test
    @DisplayName("[BACK-014] Deve rejeitar envio de mensagem por usuário terceiro intruso (HTTP 403 AccessDenied)")
    void deveRejeitarEnvioDeMensagemPorUsuarioNaoParticipante() {
        when(agendamentoRepository.findById(101L)).thenReturn(Optional.of(agendamento1));

        EnviarMensagemDTO dto = EnviarMensagemDTO.builder()
                .remetenteId(99L) // Carlos Intruso
                .conteudo("Tentando invadir conversa alheia")
                .build();

        assertThrows(AccessDeniedException.class, () -> chatService.enviarMensagem(101L, dto));
        verify(mensagemRepository, never()).save(any());
        verify(notificacaoService, never()).criarNotificacao(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("[BACK-014] Deve marcar mensagens recebidas como lidas automaticamente na consulta do histórico")
    void deveMarcarMensagensComoLidasAutomaticamenteAoConsultarHistorico() {
        when(agendamentoRepository.findById(101L)).thenReturn(Optional.of(agendamento1));
        when(conversaRepository.findByAgendamentoId(101L)).thenReturn(Optional.of(conversa1));

        Instant enviada = Instant.now().minus(5, ChronoUnit.MINUTES);
        Instant lidaEm = Instant.now();

        Mensagem m1 = Mensagem.builder()
                .id(1L)
                .conversa(conversa1)
                .remetente(professorLucas)
                .conteudo("Oi Ana!")
                .enviadaEm(enviada)
                .lida(true)
                .lidaEm(lidaEm)
                .build();

        when(mensagemRepository.findByConversaIdOrderByEnviadaEmAsc(1L))
                .thenReturn(Collections.singletonList(m1));

        List<MensagemDTO> mensagens = chatService.listarMensagens(101L, 10L);

        assertNotNull(mensagens);
        assertEquals(1, mensagens.size());
        assertTrue(mensagens.get(0).getLida());
        assertTrue(mensagens.get(0).getConfirmadaLeitura());

        // Verifica que o repositório executou o bulk update com timestamp
        verify(mensagemRepository, times(1)).marcarMensagensRecebidasComoLidas(eq(1L), eq(10L), any(Instant.class));
    }

    @Test
    @DisplayName("[BACK-014] Deve recuperar histórico de mensagens com paginação cronológica")
    void deveRecuperarHistoricoPaginadoDeMensagens() {
        when(agendamentoRepository.findById(101L)).thenReturn(Optional.of(agendamento1));
        when(conversaRepository.findByAgendamentoId(101L)).thenReturn(Optional.of(conversa1));

        Instant t1 = Instant.now().minus(10, ChronoUnit.MINUTES);
        Instant t2 = Instant.now().minus(5, ChronoUnit.MINUTES);

        Mensagem m1 = Mensagem.builder().id(1L).conversa(conversa1).remetente(aluno).conteudo("Msg 1").enviadaEm(t1).lida(true).build();
        Mensagem m2 = Mensagem.builder().id(2L).conversa(conversa1).remetente(professorLucas).conteudo("Msg 2").enviadaEm(t2).lida(true).build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by("enviadaEm").ascending());
        Page<Mensagem> pageMock = new PageImpl<>(Arrays.asList(m1, m2), pageable, 2);

        when(mensagemRepository.findByConversaIdOrderByEnviadaEmAsc(eq(1L), eq(pageable)))
                .thenReturn(pageMock);

        Page<MensagemDTO> resultado = chatService.listarMensagensPaginadasPorAgendamento(101L, 10L, pageable);

        assertNotNull(resultado);
        assertEquals(2, resultado.getTotalElements());
        assertEquals("Msg 1", resultado.getContent().get(0).getConteudo());
        assertEquals("Msg 2", resultado.getContent().get(1).getConteudo());
    }

    @Test
    @DisplayName("[BACK-014] Deve atualizar em lote status de leitura via marcarMensagensComoLidas (duplo tique)")
    void deveAtualizarEmLoteStatusDeLeitura() {
        when(conversaRepository.findById(1L)).thenReturn(Optional.of(conversa1));
        when(mensagemRepository.marcarMensagensRecebidasComoLidas(eq(1L), eq(10L), any(Instant.class)))
                .thenReturn(3);

        int totalLidas = chatService.marcarMensagensComoLidas(1L, 10L);

        assertEquals(3, totalLidas);
        verify(mensagemRepository, times(1)).marcarMensagensRecebidasComoLidas(eq(1L), eq(10L), any(Instant.class));
        verify(conversaRepository, times(1)).save(conversa1);
    }
}
