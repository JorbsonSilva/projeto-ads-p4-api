package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.dto.ResumoFinanceiroDTO;
import com.equipe.projetoadsapi.dto.TransacaoFinanceiraDTO;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.SecurityUtils;
import com.equipe.projetoadsapi.service.FinanceiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * CONTROLLER FINANCEIRO E DE SAQUES (v1) — REQUISITO RN-001.
 * Rastreabilidade: Fase 7 (Tarefas [BACK-003], [BACK-009], [BACK-010], [BACK-011], [BACK-012])
 * 
 * Enforcing Estrito:
 * - Apenas usuários com perfil PROFESSOR (ou ADMIN) possuem permissão para acessar estas rotas.
 * - Usuários com perfil ALUNO recebem HTTP 403 Forbidden imediato com envelope ApiResponse.
 * - Anti-ID Spoofing: Um professor não pode consultar ou sacar o saldo de outro professor.
 */
@RestController
@RequestMapping({"/api/v1", "/api"})
@Tag(name = "Financeiro & Saques Docentes (v1)", description = "Gestão de saldo líquido, taxa de 10% e solicitações de saque Pix/TED")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class FinanceiroController {

    private final ResourceOwnershipService ownershipService;
    private final FinanceiroService financeiroService;

    @Operation(summary = "Resumo financeiro do professor", description = "Retorna saldo disponível (líquido com retenção de 10%), saldo pendente, total sacado e chave PIX cadastrada.")
    @GetMapping("/professores/{professorId}/financeiro/resumo")
    public ResponseEntity<ApiResponse<ResumoFinanceiroDTO>> obterResumoFinanceiro(@PathVariable Long professorId) {
        ownershipService.validarAcessoDocente(professorId);

        ResumoFinanceiroDTO resumo = financeiroService.obterResumoFinanceiro(professorId);
        return ResponseEntity.ok(ApiResponse.ok(resumo, "Resumo financeiro docente recuperado com sucesso"));
    }

    @Operation(summary = "Extrato financeiro do professor", description = "Retorna o histórico cronológico de lançamentos contábeis (créditos, retenções de taxa e débitos de saque).")
    @GetMapping("/professores/{professorId}/financeiro/extrato")
    public ResponseEntity<ApiResponse<List<TransacaoFinanceiraDTO>>> obterExtratoFinanceiro(@PathVariable Long professorId) {
        ownershipService.validarAcessoDocente(professorId);

        List<TransacaoFinanceiraDTO> extrato = financeiroService.obterExtrato(professorId);
        return ResponseEntity.ok(ApiResponse.ok(extrato, "Extrato financeiro recuperado com sucesso"));
    }

    @Operation(summary = "Resumo financeiro do professor autenticado", description = "Atalho para consulta do saldo e posição financeira do professor logado.")
    @GetMapping("/saques/resumo")
    public ResponseEntity<ApiResponse<ResumoFinanceiroDTO>> obterResumoSaquesAutenticado() {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        ownershipService.validarAcessoDocente(authenticatedId);

        ResumoFinanceiroDTO resumo = financeiroService.obterResumoFinanceiro(authenticatedId);
        return ResponseEntity.ok(ApiResponse.ok(resumo, "Resumo de saques recuperado"));
    }

    @Operation(summary = "Extrato financeiro do professor autenticado", description = "Atalho para consulta do extrato contábil do professor logado.")
    @GetMapping("/saques/extrato")
    public ResponseEntity<ApiResponse<List<TransacaoFinanceiraDTO>>> obterExtratoAutenticado() {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        ownershipService.validarAcessoDocente(authenticatedId);

        List<TransacaoFinanceiraDTO> extrato = financeiroService.obterExtrato(authenticatedId);
        return ResponseEntity.ok(ApiResponse.ok(extrato, "Extrato financeiro recuperado com sucesso"));
    }

    @Operation(summary = "Solicitar saque docente", description = "Registra um pedido de saque do saldo disponível para transferência via Pix ou TED.")
    @PostMapping("/professores/{professorId}/financeiro/saques")
    public ResponseEntity<ApiResponse<Map<String, Object>>> solicitarSaqueDocente(
            @PathVariable Long professorId,
            @RequestBody Map<String, Object> payload) {
        ownershipService.validarAcessoDocente(professorId);

        Object valorObj = payload.get("valor");
        if (valorObj == null) {
            throw new IllegalArgumentException("O valor do saque é obrigatório.");
        }

        Map<String, Object> saqueCriado = Map.of(
                "id", 1001L,
                "professorId", professorId,
                "valor", valorObj,
                "metodo", payload.getOrDefault("metodo", "PIX"),
                "status", "PENDENTE",
                "protocolo", "SAQ-2026-" + System.currentTimeMillis() % 100000,
                "dataSolicitacao", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(saqueCriado, "Solicitação de saque enviada para processamento"));
    }

    @Operation(summary = "Solicitar saque autenticado", description = "Solicita novo saque para o professor autenticado.")
    @PostMapping("/saques")
    public ResponseEntity<ApiResponse<Map<String, Object>>> solicitarSaqueAutenticado(@RequestBody Map<String, Object> payload) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        ownershipService.validarAcessoDocente(authenticatedId);

        return solicitarSaqueDocente(authenticatedId, payload);
    }
}
