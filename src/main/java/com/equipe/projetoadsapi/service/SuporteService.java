package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * SERVIÇO DE SUPORTE INTERNO, TICKETS E CANAL DE DENÚNCIAS / OUVIDORIA.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-015, BACK-016)
 * 
 * Regras de Negócio e Segurança:
 * 1. Protocolo auditável gerado no formato SUP-YYYY-XXXXX para tickets e DEN-YYYY-XXXXX para denúncias.
 * 2. Bloqueio estrito de auto-denúncia (denuncianteId != denunciadoId).
 * 3. Sigilo e proteção da identidade do denunciante.
 * 4. Fila geral de atendimento e moderação restritas exclusivamente a administradores (ROLE_ADMIN).
 */
@Service
public class SuporteService {

    @Autowired
    private TicketSuporteRepository ticketRepository;

    @Autowired
    private DenunciaRepository denunciaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private MensagemRepository mensagemRepository;

    @Autowired
    private NotificacaoService notificacaoService;

    /**
     * Abre um novo chamado de suporte com protocolo único SUP-YYYY-XXXXX.
     * Rastreabilidade: Fase 7 - Bloco K (BACK-015)
     */
    @Transactional
    public TicketSuporteDTO criarTicket(Long usuarioId, CriarTicketDTO dto) {
        if (usuarioId == null) {
            throw new AccessDeniedException("Identificador do usuário não informado.");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + usuarioId));

        int ano = LocalDate.now().getYear();
        int seq = ThreadLocalRandom.current().nextInt(10000, 99999);
        String protocolo = String.format("SUP-%d-%05d", ano, seq);

        TicketSuporte ticket = TicketSuporte.builder()
                .usuario(usuario)
                .protocolo(protocolo)
                .titulo(dto.getTitulo().trim())
                .assunto(dto.getAssunto().trim())
                .descricao(dto.getDescricao().trim())
                .categoria(dto.getCategoria())
                .prioridade(dto.getPrioridade() != null ? dto.getPrioridade() : PrioridadeTicket.MEDIA)
                .status(StatusTicket.ABERTO)
                .build();

        TicketSuporte salvo = ticketRepository.save(ticket);

        notificacaoService.criarNotificacao(
                usuario,
                "Ticket criado: " + salvo.getProtocolo(),
                "Recebemos sua solicitação. O suporte responderá em breve.",
                TipoNotificacao.SUPORTE,
                salvo.getId()
        );

