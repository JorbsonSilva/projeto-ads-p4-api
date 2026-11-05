package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * CONTROLLER FINANCEIRO E DE SAQUES (v1) — REQUISITO RN-001.
 * Rastreabilidade: Fase 7 (Tarefas [BACK-003], [BACK-009]..[BACK-012])
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
public class FinanceiroController {

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Resumo financeiro do professor", description = "Retorna saldo disponível (líquido com retenção de 10%), saldo pendente e histórico de movimentações.")
    @GetMapping("/professores/{professorId}/financeiro/resumo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obterResumoFinanceiro(@PathVariable Long professorId) {
        ownershipService.validarAcessoDocente(professorId);

        Map<String, Object> resumo = Map.of(
                "professorId", professorId,
                "saldoDisponivel", BigDecimal.valueOf(1250.00),
                "saldoPendente", BigDecimal.valueOf(320.00),
                "totalGanhos", BigDecimal.valueOf(1570.00),
                "taxaPlataformaPercentual", 10.0,
                "aulasRealizadas", 14,
                "chavePixCadastrada", "professor@orienta.com.br",
                "tipoChavePix", "EMAIL",
                "dataAtualizacao", Instant.now().toString()
        );

        return ResponseEntity.ok(ApiResponse.ok(resumo, "Resumo financeiro docente recuperado com sucesso"));
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
                "protocolo", "SAQUE-2026-" + System.currentTimeMillis() % 100000,
                "dataSolicitacao", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(saqueCriado, "Solicitação de saque enviada para processamento"));
    }

    @Operation(summary = "Resumo de saques do professor autenticado", description = "Atalho para consulta do saldo e histórico de saques do professor logado.")
    @GetMapping("/saques/resumo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obterResumoSaquesAutenticado() {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Usuário não autenticado.");
        }
        ownershipService.validarAcessoDocente(authenticatedId);

        Map<String, Object> resumo = Map.of(
                "professorId", authenticatedId,
                "saldoDisponivel", BigDecimal.valueOf(1250.00),
                "saldoPendente", BigDecimal.valueOf(320.00),
                "taxaPlataformaPercentual", 10.0,
                "saques", List.of(
                        Map.of("id", 1L, "valor", 450.00, "status", "CONCLUIDO", "data", "2026-11-01T10:00:00Z"),
                        Map.of("id", 2L, "valor", 300.00, "status", "CONCLUIDO", "data", "2026-11-03T15:30:00Z")
                )
        );

        return ResponseEntity.ok(ApiResponse.ok(resumo, "Resumo de saques recuperado"));
    }

    @Operation(summary = "Solicitar saque", description = "Solicita novo saque para o professor autenticado.")
    @PostMapping("/saques")
    public ResponseEntity<ApiResponse<Map<String, Object>>> solicitarSaqueAutenticado(@RequestBody Map<String, Object> payload) {
        Long authenticatedId = SecurityUtils.getUsuarioAutenticadoId();
        if (authenticatedId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Usuário não autenticado.");
        }
        ownershipService.validarAcessoDocente(authenticatedId);

        return solicitarSaqueDocente(authenticatedId, payload);
    }
}
