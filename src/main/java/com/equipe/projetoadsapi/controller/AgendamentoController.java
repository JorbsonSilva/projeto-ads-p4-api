package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.security.ResourceOwnershipService;
import com.equipe.projetoadsapi.service.AgendamentoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLLER DE AGENDAMENTOS (v1).
 * Rastreabilidade: Fase 4 - f4-t1, f4-t2, f4-t4, f4-t5, f4-t6
 * 
 * Contrato RESTful padronizado com prefixo /api/v1/agendamentos, mantendo alias retroativo
 * para rotas legadas (/api/agendamentos e /agendamentos) para compatibilidade transitória.
 */
@RestController
@RequestMapping({"/api/v1/agendamentos", "/api/agendamentos", "/agendamentos"})
@Tag(name = "Agendamentos (v1)", description = "Ciclo de vida completo de agendamento de aulas, confirmações e reagendamento")
@SecurityRequirement(name = "bearerAuth")
public class AgendamentoController {

    @Autowired
    private AgendamentoService agendamentoService;

    @Autowired
    private ResourceOwnershipService ownershipService;

    @Operation(summary = "Solicitar novo agendamento de aula", description = "Aluno solicita um horário de mentoria com um professor específico.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados da solicitação inválidos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflito de horário do professor"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Violação de regra de negócio")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> criarAgendamento(
            @Valid @RequestBody AgendamentoRequestDTO dto) {
        ownershipService.validarPropriedadeUsuario(dto.getIdAluno());
        AgendamentoResponseDTO response = agendamentoService.agendarAula(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Agendamento solicitado com sucesso. Aguardando aceite do professor."));
    }

    @Operation(summary = "Listar agendamentos do aluno", description = "Retorna todas as aulas vinculadas ao aluno informado.")
    @GetMapping("/aluno/{alunoId}")
    public ResponseEntity<ApiResponse<List<AgendamentoResponseDTO>>> listarPorAluno(@PathVariable Long alunoId) {
        ownershipService.validarPropriedadeUsuario(alunoId);
        List<AgendamentoResponseDTO> lista = agendamentoService.buscarPorAluno(alunoId);
        return ResponseEntity.ok(ApiResponse.ok(lista, "Aulas do aluno recuperadas com sucesso"));
    }

    @Operation(summary = "Listar agendamentos do professor", description = "Retorna todas as aulas onde o usuário é o instrutor.")
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<ApiResponse<List<AgendamentoResponseDTO>>> listarPorProfessor(@PathVariable Long professorId) {
        ownershipService.validarPropriedadeUsuario(professorId);
        List<AgendamentoResponseDTO> lista = agendamentoService.buscarPorProfessor(professorId);
        return ResponseEntity.ok(ApiResponse.ok(lista, "Agenda do professor recuperada com sucesso"));
    }

    @Operation(summary = "Buscar agendamento por ID", description = "Consulta detalhes específicos de um agendamento existente.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> buscarPorId(@PathVariable Long id) {
        AgendamentoResponseDTO dto = agendamentoService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @Operation(summary = "Aceitar solicitação de aula", description = "Professor aceita a solicitação de aula pendente.")
    @PutMapping("/{id}/aceitar")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> aceitarAgendamento(@PathVariable Long id) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        ownershipService.validarPropriedadeUsuario(agendamento.getProfessorId());
        AgendamentoResponseDTO atualizado = agendamentoService.aceitarAgendamento(id);
        return ResponseEntity.ok(ApiResponse.ok(atualizado, "Aula confirmada com sucesso pelo professor"));
    }

    @Operation(summary = "Recusar solicitação de aula", description = "Professor recusa a solicitação enviando uma justificativa.")
    @PutMapping("/{id}/recusar")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> recusarAgendamento(
            @PathVariable Long id,
            @Valid @RequestBody RecusarAgendamentoDTO dto) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        ownershipService.validarPropriedadeUsuario(agendamento.getProfessorId());
        AgendamentoResponseDTO atualizado = agendamentoService.recusarAgendamento(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(atualizado, "Solicitação de aula recusada"));
    }

    @Operation(summary = "Cancelar aula agendada", description = "Cancela uma aula já marcada com envio de justificativa obrigatória.")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> cancelarAgendamento(
            @PathVariable Long id,
            @Valid @RequestBody CancelarAgendamentoDTO dto) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        // Aluno, professor ou admin podem cancelar
        ownershipService.validarPropriedadeOuAdmin(agendamento.getAlunoId());
        AgendamentoResponseDTO atualizado = agendamentoService.cancelarAgendamento(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(atualizado, "Agendamento cancelado com sucesso"));
    }

    @Operation(summary = "Remarcar aula", description = "Altera data e horário da aula confirmada.")
    @PutMapping("/{id}/remarcar")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> remarcarAgendamento(
            @PathVariable Long id,
            @Valid @RequestBody RemarcarAgendamentoDTO dto) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        ownershipService.validarPropriedadeOuAdmin(agendamento.getAlunoId());
        AgendamentoResponseDTO atualizado = agendamentoService.remarcarAgendamento(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(atualizado, "Aula remarcada com sucesso"));
    }
}