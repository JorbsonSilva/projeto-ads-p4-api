package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.service.ChatService;
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
 * CONTROLLER DE CHAT INTERNO DA AULA (v1).
 * Rastreabilidade: Fase 7 - f7-t1, f7-t2, f7-t3
 */
@RestController
@RequestMapping({"/api/v1/chat", "/api/chat"})
@Tag(name = "Chat Interno da Aula (v1)", description = "Comunicação contextual à mentoria restrita a aluno e professor a custo zero")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Obter dados da conversa", description = "Retorna os metadados da conversa vinculada ao agendamento.")
    @GetMapping("/agendamento/{agendamentoId}")
    public ResponseEntity<ApiResponse<ConversaDTO>> obterConversa(
            @PathVariable Long agendamentoId,
            @RequestParam Long usuarioId) {
        ownershipService.validarPropriedadeUsuario(usuarioId);
        ConversaDTO conversa = chatService.obterOuCriarConversa(agendamentoId, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(conversa, "Conversa recuperada"));
    }

    @Operation(summary = "Listar histórico de mensagens", description = "Retorna as mensagens em ordem cronológica e marca as mensagens recebidas como lidas.")
    @GetMapping("/agendamento/{agendamentoId}/mensagens")
    public ResponseEntity<ApiResponse<List<MensagemDTO>>> listarMensagens(
            @PathVariable Long agendamentoId,
            @RequestParam Long usuarioId) {
        ownershipService.validarPropriedadeUsuario(usuarioId);
        List<MensagemDTO> mensagens = chatService.listarMensagens(agendamentoId, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(mensagens, "Mensagens recuperadas"));
    }

    @Operation(summary = "Enviar mensagem na conversa", description = "Persiste a nova mensagem e notifica a outra parte. Não altera status do agendamento.")
    @PostMapping("/agendamento/{agendamentoId}/mensagens")
    public ResponseEntity<ApiResponse<MensagemDTO>> enviarMensagem(
            @PathVariable Long agendamentoId,
            @Valid @RequestBody EnviarMensagemDTO dto) {
        ownershipService.validarPropriedadeUsuario(dto.getRemetenteId());
        MensagemDTO salva = chatService.enviarMensagem(agendamentoId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(salva, "Mensagem enviada com sucesso"));
    }
}
