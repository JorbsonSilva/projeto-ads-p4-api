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
 * SERVIÇO DE SUPORTE INTERNO, TICKETS COM PROTOCOLO AUDITÁVEL E MODERAÇÃO DE CONDUTA.
 * Rastreabilidade: Fase 7 - Bloco K (BACK-015)
 * 
 * Regras de Negócio e Segurança:
 * 1. Protocolo auditável gerado no formato SUP-YYYY-XXXXX (ex: SUP-2026-10492).
 * 2. Isolamento por usuário: usuários comuns visualizam apenas seus próprios chamados.
 * 3. Tentativas de acesso a tickets de terceiros resultam em AccessDeniedException (HTTP 403 Forbidden).
 * 4. Fila geral e resposta/parecer restritos exclusivamente ao papel de administrador (ROLE_ADMIN).
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
    // DENÚNCIAS E MODERAÇÃO (f7-t7)
    // ==========================================

    @Transactional
    public DenunciaDTO criarDenuncia(Long denuncianteId, CriarDenunciaDTO dto) {
        if (denuncianteId.equals(dto.getDenunciadoId())) {
            throw new RegraNegocioException("Não é permitido denunciar o próprio perfil.");
        }

        Usuario denunciante = usuarioRepository.findById(denuncianteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Denunciante não encontrado: " + denuncianteId));

        Usuario denunciado = usuarioRepository.findById(dto.getDenunciadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário denunciado não encontrado: " + dto.getDenunciadoId()));

        Agendamento agendamento = null;
        if (dto.getAgendamentoId() != null) {
            agendamento = agendamentoRepository.findById(dto.getAgendamentoId()).orElse(null);
        }

        Denuncia denuncia = Denuncia.builder()
                .denunciante(denunciante)
                .denunciado(denunciado)
                .agendamento(agendamento)
                .motivo(dto.getMotivo().trim())
                .descricao(dto.getDescricao().trim())
                .status(StatusDenuncia.PENDENTE)
                .build();

        Denuncia salva = denunciaRepository.save(denuncia);

        notificacaoService.criarNotificacao(
                denunciante,
                "Denúncia Registrada",
                "Sua manifestação foi recebida e será analisada com rigor pela equipe de moderação.",
                TipoNotificacao.SISTEMA,
                salva.getId()
        );

        return DenunciaDTO.fromEntity(salva);
    }

    @Transactional(readOnly = true)
    public List<DenunciaDTO> listarDenuncias(StatusDenuncia status) {
        List<Denuncia> list = status != null 
                ? denunciaRepository.findByStatusOrderByCriadaEmDesc(status)
                : denunciaRepository.findAll();
        return list.stream().map(DenunciaDTO::fromEntity).collect(Collectors.toList());
    }
}
