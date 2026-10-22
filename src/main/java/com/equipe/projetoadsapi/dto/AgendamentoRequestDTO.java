package com.equipe.projetoadsapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO de solicitação de agendamento de aula pelo aluno.
 * Rastreabilidade: Fase 4 (DTOs) & Fase 5 (Domínio e Vínculo de Disciplina - f5-t3, f5-t4)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dados para criação de uma nova solicitação de aula")
public class AgendamentoRequestDTO {

    @NotNull(message = "O ID do aluno é obrigatório")
    @Schema(description = "Identificador único do aluno solicitante", example = "1")
    private Long idAluno;

    @NotNull(message = "O ID do professor é obrigatório")
    @Schema(description = "Identificador único do professor", example = "2")
    private Long idProfessor;

    @Schema(description = "Identificador opcional da matéria / saber lecionado", example = "1")
    private Long idDisciplina;

    @NotNull(message = "A data da aula é obrigatória")
    @FutureOrPresent(message = "A data da aula deve ser hoje ou uma data futura")
    @Schema(description = "Data desejada para a realização da aula (YYYY-MM-DD)", example = "2026-11-15")
    private LocalDate data;

    @NotNull(message = "O horário da aula é obrigatório")
    @Schema(description = "Horário de início da aula (HH:mm:ss)", example = "14:00:00")
    private LocalTime hora;

    @Schema(description = "Modalidade da aula: ONLINE ou PRESENCIAL", example = "ONLINE")
    private String modalidade;

    @Schema(description = "Assunto ou tópico específico a ser trabalhado na aula", example = "Estruturas de Dados em Java")
    private String assunto;
}
