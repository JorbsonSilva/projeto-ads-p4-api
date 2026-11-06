package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.AlunoAtendidoDTO;
import com.equipe.projetoadsapi.dto.ApiResponse;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.security.SecurityUtils;
import com.equipe.projetoadsapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLLER DE GESTÃO DOCENTE E HISTÓRICO DE ALUNOS ATENDIDOS (API RESTful v1)
 * Rastreabilidade: Fase 7 - Bloco L (Tarefa [BACK-019]) / Requisito RN-001
 * 
 * Disponibiliza endpoints para a visualização agregada dos alunos atendidos por docentes ("Meus Alunos"),
 * com proteção estrita contra ID Spoofing e isolamento de persona (ROLE_PROFESSOR / ROLE_ADMIN).
 */
@RestController
@RequestMapping({"/api/v1/professores", "/api/professores", "/professores"})
@Tag(name = "Professores & Gestão de Alunos (v1)", description = "Endpoints para histórico pedagógico e gestão de alunos atendidos pelo docente")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
@RequiredArgsConstructor
public class ProfessorController {

    private final UsuarioService usuarioService;
    private final ResourceOwnershipService ownershipService;

    @Operation(summary = "Alunos atendidos pelo professor por ID", 
               description = "Retorna o histórico agregado de alunos que concluíram mentorias com o docente. Valida propriedade para impedir ID Spoofing (RN-001).")
    @GetMapping("/{id}/alunos")
    public ResponseEntity<ApiResponse<List<AlunoAtendidoDTO>>> listarAlunosPorId(@PathVariable Long id) {
        ownershipService.validarAcessoDocente(id);
        List<AlunoAtendidoDTO> alunos = usuarioService.listarAlunosAtendidosDoProfessor(id);
        return ResponseEntity.ok(ApiResponse.ok(alunos, "Alunos atendidos recuperados com sucesso"));
    }

    @Operation(summary = "Atalho: Meus alunos atendidos (Usuário Logado)", 
               description = "Retorna os alunos atendidos pelo docente atualmente autenticado via token JWT.")
    @GetMapping("/meus-alunos")
    public ResponseEntity<ApiResponse<List<AlunoAtendidoDTO>>> listarMeusAlunos() {
        Long professorId = SecurityUtils.getUsuarioAutenticadoId();
        if (professorId == null) {
            throw new AccessDeniedException("Usuário não autenticado no contexto de segurança.");
        }
        ownershipService.validarAcessoDocente(professorId);
        List<AlunoAtendidoDTO> alunos = usuarioService.listarAlunosAtendidosDoProfessor(professorId);
        return ResponseEntity.ok(ApiResponse.ok(alunos, "Alunos atendidos do docente autenticado recuperados com sucesso"));
    }
}
