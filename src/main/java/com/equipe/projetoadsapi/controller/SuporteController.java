package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.model.StatusDenuncia;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.SecurityUtils;
import com.equipe.projetoadsapi.service.SuporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLLER DE SUPORTE E DENÚNCIAS (v1).
 * Rastreabilidade: Fase 7 - Bloco K (BACK-015) & Tarefas [BACK-003], [BACK-014]
 * 
 * Enforcing de segurança RBAC e Governança:
 * - Usuários comuns abrem tickets atrelados a seu próprio ID com protocolo oficial (SUP-YYYY-XXXXX).
 * - Usuários visualizam exclusivamente seus próprios chamados (bloqueio de cross-reading via HTTP 403 Forbidden).
 * - Fila geral e resposta/parecer de tickets restritos exclusivamente a administradores (ROLE_ADMIN).
 */
@RestController
@RequestMapping({"/api/v1/suporte", "/api/suporte"})
@Tag(name = "Suporte & Moderação (v1)", description = "Abertura de tickets de atendimento e denúncias de conduta com protocolo auditável")
@SecurityRequirement(name = "bearerAuth")
public class SuporteController {

    @Autowired
    private SuporteService suporteService;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Abrir ticket de suporte", description = "Cria um novo chamado categorizado com protocolo único no formato SUP-YYYY-XXXXX.")
    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<TicketSuporteDTO>> criarTicket(
            @RequestParam(required = false) Long usuarioId,
            @Valid @RequestBody CriarTicketDTO dto) {
        Long idAlvo = obterEValidarUsuarioAlvo(usuarioId);
        TicketSuporteDTO ticket = suporteService.criarTicket(idAlvo, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(ticket, "Ticket criado com sucesso! Protocolo: " + ticket.getProtocolo()));
    }

    @Operation(summary = "Listar meus tickets de suporte", description = "Retorna histórico de tickets abertos pelo usuário autenticado em ordem cronológica decrescente.")
    @GetMapping("/tickets/meus")
    public ResponseEntity<ApiResponse<List<TicketSuporteDTO>>> listarMeusTickets(
            @RequestParam(required = false) Long usuarioId) {
        Long idAlvo = obterEValidarUsuarioAlvo(usuarioId);
        List<TicketSuporteDTO> tickets = suporteService.listarMeusTickets(idAlvo);
        return ResponseEntity.ok(ApiResponse.ok(tickets, "Tickets recuperados"));
    }

    @Operation(summary = "Obter detalhes do ticket de suporte", description = "Retorna os detalhes e linha do tempo de atendimento de um chamado específico.")
    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<TicketSuporteDTO>> obterTicket(
            @PathVariable Long id,
            @RequestParam(required = false) Long usuarioId) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        Long idAlvo = (usuarioId != null) ? usuarioId : authenticatedId;
        boolean isAdmin = SecurityUtils.hasRole("ROLE_ADMIN");

        TicketSuporteDTO ticket = suporteService.obterTicket(id, idAlvo, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok(ticket, "Ticket recuperado com sucesso"));
    }

    @Operation(summary = "Listar todos os tickets (ADMIN)", description = "Fila geral de atendimento para administradores da plataforma.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketSuporteDTO>>> listarTodosTickets() {
        List<TicketSuporteDTO> tickets = suporteService.listarTodosTickets();
        return ResponseEntity.ok(ApiResponse.ok(tickets, "Fila de tickets recuperada"));
    }

    @Operation(summary = "Responder ticket de suporte (ADMIN)", description = "Atribui parecer técnico ou resposta da moderação e atualiza status do chamado.")
    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(value = "/tickets/{ticketId}/responder", method = {RequestMethod.PATCH, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<TicketSuporteDTO>> responderTicket(
            @PathVariable Long ticketId,
            @RequestParam(required = false) Long adminId,
            @Valid @RequestBody ResponderTicketDTO dto) {
        Long idAdmin = (adminId != null) ? adminId : SecurityUtils.getUsuarioAutenticadoId();
        TicketSuporteDTO ticket = suporteService.responderTicket(ticketId, idAdmin, dto);
        return ResponseEntity.ok(ApiResponse.ok(ticket, "Resposta enviada e ticket atualizado"));
    }

    @Operation(summary = "Registrar denúncia de usuário", description = "Envia uma denúncia para a equipe de moderação avaliar conduta imprópria.")
    @PostMapping("/denuncias")
    public ResponseEntity<ApiResponse<DenunciaDTO>> criarDenuncia(
            @RequestParam(required = false) Long denuncianteId,
            @Valid @RequestBody CriarDenunciaDTO dto) {
        Long idAlvo = obterEValidarUsuarioAlvo(denuncianteId);
        DenunciaDTO denuncia = suporteService.criarDenuncia(idAlvo, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(denuncia, "Denúncia enviada com sucesso para a moderação"));
    }

    @Operation(summary = "Listar denúncias (ADMIN)", description = "Visualização de denúncias pendentes para a moderação/administração.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/denuncias")
    public ResponseEntity<ApiResponse<List<DenunciaDTO>>> listarDenuncias(
            @RequestParam(required = false) StatusDenuncia status) {
        List<DenunciaDTO> denuncias = suporteService.listarDenuncias(status);
        return ResponseEntity.ok(ApiResponse.ok(denuncias, "Denúncias recuperadas"));
    }

    private Long obterEValidarUsuarioAlvo(Long usuarioId) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        Long idAlvo = (usuarioId != null) ? usuarioId : authenticatedId;

        if (idAlvo == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }

        if (authenticatedId != null && !authenticatedId.equals(idAlvo)) {
            ownershipService.validarPropriedadeUsuario(idAlvo);
        }

        return idAlvo;
    }
}
