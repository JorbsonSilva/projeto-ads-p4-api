package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.ConversaDTO;
import com.equipe.projetoadsapi.dto.EnviarMensagemDTO;
import com.equipe.projetoadsapi.dto.MensagemDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVIÇO DE CHAT CONTEXTUALIZADO À AULA.
 * Rastreabilidade: Fase 7 - f7-t1, f7-t2, f7-t3
 * Regra: Chat é restrito a aluno e professor e JAMAIS altera status de agendamento diretamente.
 */
@Service
public class ChatService {

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private ConversaRepository conversaRepository;

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    /**
     * Retorna a lista unificada de conversas ativas do usuário (aluno ou professor),
     * com dados do interlocutor, última mensagem e contador de mensagens não lidas.
     * As conversas são ordenadas pela data da última mensagem em ordem decrescente.
     * Rastreabilidade: Fase 7 - Bloco K (BACK-013)
     */
    @Transactional(readOnly = true)
    public List<ConversaDTO> listarConversas(Long usuarioId) {
        if (usuarioId == null) {
            throw new IllegalArgumentException("O ID do usuário é obrigatório.");
        }
        if (!usuarioRepository.existsById(usuarioId)) {
            throw new RecursoNaoEncontradoException("Usuário não encontrado: " + usuarioId);
        }

        List<Conversa> conversas = conversaRepository.findByUsuarioParticipante(usuarioId);

        List<ConversaDTO> dtos = conversas.stream().map(conversa -> {
            Mensagem ultima = mensagemRepository.findFirstByConversaIdOrderByEnviadaEmDesc(conversa.getId()).orElse(null);
            long naoLidas = mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(conversa.getId(), usuarioId);
            return ConversaDTO.fromEntity(conversa, usuarioId, naoLidas, ultima);
        }).collect(Collectors.toList());

        // Ordenar conversas ativas por data da última mensagem (decrescente)
        dtos.sort((c1, c2) -> {
            Instant d1 = c1.getDataHoraUltimaMensagem() != null ? c1.getDataHoraUltimaMensagem() : c1.getAtualizadaEm();
            Instant d2 = c2.getDataHoraUltimaMensagem() != null ? c2.getDataHoraUltimaMensagem() : c2.getAtualizadaEm();
            if (d1 == null && d2 == null) return 0;
            if (d1 == null) return 1;
            if (d2 == null) return -1;
            return d2.compareTo(d1);
        });

        return dtos;
    }

    @Transactional
    public ConversaDTO obterOuCriarConversa(Long agendamentoId, Long usuarioId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + agendamentoId));

        validarParticipanteDaAula(agendamento, usuarioId);

        Conversa conversa = conversaRepository.findByAgendamentoId(agendamentoId)
                .orElseGet(() -> {
                    Conversa nova = Conversa.builder()
                            .agendamento(agendamento)
                            .ativa(true)
                            .build();
                    return conversaRepository.save(nova);
                });

        Mensagem ultima = mensagemRepository.findFirstByConversaIdOrderByEnviadaEmDesc(conversa.getId()).orElse(null);
        long naoLidas = mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(conversa.getId(), usuarioId);
        return ConversaDTO.fromEntity(conversa, usuarioId, naoLidas, ultima);
    }

    @Transactional
    public List<MensagemDTO> listarMensagens(Long agendamentoId, Long usuarioId) {
        ConversaDTO conversaDto = obterOuCriarConversa(agendamentoId, usuarioId);

        // Marca como lidas as mensagens recebidas da outra parte
        mensagemRepository.marcarMensagensRecebidasComoLidas(conversaDto.getId(), usuarioId, Instant.now());

        return mensagemRepository.findByConversaIdOrderByEnviadaEmAsc(conversaDto.getId())
                .stream()
                .map(MensagemDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public MensagemDTO enviarMensagem(Long agendamentoId, EnviarMensagemDTO dto) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + agendamentoId));

        validarParticipanteDaAula(agendamento, dto.getRemetenteId());

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO || agendamento.getStatus() == StatusAgendamento.RECUSADO) {
            throw new RegraNegocioException("Não é permitido enviar mensagens para agendamentos cancelados ou recusados.");
        }

        Usuario remetente = usuarioRepository.findById(dto.getRemetenteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Remetente não encontrado: " + dto.getRemetenteId()));

        Conversa conversa = conversaRepository.findByAgendamentoId(agendamentoId)
                .orElseGet(() -> conversaRepository.save(Conversa.builder().agendamento(agendamento).ativa(true).build()));

        Mensagem mensagem = Mensagem.builder()
                .conversa(conversa)
                .remetente(remetente)
                .conteudo(dto.getConteudo().trim())
                .lida(false)
                .enviadaEm(Instant.now())
                .build();

        Mensagem salva = mensagemRepository.save(mensagem);

        // Atualiza timestamp da conversa
        conversa.setAtualizadaEm(Instant.now());
        conversaRepository.save(conversa);

        // Notifica o destinatário (o outro participante da aula)
        Usuario destinatario = agendamento.getAluno().getId().equals(remetente.getId())
                ? agendamento.getProfessor()
                : agendamento.getAluno();

        notificacaoService.criarNotificacao(
                destinatario,
                "Nova mensagem de " + remetente.getNome(),
                salva.getConteudo().length() > 50 ? salva.getConteudo().substring(0, 47) + "..." : salva.getConteudo(),
                TipoNotificacao.CHAT,
                agendamento.getId()
        );

        return MensagemDTO.fromEntity(salva);
    }

    private void validarParticipanteDaAula(Agendamento agendamento, Long usuarioId) {
        boolean isAluno = agendamento.getAluno() != null && agendamento.getAluno().getId().equals(usuarioId);
        boolean isProfessor = agendamento.getProfessor() != null && agendamento.getProfessor().getId().equals(usuarioId);

        if (!isAluno && !isProfessor) {
            throw new RegraNegocioException("Acesso negado: apenas o aluno ou o professor vinculados à aula podem acessar esta conversa.");
        }
    }
}