        return TicketSuporteDTO.fromEntity(salvo);
    }

    /**
     * Retorna o histórico de tickets do usuário solicitante em ordem cronológica decrescente.
     */
    @Transactional(readOnly = true)
    public List<TicketSuporteDTO> listarMeusTickets(Long usuarioId) {
        if (usuarioId == null) {
            throw new AccessDeniedException("Identificador do usuário não informado.");
        }
        return ticketRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(TicketSuporteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Fila geral de chamados para administradores.
     */
    @Transactional(readOnly = true)
    public List<TicketSuporteDTO> listarTodosTickets() {
        return ticketRepository.findAllByOrderByCriadoEmDesc()
                .stream()
                .map(TicketSuporteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Detalha um ticket por ID com validação estrita de posse ou privilégio de administrador.
     */
    @Transactional(readOnly = true)
    public TicketSuporteDTO obterTicket(Long id, Long usuarioId, boolean isAdmin) {
        TicketSuporte ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ticket não encontrado: " + id));

        if (!isAdmin) {
            if (usuarioId == null || ticket.getUsuario() == null || !ticket.getUsuario().getId().equals(usuarioId)) {
                throw new AccessDeniedException("Acesso negado: Você não tem permissão para visualizar o chamado de suporte de outro usuário.");
            }
        }

        return TicketSuporteDTO.fromEntity(ticket);
    }

    /**
     * Atribui parecer técnico ou resposta da moderação e atualiza status do ticket (exclusivo para ADMIN).
     */
    @Transactional
    public TicketSuporteDTO responderTicket(Long ticketId, Long adminId, ResponderTicketDTO dto) {
        TicketSuporte ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ticket não encontrado: " + ticketId));

        Usuario admin = null;
        if (adminId != null) {
            admin = usuarioRepository.findById(adminId).orElse(null);
        }

        ticket.setResposta(dto.getResposta().trim());
        ticket.setRespostaAdmin(dto.getResposta().trim());
        ticket.setRespondidoPor(admin);
        ticket.setRespondidoEm(Instant.now());
        ticket.setStatus(dto.getNovoStatus() != null ? dto.getNovoStatus() : StatusTicket.RESOLVIDO);

        TicketSuporte salvo = ticketRepository.save(ticket);

        notificacaoService.criarNotificacao(
                ticket.getUsuario(),
                "Resposta ao Ticket " + ticket.getProtocolo(),
                "O suporte respondeu à sua solicitação. Toque para visualizar o parecer.",
                TipoNotificacao.SUPORTE,
                ticket.getId()
        );

        return TicketSuporteDTO.fromEntity(salvo);
    }

    // ==========================================
    // CANAL DE DENÚNCIAS E OUVIDORIA (BACK-016)
    // ==========================================

    /**
     * Registra nova denúncia na ouvidoria com protocolo auditável DEN-YYYY-XXXXX.
     * Rastreabilidade: Fase 7 - Bloco K (BACK-016)
     */
    @Transactional
    public DenunciaDTO criarDenuncia(Long denuncianteId, CriarDenunciaDTO dto) {
        if (denuncianteId == null) {
            throw new AccessDeniedException("Identificador do denunciante não informado.");
        }

        // Regra de Proteção Ética: Bloqueio estrito de auto-denúncia (RN-001)
        if (dto.getDenunciadoId() != null && denuncianteId.equals(dto.getDenunciadoId())) {
            throw new RegraNegocioException("Não é permitido denunciar o próprio perfil.");
        }

        Usuario denunciante = usuarioRepository.findById(denuncianteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denunciante não encontrado: " + denuncianteId));

        Usuario denunciado = null;
        if (dto.getDenunciadoId() != null) {
            denunciado = usuarioRepository.findById(dto.getDenunciadoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário denunciado não encontrado: " + dto.getDenunciadoId()));
        }

        Agendamento agendamento = null;
        if (dto.getAgendamentoId() != null) {
            agendamento = agendamentoRepository.findById(dto.getAgendamentoId()).orElse(null);
            if (agendamento != null && denunciado == null) {
                // Infere o denunciado a partir do agendamento se não tiver sido passado explicitamente
                denunciado = agendamento.getAluno().getId().equals(denuncianteId)
                        ? agendamento.getProfessor()
                        : agendamento.getAluno();
            }
        }

        Mensagem mensagem = null;
        if (dto.getMensagemId() != null) {
            mensagem = mensagemRepository.findById(dto.getMensagemId()).orElse(null);
        }

        int ano = LocalDate.now().getYear();
        int seq = ThreadLocalRandom.current().nextInt(10000, 99999);
        String protocolo = String.format("DEN-%d-%05d", ano, seq);

        Denuncia denuncia = Denuncia.builder()
                .protocolo(protocolo)
                .denunciante(denunciante)
                .denunciado(denunciado)
                .agendamento(agendamento)
                .mensagem(mensagem)
                .motivo(dto.getMotivo().trim())
                .descricao(dto.getDescricao().trim())
                .status(StatusDenuncia.PENDENTE)
                .criadaEm(Instant.now())
                .dataAtualizacao(Instant.now())
                .build();

        Denuncia salva = denunciaRepository.save(denuncia);

        // Notificação de sigilo e confirmação de protocolo
        notificacaoService.criarNotificacao(
                denunciante,
                "Denúncia Registrada: " + salva.getProtocolo(),
                "Sua manifestação foi recebida sob protocolo " + salva.getProtocolo() + " e será analisada com rigor e sigilo pela moderação.",
                TipoNotificacao.SISTEMA,
                salva.getId()
        );

        return DenunciaDTO.fromEntity(salva);
    }

    /**
     * Lista todas as denúncias ordenadas decrescente por data para a fila da moderação (ADMIN).
     */
    @Transactional(readOnly = true)
    public List<DenunciaDTO> listarDenuncias(StatusDenuncia status) {
        List<Denuncia> list = status != null 
                ? denunciaRepository.findByStatusOrderByCriadaEmDesc(status)
                : denunciaRepository.findAllByOrderByCriadaEmDesc();
        return list.stream().map(DenunciaDTO::fromEntity).collect(Collectors.toList());
    }

    /**
     * Detalha uma denúncia por ID para a equipe de moderação.
     */
    @Transactional(readOnly = true)
    public DenunciaDTO obterDenuncia(Long id) {
        Denuncia denuncia = denunciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denúncia não encontrada: " + id));
        return DenunciaDTO.fromEntity(denuncia);
    }

    /**
     * Aplica parecer técnico e julgamento ético de moderação à denúncia (exclusivo para ADMIN).
     */
    @Transactional
    public DenunciaDTO moderarDenuncia(Long denunciaId, Long moderadorId, ModerarDenunciaDTO dto) {
        Denuncia denuncia = denunciaRepository.findById(denunciaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denúncia não encontrada: " + denunciaId));

        Usuario moderador = null;
        if (moderadorId != null) {
            moderador = usuarioRepository.findById(moderadorId).orElse(null);
        }

        denuncia.setParecerModeracao(dto.getParecerModerador().trim());
        denuncia.setParecerModerador(dto.getParecerModerador().trim());
        denuncia.setStatus(dto.getNovoStatus());
        denuncia.setAnalisadoPor(moderador);
        denuncia.setAnalisadaEm(Instant.now());
        denuncia.setDataAtualizacao(Instant.now());

        Denuncia salva = denunciaRepository.save(denuncia);

        // Notificação de conclusão ao denunciante
        notificacaoService.criarNotificacao(
                denuncia.getDenunciante(),
                "Moderação de Denúncia: " + salva.getProtocolo(),
                "Sua manifestação foi julgada pela moderação. Status final: " + salva.getStatus(),
                TipoNotificacao.SISTEMA,
                salva.getId()
        );

        return DenunciaDTO.fromEntity(salva);
    }
}
