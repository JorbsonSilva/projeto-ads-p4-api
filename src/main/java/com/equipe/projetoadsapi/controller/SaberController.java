package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.model.Saber;
import com.equipe.projetoadsapi.service.SaberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CONTROLLER DE SABERES / MATÉRIAS (v1).
 * Rastreabilidade: Fase 4 - f4-t1, f4-t5
 */
@RestController
@RequestMapping({"/api/v1/saberes", "/api/saberes"})
@Tag(name = "Saberes e Matérias (v1)", description = "Catálogo de disciplinas, competências e saberes acadêmicos")
public class SaberController {

    @Autowired
    private SaberService service;

    @Operation(summary = "Listar todas as matérias cadastradas", description = "Retorna lista pública de disciplinas e categorias disponíveis para busca e mentoria.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Saber>>> listarSaberes() {
        List<Saber> lista = service.listarTodos();
        return ResponseEntity.ok(ApiResponse.ok(lista, "Catálogo de saberes recuperado com sucesso"));
    }
}