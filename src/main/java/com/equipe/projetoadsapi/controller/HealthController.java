package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * CONTROLLER DE HEALTH CHECK (v1).
 * Rastreabilidade: Fase 1 (Ambiente Local) & Fase 4 (Contrato v1) - f4-t1
 */
@RestController
@RequestMapping({"/api/v1/health", "/api/health"})
@Tag(name = "Health Check (v1)", description = "Diagnóstico de conectividade e status operacional da API")
public class HealthController {

    @Operation(summary = "Verificar integridade do backend", description = "Retorna status UP, nome do serviço e timestamp para o mobile e monitoramento.")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "projeto-ads-p4-api");
        health.put("environment", "local-p4");
        health.put("architecture", "Monolito Modular - Custo Zero");
        health.put("contractVersion", "v1.0.0");
        health.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.ok(health, "Serviço operacional"));
    }
}
