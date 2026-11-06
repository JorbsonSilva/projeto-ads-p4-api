package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.ConversaDTO;
import com.equipe.projetoadsapi.dto.EnviarMensagemDTO;
import com.equipe.projetoadsapi.dto.MensagemDTO;
import com.equipe.projetoadsapi.dto.MensagemRequestDTO;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import com.equipe.projetoadsapi.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVIÇO DE CHAT CONTEXTUALIZADO À AULA E COMUNICAÇÃO BILATERAL.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-013, BACK-014)
 * 
 * Regras de Negócio e Segurança:
 * 1. Chat é restrito a aluno e professor do agendamento (isolamento e anti-ID spoofing).
 * 2. Bloqueio imediato para aulas canceladas ou recusadas.
 * 3. Confirmação de leitura (duplo tique) automática na leitura ou explícita via PATCH.
 * 4. Suporte a metadados de anexos (imagens, documentos) e paginação cronológica.
 * 5. Não altera status do agendamento diretamente.
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

    /**
     * Recupera ou inicializa a conversa vinculada a um agendamento.
     */
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

    /**
     * Recupera a conversa por seu ID direto, validando se o usuário é participante.
     */
    @Transactional(readOnly = true)
    public ConversaDTO obterConversaPorId(Long conversaId, Long usuarioId) {
        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada: " + conversaId));

        validarParticipanteDaAula(conversa.getAgendamento(), usuarioId);

        Mensagem ultima = mensagemRepository.findFirstByConversaIdOrderByEnviadaEmDesc(conversa.getId()).orElse(null);
        long naoLidas = mensagemRepository.countByConversaIdAndRemetenteIdNotAndLidaFalse(conversa.getId(), usuarioId);
        return ConversaDTO.fromEntity(conversa, usuarioId, naoLidas, ultima);
    }

    /**
     * Lista o histórico completo de mensagens de um agendamento e marca as recebidas como lidas.
     */
    @Transactional
    public List<MensagemDTO> listarMensagens(Long agendamentoId, Long usuarioId) {
        ConversaDTO conversaDto = obterOuCriarConversa(agendamentoId, usuarioId);

        // Marca como lidas as mensagens recebidas da outra parte (duplo tique)
        mensagemRepository.marcarMensagensRecebidasComoLidas(conversaDto.getId(), usuarioId, Instant.now());

        return mensagemRepository.findByConversaIdOrderByEnviadaEmAsc(conversaDto.getId())
                .stream()
                .map(MensagemDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lista o histórico completo de mensagens de uma conversa direta e marca como lidas.
     */
    @Transactional
    public List<MensagemDTO> listarMensagensPorConversa(Long conversaId, Long usuarioId) {
        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada: " + conversaId));

        validarParticipanteDaAula(conversa.getAgendamento(), usuarioId);

        // Marca como lidas as mensagens recebidas
        mensagemRepository.marcarMensagensRecebidasComoLidas(conversaId, usuarioId, Instant.now());

        return mensagemRepository.findByConversaIdOrderByEnviadaEmAsc(conversaId)
                .stream()
                .map(MensagemDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Recupera histórico paginado de mensagens por agendamento em ordem cronológica (BACK-014).
     */
    @Transactional
    public Page<MensagemDTO> listarMensagensPaginadasPorAgendamento(Long agendamentoId, Long usuarioId, Pageable pageable) {
        ConversaDTO conversaDto = obterOuCriarConversa(agendamentoId, usuarioId);

        // Marca como lidas as mensagens recebidas da outra parte (duplo tique)
        mensagemRepository.marcarMensagensRecebidasComoLidas(conversaDto.getId(), usuarioId, Instant.now());

        Page<Mensagem> pagina = mensagemRepository.findByConversaIdOrderByEnviadaEmAsc(conversaDto.getId(), pageable);
        return pagina.map(MensagemDTO::fromEntity);
    }

    /**
     * Recupera histórico paginado de mensagens por conversa em ordem cronológica (BACK-014).
     */
    @Transactional
    public Page<MensagemDTO> listarMensagensPaginadasPorConversa(Long conversaId, Long usuarioId, Pageable pageable) {
        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada: " + conversaId));

        validarParticipanteDaAula(conversa.getAgendamento(), usuarioId);

        // Marca como lidas as mensagens recebidas
        mensagemRepository.marcarMensagensRecebidasComoLidas(conversaId, usuarioId, Instant.now());

        Page<Mensagem> pagina = mensagemRepository.findByConversaIdOrderByEnviadaEmAsc(conversaId, pageable);
        return pagina.map(MensagemDTO::fromEntity);
    }

    /**
     * Envia mensagem vinculada a um agendamento específico.
     */
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
                .tipo(dto.getTipo() != null && !dto.getTipo().isBlank() ? dto.getTipo() : "TEXTO")
                .anexoUrl(dto.getAnexoUrl())
                .nomeArquivo(dto.getNomeArquivo())
                .tamanhoArquivo(dto.getTamanhoArquivo())
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

    /**
     * Envia mensagem usando o DTO unificado MensagemRequestDTO (POST /api/v1/chat/mensagens).
     * Rastreabilidade: Fase 7 - Bloco K (BACK-014)
     */
    @Transactional
    public MensagemDTO enviarMensagemUnificada(MensagemRequestDTO dto) {
        if (dto.getConteudo() == null || dto.getConteudo().trim().isEmpty()) {
            throw new RegraNegocioException("O conteúdo da mensagem não pode ser vazio.");
        }

        // 1. Identificar a Conversa e o Agendamento
        Conversa conversa;
        Agendamento agendamento;

        if (dto.getConversaId() != null) {
            conversa = conversaRepository.findById(dto.getConversaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada: " + dto.getConversaId()));
            agendamento = conversa.getAgendamento();
        } else if (dto.getAgendamentoId() != null) {
            agendamento = agendamentoRepository.findById(dto.getAgendamentoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + dto.getAgendamentoId()));
            conversa = conversaRepository.findByAgendamentoId(agendamento.getId())
                    .orElseGet(() -> conversaRepository.save(Conversa.builder().agendamento(agendamento).ativa(true).build()));
        } else {
            throw new RegraNegocioException("Informe o identificador do agendamento ou da conversa para enviar a mensagem.");
        }

        // 2. Identificar e validar o Remetente
        Long remetenteAuthId = dto.getRemetenteId() != null ? dto.getRemetenteId() : SecurityUtils.getUsuarioAutenticadoId();
        if (remetenteAuthId == null) {
            throw new AccessDeniedException("Usuário remetente não autenticado.");
        }
        final Long remetenteId = remetenteAuthId;

        validarParticipanteDaAula(agendamento, remetenteId);

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO || agendamento.getStatus() == StatusAgendamento.RECUSADO) {
            throw new RegraNegocioException("Não é permitido enviar mensagens para agendamentos cancelados ou recusados.");
        }

        Usuario remetente = usuarioRepository.findById(remetenteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Remetente não encontrado: " + remetenteId));

        // 3. Montar e persistir a mensagem
        Mensagem mensagem = Mensagem.builder()
                .conversa(conversa)
                .remetente(remetente)
                .conteudo(dto.getConteudo().trim())
                .tipo(dto.getTipo() != null && !dto.getTipo().isBlank() ? dto.getTipo() : "TEXTO")
                .anexoUrl(dto.getAnexoUrl())
                .nomeArquivo(dto.getNomeArquivo())
                .tamanhoArquivo(dto.getTamanhoArquivo())
                .lida(false)
                .enviadaEm(Instant.now())
                .build();

        Mensagem salva = mensagemRepository.save(mensagem);

        // 4. Atualizar conversa
        conversa.setAtualizadaEm(Instant.now());
        conversaRepository.save(conversa);

        // 5. Notificar a outra parte
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

    /**
     * Atualiza em lote o status de leitura para lida = true nas mensagens recebidas da conversa.
     * Rastreabilidade: Fase 7 - Bloco K (BACK-014)
     */
    @Transactional
    public int marcarMensagensComoLidas(Long conversaId, Long usuarioId) {
        Conversa conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa não encontrada: " + conversaId));

        validarParticipanteDaAula(conversa.getAgendamento(), usuarioId);

        int totalMarcadas = mensagemRepository.marcarMensagensRecebidasComoLidas(conversaId, usuarioId, Instant.now());
        if (totalMarcadas > 0) {
            conversa.setAtualizadaEm(Instant.now());
            conversaRepository.save(conversa);
        }
        return totalMarcadas;
    }

    /**
     * Atualiza em lote o status de leitura para lida = true nas mensagens recebidas pelo agendamento.
     * Rastreabilidade: Fase 7 - Bloco K (BACK-014)
     */
    @Transactional
    public int marcarMensagensComoLidasPorAgendamento(Long agendamentoId, Long usuarioId) {
        Agendamento agendamento = agendamentoRepository.findById(agendamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Agendamento não encontrado: " + agendamentoId));

        validarParticipanteDaAula(agendamento, usuarioId);

        Conversa conversa = conversaRepository.findByAgendamentoId(agendamentoId)
                .orElse(null);

        if (conversa == null) {
            return 0;
        }

        int totalMarcadas = mensagemRepository.marcarMensagensRecebidasComoLidas(conversa.getId(), usuarioId, Instant.now());
        if (totalMarcadas > 0) {
            conversa.setAtualizadaEm(Instant.now());
            conversaRepository.save(conversa);
        }
        return totalMarcadas;
    }

    private void validarParticipanteDaAula(Agendamento agendamento, Long usuarioId) {
        if (agendamento == null || usuarioId == null) {
            throw new AccessDeniedException("Acesso negado: dados de agendamento ou usuário inválidos.");
        }

        // Administrador tem permissão de moderação
        if (SecurityUtils.hasRole("ROLE_ADMIN")) {
            return;
        }

        boolean isAluno = agendamento.getAluno() != null && agendamento.getAluno().getId().equals(usuarioId);
        boolean isProfessor = agendamento.getProfessor() != null && agendamento.getProfessor().getId().equals(usuarioId);

        if (!isAluno && !isProfessor) {
            throw new AccessDeniedException("Acesso negado: apenas o aluno ou o professor vinculados à aula podem acessar esta conversa.");
        }
    }
}
