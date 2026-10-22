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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLLER DE SUPORTE E DENÚNCIAS (v1).
 * Rastreabilidade: Fase 7 - f7-t4, f7-t7
 */
@RestController
@RequestMapping({"/api/v1/suporte", "/api/suporte"})
@Tag(name = "Suporte & Moderação (v1)", description = "Abertura de tickets de atendimento e denúncias de conduta")
@SecurityRequirement(name = "bearerAuth")
public class SuporteController {

    @Autowired
    private SuporteService suporteService;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Abrir ticket de suporte", description = "Cria um novo chamado com protocolo único, categoria e prioridade.")
    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<TicketSuporteDTO>> criarTicket(
            @RequestParam Long usuarioId,
            @Valid @RequestBody CriarTicketDTO dto) {
        ownershipService.validarPropriedadeUsuario(usuarioId);
        TicketSuporteDTO ticket = suporteService.criarTicket(usuarioId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(ticket, "Ticket criado com sucesso! Protocolo: " + ticket.getProtocolo()));
    }

    @Operation(summary = "Listar meus tickets de suporte", description = "Retorna histórico de tickets abertos pelo usuário autenticado.")
    @GetMapping("/tickets/meus")
    public ResponseEntity<ApiResponse<List<TicketSuporteDTO>>> listarMeusTickets(@RequestParam Long usuarioId) {
        ownershipService.validarPropriedadeUsuario(usuarioId);
        List<TicketSuporteDTO> tickets = suporteService.listarMeusTickets(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(tickets, "Tickets recuperados"));
    }

    @Operation(summary = "Listar todos os tickets (ADMIN)", description = "Fila geral de atendimento para administradores.")
    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketSuporteDTO>>> listarTodosTickets() {
        if (!SecurityUtils.possuiPapel("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<TicketSuporteDTO> tickets = suporteService.listarTodosTickets();
        return ResponseEntity.ok(ApiResponse.ok(tickets, "Fila de tickets recuperada"));
    }

    @Operation(summary = "Responder ticket de suporte", description = "Atribui parecer técnico ou resposta da moderação e atualiza status.")
    @PatchMapping("/tickets/{ticketId}/responder")
    public ResponseEntity<ApiResponse<TicketSuporteDTO>> responderTicket(
            @PathVariable Long ticketId,
            @RequestParam Long adminId,
            @Valid @RequestBody ResponderTicketDTO dto) {
        if (!SecurityUtils.possuiPapel("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        TicketSuporteDTO ticket = suporteService.responderTicket(ticketId, adminId, dto);
        return ResponseEntity.ok(ApiResponse.ok(ticket, "Resposta enviada e ticket atualizado"));
    }

    @Operation(summary = "Registrar denúncia de usuário", description = "Envia uma denúncia para a equipe de moderação avaliar conduta imprópria.")
    @PostMapping("/denuncias")
    public ResponseEntity<ApiResponse<DenunciaDTO>> criarDenuncia(
            @RequestParam Long denuncianteId,
            @Valid @RequestBody CriarDenunciaDTO dto) {
        ownershipService.validarPropriedadeUsuario(denuncianteId);
        DenunciaDTO denuncia = suporteService.criarDenuncia(denuncianteId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(denuncia, "Denúncia enviada com sucesso para a moderação"));
    }

    @Operation(summary = "Listar denúncias (ADMIN)", description = "Visualização de denúncias pendentes para a banca/administração.")
    @GetMapping("/denuncias")
    public ResponseEntity<ApiResponse<List<DenunciaDTO>>> listarDenuncias(
            @RequestParam(required = false) StatusDenuncia status) {
        if (!SecurityUtils.possuiPapel("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        List<DenunciaDTO> denuncias = suporteService.listarDenuncias(status);
        return ResponseEntity.ok(ApiResponse.ok(denuncias, "Denúncias recuperadas"));
    }
}
