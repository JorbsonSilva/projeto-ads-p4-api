package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.ConversaDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.AgendamentoRepository;
import com.equipe.projetoadsapi.repository.ConversaRepository;
import com.equipe.projetoadsapi.repository.MensagemRepository;
import com.equipe.projetoadsapi.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para o serviço de Chat e Caixa de Entrada Unificada (BACK-013).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-013)
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

        // Conversa 2 foi retornada primeiro pelo banco, mas Conversa 1 tem a mensagem mais recente
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

        // A primeira deve ser a Conversa 1 (mensagem mais recente há 10 min)
        ConversaDTO primeira = resultado.get(0);
        assertEquals(1L, primeira.getId());
        assertEquals(101L, primeira.getAgendamentoId());
        assertEquals("Matemática Avançada", primeira.getDisciplinaNome());
        assertEquals(20L, primeira.getInterlocutorId());
        assertEquals("Prof. Lucas Rocha", primeira.getNomeInterlocutor());
        assertEquals("PROFESSOR", primeira.getPapelInterlocutor());
        assertEquals("https://orienta.com.br/fotos/lucas.jpg", primeira.getFotoUrlInterlocutor());
        assertEquals("Combinado, te espero na sala às 14h!", primeira.getUltimaMensagem());
        assertEquals(agoraMenos10Min, primeira.getDataHoraUltimaMensagem());
        assertFalse(primeira.getEnviadaPorMim());
        assertEquals(1L, primeira.getMensagensNaoLidas());

        // A segunda deve ser a Conversa 2 (mensagem antiga há 2h)
        ConversaDTO segunda = resultado.get(1);
        assertEquals(2L, segunda.getId());
        assertEquals(102L, segunda.getAgendamentoId());
        assertEquals("Física Mecânica", segunda.getDisciplinaNome());
        assertEquals(30L, segunda.getInterlocutorId());
        assertEquals("Profa. Mariana Costa", segunda.getNomeInterlocutor());
        assertEquals("PROFESSOR", segunda.getPapelInterlocutor());
        assertEquals("https://orienta.com.br/fotos/mariana.jpg", segunda.getFotoUrlInterlocutor());
        assertEquals("Favor trazer a lista de exercícios resolvida.", segunda.getUltimaMensagem());
        assertEquals(agoraMenos2Horas, segunda.getDataHoraUltimaMensagem());
        assertEquals(0L, segunda.getMensagensNaoLidas());
    }

    @Test
    @DisplayName("[BACK-013] Deve calcular contagem exata do badge de mensagens não lidas")
    void deveCalcularContagemExataDeMensagensNaoLidas() {
        when(usuarioRepository.existsById(10L)).thenReturn(true);
        when(conversaRepository.findByUsuarioParticipante(10L))
                .thenReturn(Collections.singletonList(conversa1));

        when(mensagemRepository.findFirstByConversaIdOrderByEnviadaEmDesc(1L))
                .thenReturn(Optional.empty());
        when(mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(1L, 10L))
                .thenReturn(5L);

        List<ConversaDTO> resultado = chatService.listarConversas(10L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(5L, resultado.get(0).getMensagensNaoLidas());
        verify(mensagemRepository, times(1)).countByConversaIdAndRemetenteIdNotAndLidaFalse(1L, 10L);
    }

    @Test
    @DisplayName("[BACK-013] Deve garantir isolamento estrito de conversas entre usuários distintos")
    void deveGarantirIsolamentoDeConversasPorUsuario() {
        when(usuarioRepository.existsById(99L)).thenReturn(true);
        when(conversaRepository.findByUsuarioParticipante(99L))
                .thenReturn(Collections.emptyList());

        List<ConversaDTO> resultado = chatService.listarConversas(99L);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(conversaRepository, times(1)).findByUsuarioParticipante(99L);
        verify(conversaRepository, never()).findByUsuarioParticipante(10L);
    }

    @Test
    @DisplayName("[BACK-013] Deve mapear corretamente o aluno como interlocutor quando o usuário logado for o professor")
    void deveMapearAlunoComoInterlocutorQuandoUsuarioForProfessor() {
        when(usuarioRepository.existsById(20L)).thenReturn(true); // Prof. Lucas
        when(conversaRepository.findByUsuarioParticipante(20L))
                .thenReturn(Collections.singletonList(conversa1));

        Mensagem msgAluno = Mensagem.builder()
                .id(301L)
                .conversa(conversa1)
                .remetente(aluno)
                .conteudo("Professor, estou com dúvida no exercício 4.")
                .enviadaEm(Instant.now().minus(5, ChronoUnit.MINUTES))
                .lida(false)
                .build();

        when(mensagemRepository.findFirstByConversaIdOrderByEnviadaEmDesc(1L))
                .thenReturn(Optional.of(msgAluno));
        when(mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(1L, 20L))
                .thenReturn(1L);

        List<ConversaDTO> resultado = chatService.listarConversas(20L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        ConversaDTO dto = resultado.get(0);

        // O interlocutor deve ser a aluna
        assertEquals(10L, dto.getInterlocutorId());
        assertEquals("Ana Clara", dto.getNomeInterlocutor());
        assertEquals("ALUNO", dto.getPapelInterlocutor());
        assertEquals("https://orienta.com.br/fotos/ana.jpg", dto.getFotoUrlInterlocutor());
        assertEquals("Professor, estou com dúvida no exercício 4.", dto.getUltimaMensagem());
        assertFalse(dto.getEnviadaPorMim()); // Enviada pela aluna, não pelo professor
    }

    @Test
    @DisplayName("[BACK-013] Deve lançar RecursoNaoEncontradoException quando usuário não existir")
    void deveLancarExcecaoQuandoUsuarioNaoExistir() {
        when(usuarioRepository.existsById(999L)).thenReturn(false);

        assertThrows(RecursoNaoEncontradoException.class, () -> chatService.listarConversas(999L));
        verify(conversaRepository, never()).findByUsuarioParticipante(any());
    }
}
