package com.equipe.projetoadsapi.controller;

import com.equipe.projetoadsapi.dto.*;
import com.equipe.projetoadsapi.model.AgendamentoHistorico;
import com.equipe.projetoadsapi.repository.AgendamentoHistoricoRepository;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLLER DE AGENDAMENTOS (v1) — REQUISITO RN-001.
 * Rastreabilidade: Fase 4 (Contrato v1) & Fase 5 (Evolução de Domínio) & Tarefa [BACK-003]
 * 
 * Contrato RESTful com prefixo /api/v1/agendamentos, validação de antecedência de 24h,
 * isolamento estrito de personas (aluno agenda; professor aceita/recusa),
 * prevenção contra auto-contratação e proteção contra ID Spoofing.
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

    @Autowired
    private AgendamentoHistoricoRepository historicoRepository;

    @Operation(summary = "Solicitar novo agendamento de aula", description = "Aluno solicita mentoria com antecedência mínima de 24h. Bloqueia auto-contratação docente.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Solicitação criada com sucesso"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dados da solicitação inválidos"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso proibido ou tentativa de auto-contratação"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflito de horário do professor"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "Violação de regra de antecedência de 24h ou negócio")
    })
    @PreAuthorize("hasRole('ALUNO') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> criarAgendamento(
            @Valid @RequestBody AgendamentoRequestDTO dto) {
        ownershipService.validarAcessoAluno(dto.getIdAluno());
        ownershipService.validarNaoAutoContratacao(dto.getIdAluno(), dto.getIdProfessor());
        AgendamentoResponseDTO response = agendamentoService.agendarAula(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(response, "Agendamento solicitado com sucesso. Aguardando aceite do professor."));
    }

    @Operation(summary = "Listar agendamentos do aluno", description = "Retorna todas as aulas vinculadas ao aluno informado com bloqueio de ID Spoofing.")
    @PreAuthorize("(hasRole('ALUNO') and @resourceOwnershipService.isDonoDoRecurso(#alunoId)) or hasRole('ADMIN')")
    @GetMapping("/aluno/{alunoId}")
    public ResponseEntity<ApiResponse<List<AgendamentoResponseDTO>>> listarPorAluno(@PathVariable Long alunoId) {
        ownershipService.validarAcessoAluno(alunoId);
        List<AgendamentoResponseDTO> lista = agendamentoService.buscarPorAluno(alunoId);
        return ResponseEntity.ok(ApiResponse.ok(lista, "Aulas do aluno recuperadas com sucesso"));
    }

    @Operation(summary = "Listar agendamentos do professor", description = "Retorna todas as aulas onde o usuário é o instrutor. Restrito a docentes.")
    @PreAuthorize("(hasRole('PROFESSOR') and @resourceOwnershipService.isDonoDoRecurso(#professorId)) or hasRole('ADMIN')")
    @GetMapping("/professor/{professorId}")
    public ResponseEntity<ApiResponse<List<AgendamentoResponseDTO>>> listarPorProfessor(@PathVariable Long professorId) {
        ownershipService.validarAcessoDocente(professorId);
        List<AgendamentoResponseDTO> lista = agendamentoService.buscarPorProfessor(professorId);
        return ResponseEntity.ok(ApiResponse.ok(lista, "Agenda do professor recuperada com sucesso"));
    }

    @Operation(summary = "Buscar agendamento por ID", description = "Consulta detalhes específicos de um agendamento existente.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> buscarPorId(@PathVariable Long id) {
        AgendamentoResponseDTO dto = agendamentoService.buscarPorId(id);
        ownershipService.validarParticipanteAgendamento(dto.getAlunoId(), dto.getProfessorId());
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @Operation(summary = "Histórico de auditoria do agendamento", description = "Consulta todas as transições de status da aula com data/hora e autor.")
    @GetMapping("/{id}/historico")
    public ResponseEntity<ApiResponse<List<AgendamentoHistorico>>> obterHistorico(@PathVariable Long id) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        ownershipService.validarParticipanteAgendamento(agendamento.getAlunoId(), agendamento.getProfessorId());
        List<AgendamentoHistorico> historico = historicoRepository.findByAgendamentoIdOrderByDataHoraDesc(id);
        return ResponseEntity.ok(ApiResponse.ok(historico, "Histórico de transições recuperado"));
    }

    @Operation(summary = "Aceitar solicitação de aula", description = "Professor aceita a solicitação de aula pendente.")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
    @PutMapping("/{id}/aceitar")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> aceitarAgendamento(@PathVariable Long id) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        ownershipService.validarAcessoDocente(agendamento.getProfessorId());
        AgendamentoResponseDTO atualizado = agendamentoService.aceitarAgendamento(id);
        return ResponseEntity.ok(ApiResponse.ok(atualizado, "Aula confirmada com sucesso pelo professor"));
    }

    @Operation(summary = "Recusar solicitação de aula", description = "Professor recusa a solicitação enviando uma justificativa.")
    @PreAuthorize("hasRole('PROFESSOR') or hasRole('ADMIN')")
    @PutMapping("/{id}/recusar")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> recusarAgendamento(
            @PathVariable Long id,
            @Valid @RequestBody RecusarAgendamentoDTO dto) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        ownershipService.validarAcessoDocente(agendamento.getProfessorId());
        AgendamentoResponseDTO atualizado = agendamentoService.recusarAgendamento(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(atualizado, "Solicitação de aula recusada"));
    }

    @Operation(summary = "Cancelar aula agendada", description = "Cancela uma aula já marcada com envio de justificativa obrigatória.")
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> cancelarAgendamento(
            @PathVariable Long id,
            @Valid @RequestBody CancelarAgendamentoDTO dto) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        ownershipService.validarParticipanteAgendamento(agendamento.getAlunoId(), agendamento.getProfessorId());
        AgendamentoResponseDTO atualizado = agendamentoService.cancelarAgendamento(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(atualizado, "Agendamento cancelado com sucesso"));
    }

    @Operation(summary = "Remarcar aula", description = "Altera data e horário da aula confirmada com validação de 24h e conflito.")
    @PutMapping("/{id}/remarcar")
    public ResponseEntity<ApiResponse<AgendamentoResponseDTO>> remarcarAgendamento(
            @PathVariable Long id,
            @Valid @RequestBody RemarcarAgendamentoDTO dto) {
        AgendamentoResponseDTO agendamento = agendamentoService.buscarPorId(id);
        ownershipService.validarParticipanteAgendamento(agendamento.getAlunoId(), agendamento.getProfessorId());
        AgendamentoResponseDTO atualizado = agendamentoService.remarcarAgendamento(id, dto);
        return ResponseEntity.ok(ApiResponse.ok(atualizado, "Aula remarcada com sucesso"));
    }
}