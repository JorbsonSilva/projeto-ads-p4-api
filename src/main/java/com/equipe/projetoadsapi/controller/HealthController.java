package com.equipe.projetoadsapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * CONTROLLER DE HEALTH CHECK
 * Rastreabilidade: Fase 1 (Tornar a execução reproduzível) - DT-001 / DT-007
 * Permite que scripts de monitoramento local, docker-compose e o mobile
 * validem a conectividade da API antes de enviar requisições.
 */
@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "projeto-ads-p4-api");
        health.put("environment", "local-p4");
        health.put("architecture", "Monolito Modular - Custo Zero");
        health.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(health);
    }
}
