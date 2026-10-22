package com.equipe.projetoadsapi.dto;

import com.equipe.projetoadsapi.model.Agendamento;
import com.equipe.projetoadsapi.model.StatusAgendamento;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de retorno para agendamento de aula.
 * Rastreabilidade: Fase 4 - f4-t6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados detalhados do agendamento de aula")
public class AgendamentoResponseDTO {

    @Schema(description = "Identificador único do agendamento", example = "10")
    private Long id;

    @Schema(description = "ID do aluno", example = "1")
    private Long alunoId;

    @Schema(description = "Nome do aluno", example = "Carlos Silva")
    private String alunoNome;

    @Schema(description = "E-mail do aluno", example = "carlos@aluno.orienta.com.br")
    private String alunoEmail;

    @Schema(description = "ID do professor", example = "2")
    private Long professorId;

    @Schema(description = "Nome do professor", example = "Profª. Helena Souza")
    private String professorNome;

    @Schema(description = "E-mail do professor", example = "helena@orienta.com.br")
    private String professorEmail;

    @Schema(description = "Data agendada (YYYY-MM-DD)", example = "2026-11-15")
    private LocalDate data;

    @Schema(description = "Horário de início (HH:mm:ss)", example = "14:00:00")
    private LocalTime hora;

    @Schema(description = "Status atual do agendamento", example = "CONFIRMADO")
    private StatusAgendamento status;

    @Schema(description = "Justificativa de cancelamento ou recusa (se aplicável)")
    private String justificativaCancelamento;

    @Schema(description = "Modalidade da aula (ONLINE ou PRESENCIAL)", example = "ONLINE")
    private String modalidade;

    @Schema(description = "Assunto ou tópico a ser trabalhado", example = "Algoritmos e Estruturas de Dados")
    private String assunto;

    public static AgendamentoResponseDTO fromEntity(Agendamento agendamento) {
        if (agendamento == null) return null;

        return AgendamentoResponseDTO.builder()
                .id(agendamento.getId())
                .alunoId(agendamento.getAluno() != null ? agendamento.getAluno().getId() : null)
                .alunoNome(agendamento.getAluno() != null ? agendamento.getAluno().getNome() : null)
                .alunoEmail(agendamento.getAluno() != null ? agendamento.getAluno().getEmail() : null)
                .professorId(agendamento.getProfessor() != null ? agendamento.getProfessor().getId() : null)
                .professorNome(agendamento.getProfessor() != null ? agendamento.getProfessor().getNome() : null)
                .professorEmail(agendamento.getProfessor() != null ? agendamento.getProfessor().getEmail() : null)
                .data(agendamento.getData())
                .hora(agendamento.getHora())
                .status(agendamento.getStatus())
                .justificativaCancelamento(agendamento.getJustificativaCancelamento())
                .modalidade(agendamento.getModalidade() != null ? agendamento.getModalidade() : "PRESENCIAL")
                .assunto(agendamento.getAssunto() != null ? agendamento.getAssunto() : "Mentoria Acadêmica")
                .build();
    }
}
