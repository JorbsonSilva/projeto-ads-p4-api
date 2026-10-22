package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.dto.NotificacaoDTO;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CONTROLLER DE NOTIFICAÇÕES INTERNAS (v1).
 * Rastreabilidade: Fase 7 - f7-t5
 */
@RestController
@RequestMapping({"/api/v1/notificacoes", "/api/notificacoes"})
@Tag(name = "Notificações Internas (v1)", description = "Central de avisos internos por polling (Zero Custo)")
@SecurityRequirement(name = "bearerAuth")
public class NotificacaoController {

    @Autowired
    private NotificacaoService notificacaoService;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Listar notificações do usuário", description = "Retorna até 20 notificações mais recentes do usuário autenticado.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificacaoDTO>>> listarNotificacoes(@RequestParam Long usuarioId) {
        ownershipService.validarPropriedadeUsuario(usuarioId);
        List<NotificacaoDTO> lista = notificacaoService.listarNotificacoes(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(lista, "Notificações recuperadas"));
    }

    @Operation(summary = "Contar notificações não lidas", description = "Retorna o total de notificações não lidas para o badge do aplicativo.")
    @GetMapping("/nao-lidas/count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> contarNaoLidas(@RequestParam Long usuarioId) {
        ownershipService.validarPropriedadeUsuario(usuarioId);
        long count = notificacaoService.contarNaoLidas(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("totalNaoLidas", count), "Contagem realizada"));
    }

    @Operation(summary = "Marcar notificação como lida", description = "Marca individualmente uma notificação como lida.")
    @PatchMapping("/{notificacaoId}/lida")
    public ResponseEntity<ApiResponse<Void>> marcarComoLida(
            @PathVariable Long notificacaoId,
            @RequestParam Long usuarioId) {
        ownershipService.validarPropriedadeUsuario(usuarioId);
        notificacaoService.marcarComoLida(notificacaoId, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Notificação marcada como lida"));
    }

    @Operation(summary = "Marcar todas como lidas", description = "Marca todas as notificações pendentes do usuário como lidas.")
    @PatchMapping("/marcar-todas-lidas")
    public ResponseEntity<ApiResponse<Void>> marcarTodasComoLidas(@RequestParam Long usuarioId) {
        ownershipService.validarPropriedadeUsuario(usuarioId);
        notificacaoService.marcarTodasComoLidas(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Todas as notificações foram marcadas como lidas"));
    }
}
