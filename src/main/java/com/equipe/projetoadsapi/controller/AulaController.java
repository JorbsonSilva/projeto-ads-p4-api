package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.exception.RegraNegocioException;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.SecurityUtils;
import com.equipe.projetoadsapi.service.AulaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CONTROLLER DE AULAS AO VIVO, CRONÔMETRO E AVALIAÇÕES (v1).
 * Rastreabilidade: Fase 6 - f6-t1..f6-t8 | Fase 7 - BACK-007
 */
@RestController
@RequestMapping({"/api/v1/aulas", "/api/aulas"})
@Tag(name = "Aulas Ao Vivo & Avaliações (v1)", description = "Check-in bilateral, cronômetro oficial no servidor e avaliação pós-aula")
@SecurityRequirement(name = "bearerAuth")
public class AulaController {

    @Autowired
    private AulaService aulaService;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Obter status da sessão ao vivo", description = "Retorna o estado oficial da sessão sincronizado com o relógio do servidor.")
    @GetMapping("/{agendamentoId}/sessao")
    public ResponseEntity<ApiResponse<SessaoAulaDTO>> obterSessao(@PathVariable Long agendamentoId) {
        SessaoAulaDTO sessao = aulaService.obterOuCriarSessao(agendamentoId);
        return ResponseEntity.ok(ApiResponse.ok(sessao, "Sessão de aula recuperada"));
    }

    @Operation(summary = "Realizar check-in na aula", description = "Check-in individual do participante. A aula e o cronômetro só iniciam quando ambos (aluno e professor) derem check-in.")
    @PostMapping("/{agendamentoId}/checkin")
    public ResponseEntity<ApiResponse<SessaoAulaDTO>> checkin(
            @PathVariable Long agendamentoId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Long usuarioId = extrairEValidarUsuarioId(payload);
        SessaoAulaDTO sessao = aulaService.realizarCheckin(agendamentoId, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(sessao, "Check-in registrado com sucesso"));
    }

    @Operation(summary = "Pausar aula", description = "Pausa o cronômetro oficial no servidor por consentimento mútuo.")
    @PostMapping("/{agendamentoId}/pausar")
    public ResponseEntity<ApiResponse<SessaoAulaDTO>> pausar(
            @PathVariable Long agendamentoId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Long usuarioId = extrairEValidarUsuarioId(payload);
        String motivo = payload != null && payload.get("motivo") != null ? (String) payload.get("motivo") : "Pausa solicitada";
        SessaoAulaDTO sessao = aulaService.pausarAula(agendamentoId, usuarioId, motivo);
        return ResponseEntity.ok(ApiResponse.ok(sessao, "Aula pausada"));
    }

    @Operation(summary = "Retomar aula", description = "Retoma a contagem do cronômetro após uma pausa.")
    @PostMapping("/{agendamentoId}/retomar")
    public ResponseEntity<ApiResponse<SessaoAulaDTO>> retomar(
            @PathVariable Long agendamentoId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Long usuarioId = extrairEValidarUsuarioId(payload);
        SessaoAulaDTO sessao = aulaService.retomarAula(agendamentoId, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(sessao, "Aula retomada"));
    }

    @Operation(summary = "Finalizar aula bilateralmente", description = "Solicita ou confirma a finalização. A aula só atinge status REALIZADA após o aceite mútuo.")
    @PostMapping("/{agendamentoId}/finalizar")
    public ResponseEntity<ApiResponse<SessaoAulaDTO>> finalizar(
            @PathVariable Long agendamentoId,
            @RequestBody(required = false) Map<String, Object> payload) {
        Long usuarioId = extrairEValidarUsuarioId(payload);
        SessaoAulaDTO sessao = aulaService.finalizarAula(agendamentoId, usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(sessao, "Confirmação de encerramento registrada"));
    }

    @Operation(summary = "Avaliar aula concluída", description = "Envia nota de 1 a 5 estrelas e feedback. Liberado apenas após o status atingir REALIZADA. Restrito a Aluno.")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ALUNO') or hasRole('ADMIN')")
    @PostMapping("/{agendamentoId}/avaliar")
    public ResponseEntity<ApiResponse<AvaliacaoResponseDTO>> avaliar(
            @PathVariable Long agendamentoId,
            @RequestParam(required = false) Long avaliadorId,
            @Valid @RequestBody AvaliacaoRequestDTO dto) {
        if (avaliadorId == null) {
            avaliadorId = SecurityUtils.getUsuarioAutenticadoId();
        }
        if (avaliadorId == null) {
            throw new RegraNegocioException("ID do avaliador não informado.");
        }
        ownershipService.validarPropriedadeUsuario(avaliadorId);
        AvaliacaoResponseDTO avaliacao = aulaService.avaliarAula(agendamentoId, avaliadorId, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(avaliacao, "Avaliação enviada com sucesso! Obrigado pelo feedback."));
    }

    private Long extrairEValidarUsuarioId(Map<String, Object> payload) {
        Long usuarioId = null;
        if (payload != null && payload.get("usuarioId") != null) {
            usuarioId = ((Number) payload.get("usuarioId")).longValue();
        } else {
            usuarioId = SecurityUtils.getUsuarioAutenticadoId();
        }
        if (usuarioId == null) {
            throw new RegraNegocioException("ID do usuário participante não informado.");
        }
        ownershipService.validarPropriedadeUsuario(usuarioId);
        return usuarioId;
    }
}
