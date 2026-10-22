package com.equipe.projetoadsapi.service;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.exception.RecursoNaoEncontradoException;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.model.*;
import com.equipe.projetoadsapi.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SERVIÇO DE SUPORTE INTERNO, TICKETS E MODERAÇÃO DE CONDUTA.
 * Rastreabilidade: Fase 7 - f7-t4, f7-t7
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

    @Transactional
    public TicketSuporteDTO criarTicket(Long usuarioId, CriarTicketDTO dto) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + usuarioId));

        String protocolo = "SUP-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        TicketSuporte ticket = TicketSuporte.builder()
                .usuario(usuario)
                .protocolo(protocolo)
                .titulo(dto.getTitulo().trim())
                .descricao(dto.getDescricao().trim())
                .categoria(dto.getCategoria())
                .prioridade(dto.getPrioridade() != null ? dto.getPrioridade() : PrioridadeTicket.MEDIA)
                .status(StatusTicket.ABERTO)
                .build();

        TicketSuporte salvo = ticketRepository.save(ticket);

        notificacaoService.criarNotificacao(
                usuario,
                "Ticket criado: " + protocolo,
                "Recebemos sua solicitação. O suporte responderá em breve.",
                TipoNotificacao.SUPORTE,
                salvo.getId()
        );

        return TicketSuporteDTO.fromEntity(salvo);
    }

    @Transactional(readOnly = true)
    public List<TicketSuporteDTO> listarMeusTickets(Long usuarioId) {
        return ticketRepository.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                .stream()
                .map(TicketSuporteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketSuporteDTO> listarTodosTickets() {
        return ticketRepository.findAllByOrderByCriadoEmDesc()
                .stream()
                .map(TicketSuporteDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketSuporteDTO obterTicket(Long id, Long usuarioId, boolean isAdmin) {
        TicketSuporte ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ticket não encontrado: " + id));

        if (!isAdmin && !ticket.getUsuario().getId().equals(usuarioId)) {
            throw new RegraNegocioException("Acesso negado ao ticket.");
        }

        return TicketSuporteDTO.fromEntity(ticket);
    }

    @Transactional
    public TicketSuporteDTO responderTicket(Long ticketId, Long adminId, ResponderTicketDTO dto) {
        TicketSuporte ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ticket não encontrado: " + ticketId));

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário administrador não encontrado: " + adminId));

        ticket.setResposta(dto.getResposta().trim());
        ticket.setRespondidoPor(admin);
        ticket.setRespondidoEm(Instant.now());
        ticket.setStatus(dto.getNovoStatus() != null ? dto.getNovoStatus() : StatusTicket.RESOLVIDO);

        TicketSuporte salvo = ticketRepository.save(ticket);

        notificacaoService.criarNotificacao(
                ticket.getUsuario(),
                "Resposta ao Ticket " + ticket.getProtocolo(),
                "O suporte respondeu à sua solicitação. Toque para visualizar.",
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
